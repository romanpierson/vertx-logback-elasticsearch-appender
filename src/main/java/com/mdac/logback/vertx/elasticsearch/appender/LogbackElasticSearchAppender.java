package com.mdac.logback.vertx.elasticsearch.appender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.mdac.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants;
import com.mdac.vertx.elasticsearch.indexer.ElasticSearchIndexerConstants.Message.Structure.Field;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class LogbackElasticSearchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
	
	private EventBus vertxEventBus; 
	
	private boolean isVertxInitialized = false;
	boolean isInitalizingInProcess = false;
	
	private BlockingQueue<JsonObject> offlineQueue = new LinkedBlockingQueue<>();
	
	ThrowableProxyConverter fullStackTraceConverter;
	
	// Appender Settings
	private String instanceIdentifier;
	

	@Override
	public void start() {
		
		int errors = 0;
		
		if(this.instanceIdentifier == null || this.instanceIdentifier.trim().length() == 0) {
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

	@Override
	protected void append(final ILoggingEvent eventObject) {
		
		initializeVertxIfRequired();
		
		if(isVertxInitialized) {
			
			vertxEventBus.send(ElasticSearchIndexerConstants.EVENTBUS_EVENT_NAME, convert(eventObject));
			
		} else {
			
			try {
				this.offlineQueue.put(convert(eventObject));
			}catch(Exception ex) {
				LOG.error("Error when trying to add event to offline queue", ex);
			}
		}
		
	}
	
	private JsonObject convert(final ILoggingEvent loggingEvent) {
		
		JsonObject jsonMeta = new JsonObject();
		jsonMeta.put(Field.TIMESTAMP.getFieldName(), loggingEvent.getTimeStamp());
		jsonMeta.put(Field.INSTANCE_IDENTIFIER.getFieldName(), this.instanceIdentifier);
		
		JsonObject jsonMessage = new JsonObject();
		jsonMessage.put("level", loggingEvent.getLevel().toString());
		jsonMessage.put("message", loggingEvent.getFormattedMessage());
		jsonMessage.put("thread", loggingEvent.getThreadName());
		jsonMessage.put("loggerName", loggingEvent.getLoggerName());
		
		if(loggingEvent.getThrowableProxy() != null) {
			jsonMessage.put("stackTrace", fullStackTraceConverter.convert(loggingEvent));
		}
		
		return new JsonObject()
				.put(Field.META.getFieldName(), jsonMeta)
				.put(Field.MESSAGE.getFieldName(), jsonMessage);
		
	}
	
	private void initializeVertxIfRequired() {
		
		if(isVertxInitialized) {
			return;
		}
		else if (Vertx.currentContext() == null) {
			// As long as Vertx context is not ready there is no sense to continue
			return;
		} else if (isInitalizingInProcess) {
			return;
		} 
		
		isInitalizingInProcess = true;
		
		long startBusTS = System.currentTimeMillis();
		vertxEventBus = Vertx.currentContext().owner().eventBus();
	
		LOG.info("Successfully connected to Vertx Event Bus - took [" + (System.currentTimeMillis() - startBusTS) + "] ms");
		
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
	
	
}
