package com.romanpierson.logback.vertx.elasticsearch.appender;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.romanpierson.logback.vertx.elasticsearch.appender.Constants.Message.Structure.Field;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class LogbackElasticSearchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	private EventBus vertxEventBus;

	private boolean isVertxInitialized = false;
	boolean isInitalizingInProcess = false;

	private BlockingQueue<JsonObject> offlineQueue = new LinkedBlockingQueue<>();

	private ThrowableProxyConverter fullStackTraceConverter;

	// Appender Settings
	private String instanceIdentifier;

	private MessageDigest hashDigest;

	// Configured output
	private String labelLevel = null;
	private String labelThread = null;
	private String labelMessage = null;
	private String labelStackTrace = null;
	private String labelStackTraceHash = null;
	private String labelLogger = null;

	private Map<String, String> extraParameters = new HashMap<>();

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
		
		fullStackTraceConverter = new ThrowableProxyConverter();
		fullStackTraceConverter.setOptionList(Arrays.asList("full"));
		fullStackTraceConverter.start();

		if (errors == 0) {
			super.start();
		}

	}

	private boolean resolveStaticProperty(final String propertyName, final String propertyValue) {

		if ("%level".equalsIgnoreCase(propertyValue)) {
			labelLevel = propertyName;
			return true;
		} else if ("%thread".equalsIgnoreCase(propertyValue)) {
			labelThread = propertyName;
			return true;
		} else if ("%message".equalsIgnoreCase(propertyValue)) {
			labelMessage = propertyName;
			return true;
		} else if ("%ex".equalsIgnoreCase(propertyValue)) {
			labelStackTrace = propertyName;
			return true;
		} else if ("%exhash".equalsIgnoreCase(propertyValue)) {
			labelStackTraceHash = propertyName;
			return true;
		} else if ("%logger".equalsIgnoreCase(propertyValue)) {
			labelLogger = propertyName;
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
		if (hashtext.length() < 32) {
			final StringBuilder sb = new StringBuilder();

			for (int i = hashtext.length(); i < 32; i++) {
				sb.append('0');
			}
			sb.append(hashtext);

			hashtext = sb.toString();
		}

		// return the HashText
		return hashtext;

	}

	@Override
	protected void append(final ILoggingEvent eventObject) {

		initializeVertxIfRequired();

		if (isVertxInitialized) {

			vertxEventBus.send(Constants.EVENTBUS_EVENT_NAME, convert(eventObject));

		} else {

			try {
				this.offlineQueue.put(convert(eventObject));
			} catch (Exception ex) {
				logger.error("Error when trying to add event to offline queue", ex);
			}
		}

	}

	private JsonObject convert(final ILoggingEvent loggingEvent) {

		JsonObject jsonMeta = new JsonObject();
		jsonMeta.put(Field.TIMESTAMP.getFieldName(), loggingEvent.getTimeStamp());
		jsonMeta.put(Field.INSTANCE_IDENTIFIER.getFieldName(), this.instanceIdentifier);

		JsonObject jsonMessage = new JsonObject();

		if (labelLevel != null)
			jsonMessage.put(labelLevel, loggingEvent.getLevel().toString());
		if (labelMessage != null)
			jsonMessage.put(labelMessage, loggingEvent.getFormattedMessage());
		if (labelThread != null)
			jsonMessage.put(labelThread, loggingEvent.getThreadName());
		if (labelLogger != null)
			jsonMessage.put(labelLogger, loggingEvent.getLoggerName());

		if (loggingEvent.getThrowableProxy() != null && (labelStackTrace != null || labelStackTraceHash != null)) {
			final String stackTraceAsString = fullStackTraceConverter.convert(loggingEvent);
			if (labelStackTrace != null && stackTraceAsString != null)
				jsonMessage.put(labelStackTrace, stackTraceAsString);
			if (labelStackTraceHash != null && stackTraceAsString != null)
				jsonMessage.put(labelStackTraceHash, getHash(stackTraceAsString));
		}

		for (final Entry<String, String> entry : extraParameters.entrySet()) {
			jsonMessage.put(entry.getKey(), entry.getValue());
		}

		return new JsonObject().put(Field.META.getFieldName(), jsonMeta).put(Field.MESSAGE.getFieldName(), jsonMessage);

	}

	private void initializeVertxIfRequired() {

		if (isVertxInitialized || Vertx.currentContext() == null || isInitalizingInProcess) {
			return;
		}

		isInitalizingInProcess = true;

		long startBusTS = System.currentTimeMillis();
		vertxEventBus = Vertx.currentContext().owner().eventBus();

		logger.info("Successfully connected to Vertx Event Bus - took [" + (System.currentTimeMillis() - startBusTS)
				+ "] ms");

		int currentSize = offlineQueue.size() + 1;

		logger.info("Offline queue not empty - sending [" + currentSize + "] events to ElasticSearchIndexerVerticle");

		final Collection<JsonObject> drainedValues = new ArrayList<>(currentSize);

		this.offlineQueue.drainTo(drainedValues, currentSize);

		drainedValues.forEach(s -> vertxEventBus.send(Constants.EVENTBUS_EVENT_NAME, s));

		isVertxInitialized = true;

	}

	public String getInstanceIdentifier() {
		return instanceIdentifier;
	}

	public void setInstanceIdentifier(final String instanceIdentifier) {
		this.instanceIdentifier = instanceIdentifier;
	}
	
	public void setIndexProperties(String indexPropertiesConfiguration) {
		
		// As logback seems to have complicated much the handling of custom models we use a more simpler approach for now
		// Basically the properties are passed as string in a single configuration string
		// label1,value1,label2,value2,labelN,valueN
		
		final String[] tokens = indexPropertiesConfiguration == null ?  null : indexPropertiesConfiguration.split(",");
		
		if(tokens == null || tokens.length == 0 || tokens.length % 2 != 0) {
			logger.error("Value provided for indexProperties invalid");
			return;
		}
		
		// If we come here it means we have pairs of label/value - lets translate them to properties
		for(int i=0;i< tokens.length;i = i + 2) {
		
			if(resolveStaticProperty(tokens[i], tokens[i+1])) {
				continue;
			}
			
			extraParameters.put(tokens[i], tokens[i+1]);
			
		}
		
	}

}
