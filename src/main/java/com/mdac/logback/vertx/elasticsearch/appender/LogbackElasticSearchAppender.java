package com.mdac.logback.vertx.elasticsearch.appender;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.mdac.logback.vertx.elasticsearch.appender.config.Properties;
import com.mdac.logback.vertx.elasticsearch.appender.config.Property;
import com.mdac.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants;
import com.mdac.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants.Message.Structure.Field;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackElasticSearchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());

	private EventBus vertxEventBus;

	private boolean isVertxInitialized = false;
	boolean isInitalizingInProcess = false;

	private BlockingQueue<JsonObject> offlineQueue = new LinkedBlockingQueue<>();

	ThrowableProxyConverter fullStackTraceConverter;

	// Appender Settings
	private String instanceIdentifier;
	private Properties properties;

	private MessageDigest hashDigest;

	// Configured output
	private String level = null;
	private String thread = null;
	private String message = null;
	private String stackTrace = null;
	private String stackTraceHash = null;
	private String logger = null;

	private Map<String, String> extraParameters = new HashMap<String, String>();

	@Override
	public void start() {

		int errors = 0;

		try {
			hashDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException ex) {
			errors++;
			addError("Could not resolve hash digest SHA-1 for appender named [" + name + "].");
		}

		if (this.instanceIdentifier == null || this.instanceIdentifier.trim().length() == 0) {
			errors++;
			addError("\"instanceIdentifier\" property not set for appender named [" + name + "].");
		}

		for (final Property property : this.getProperties().getProperties()) {

			if (property.getName() == null || property.getName().trim().isEmpty() || property.getValue() == null
					|| property.getValue().trim().isEmpty()) {
				errors++;
				addError("found invalid property for appender named [" + name + "].");
			}

			if (resolveStaticProperty(property)) {
				continue;
			}

			extraParameters.put(property.getName(), property.getValue());
		}

		fullStackTraceConverter = new ThrowableProxyConverter();
		fullStackTraceConverter.setOptionList(Arrays.asList("full"));
		fullStackTraceConverter.start();

		if (errors == 0) {
			super.start();
		}

	}

	private boolean resolveStaticProperty(final Property property) {

		if ("%level".equalsIgnoreCase(property.getValue())) {
			level = property.getName();
			return true;
		} else if ("%thread".equalsIgnoreCase(property.getValue())) {
			thread = property.getName();
			return true;
		} else if ("%message".equalsIgnoreCase(property.getValue())) {
			message = property.getName();
			return true;
		} else if ("%ex".equalsIgnoreCase(property.getValue())) {
			stackTrace = property.getName();
			return true;
		} else if ("%exhash".equalsIgnoreCase(property.getValue())) {
			stackTraceHash = property.getName();
			return true;
		} else if ("%logger".equalsIgnoreCase(property.getValue())) {
			logger = property.getName();
			return true;
		}

		return false;

	}

	private String getHash(String input) {

		byte[] messageDigest = hashDigest.digest(input.getBytes());

		// Convert byte array into signum representation
		BigInteger no = new BigInteger(1, messageDigest);

		// Convert message digest into hex value
		String hashtext = no.toString(16);

		// Add preceding 0s to make it 32 bit
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}

		// return the HashText
		return hashtext;

	}

	@Override
	protected void append(final ILoggingEvent eventObject) {

		initializeVertxIfRequired();

		if (isVertxInitialized) {

			vertxEventBus.send(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME, convert(eventObject));

		} else {

			try {
				this.offlineQueue.put(convert(eventObject));
			} catch (Exception ex) {
				LOG.error("Error when trying to add event to offline queue", ex);
			}
		}

	}

	private JsonObject convert(final ILoggingEvent loggingEvent) {

		JsonObject jsonMeta = new JsonObject();
		jsonMeta.put(Field.TIMESTAMP.getFieldName(), loggingEvent.getTimeStamp());
		jsonMeta.put(Field.INSTANCE_IDENTIFIER.getFieldName(), this.instanceIdentifier);

		JsonObject jsonMessage = new JsonObject();

		if (level != null)
			jsonMessage.put(level, loggingEvent.getLevel().toString());
		if (message != null)
			jsonMessage.put("message", loggingEvent.getFormattedMessage());
		if (thread != null)
			jsonMessage.put(thread, loggingEvent.getThreadName());
		if (logger != null)
			jsonMessage.put(logger, loggingEvent.getLoggerName());

		if (loggingEvent.getThrowableProxy() != null && (stackTrace != null || stackTraceHash != null)) {
			final String stackTrace = fullStackTraceConverter.convert(loggingEvent);
			if (stackTrace != null)
				jsonMessage.put("stackTrace", stackTrace);
			if (stackTraceHash != null)
				jsonMessage.put("stackTraceHash", getHash(stackTrace));
		}

		for (final Entry<String, String> entry : extraParameters.entrySet()) {
			jsonMessage.put(entry.getKey(), entry.getValue());
		}

		return new JsonObject().put(Field.META.getFieldName(), jsonMeta).put(Field.MESSAGE.getFieldName(), jsonMessage);

	}

	private void initializeVertxIfRequired() {

		if (isVertxInitialized) {
			return;
		} else if (Vertx.currentContext() == null) {
			// As long as Vertx context is not ready there is no sense to continue
			return;
		} else if (isInitalizingInProcess) {
			return;
		}

		isInitalizingInProcess = true;

		long startBusTS = System.currentTimeMillis();
		vertxEventBus = Vertx.currentContext().owner().eventBus();

		LOG.info("Successfully connected to Vertx Event Bus - took [" + (System.currentTimeMillis() - startBusTS)
				+ "] ms");

		int currentSize = offlineQueue.size() + 1;

		LOG.info("Offline queue not empty - sending [" + currentSize + "] events to ElasticSearchIndexerVerticle");

		final Collection<JsonObject> drainedValues = new ArrayList<>(currentSize);

		this.offlineQueue.drainTo(drainedValues, currentSize);

		drainedValues.forEach(s -> {
			vertxEventBus.send(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME, s);
		});

		isVertxInitialized = true;

	}

	public String getInstanceIdentifier() {
		return instanceIdentifier;
	}

	public void setInstanceIdentifier(final String instanceIdentifier) {
		this.instanceIdentifier = instanceIdentifier;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

}
