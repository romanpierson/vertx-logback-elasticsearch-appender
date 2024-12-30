package com.romanpierson.vertx.web.accesslogger;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import com.romanpierson.vertx.test.verticle.SimpleJsonResponseVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class LogbackAppenderTest {

	/**
	 * 
	 * This is just a basic test where we send several requests and check the correct event created on the event bus 
	 * on the address the elasticsearch indexer is listening to
	 * 
	 * 1 x ok message -> INFO message
	 * 2 x handled error -> INFO message plus ERROR message with stacktrace and both stacktrace hashes should be same
	 * 2 x unhandled error -> INFO message plus ERROR message with stacktrace and both stacktrace hashes should be same
	 * 
	 */
	
	// Here we store all messages
	private boolean foundOkMessage = false;
	private boolean foundErrorMessage2info = false;
	private boolean foundErrorMessage3info = false;
	private boolean foundErrorMessage4info = false;
	private boolean foundErrorMessage5info = false;
	private int     foundHandledErrorCounts = 0;
	private String  foundHandledErrorHash = null;
	private int     foundUnhandledErrorCounts = 0;
	private String  foundUnhandledErrorHash = null;
			
	@Test
	@Order(value = 1)
	void testInvalidConfig(Vertx vertx, VertxTestContext testContext) {

		vertx.exceptionHandler(throwable -> {
			testContext.failNow(throwable);
		});
		
		vertx.eventBus().<JsonObject>consumer("es.indexer.event", message -> {
			
			// Add the message to all our messages
			handleMessage(message.body());
			
			// And check if we have received all messages we expect and can finish the test
			checkIfWeHaveAllMessages(testContext);
			
		});
		
		vertx
			.deployVerticle(new SimpleJsonResponseVerticle())
			.onComplete(testContext.succeeding(deploymentId -> {
			
				HttpClient client = vertx.createHttpClient();
				
				// Just to fix github actions issue
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				client
					.request(HttpMethod.GET, 8080, "localhost", "/test")
					.compose(req -> req.send().compose(HttpClientResponse::body))
					.onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
					})));
				
				client
					.request(HttpMethod.GET, 8080, "localhost", "/test2?handledError=true")
					.compose(req -> req.send().compose(HttpClientResponse::body))
					.onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
					})));
			
				client
					.request(HttpMethod.GET, 8080, "localhost", "/test3?unhandledError=true")
					.compose(req -> req.send().compose(HttpClientResponse::body))
					.onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
					})));
				
				client
					.request(HttpMethod.GET, 8080, "localhost", "/test4?handledError=true")
					.compose(req -> req.send().compose(HttpClientResponse::body))
					.onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
					})));
			
				client
					.request(HttpMethod.GET, 8080, "localhost", "/test5?unhandledError=true")
					.compose(req -> req.send().compose(HttpClientResponse::body))
					.onComplete(testContext.succeeding(buffer -> testContext.verify(() -> {
					})));
			
		}));

		
	}
	
	private void handleMessage(JsonObject messageBody) {
		
		// Validate the correct meta info
		JsonObject meta = messageBody.getJsonObject("meta");
		
		if(meta == null || meta.getLong("timestamp") < 1 || !meta.getString("instance_identifier").equals("applicationlog")) {
			return;
		}
		
		JsonObject message = messageBody.getJsonObject("message");
		
		// Logger and thread name should always be there
		if(message == null || message.getString("thread") == null || message.getString("logger") == null) {
			return;
		}
		
		if("Got request for [/test]".equals(message.getString("message")) && validInfoMessage(message)) {
			foundOkMessage = true;
		} else if("Got request for [/test2?handledError=true]".equals(message.getString("message")) && validInfoMessage(message)) {
			foundErrorMessage2info = true;
		} else if("Got request for [/test3?unhandledError=true]".equals(message.getString("message")) && validInfoMessage(message)) {
			foundErrorMessage3info = true;
		} else if("Got request for [/test4?handledError=true]".equals(message.getString("message")) && validInfoMessage(message)) {
			foundErrorMessage4info = true;
		} else if("Got request for [/test5?unhandledError=true]".equals(message.getString("message")) && validInfoMessage(message)) {
			foundErrorMessage5info = true;
		} else if("This is a handled error".equals(message.getString("message")) && validErrorMessage(message)) {
			
			String stacktraceHash = message.getString("stacktraceHash");
			
			if(foundHandledErrorCounts == 0) {
				foundHandledErrorCounts = 1;
				foundHandledErrorHash = stacktraceHash;
			} else if(foundHandledErrorCounts == 1 && stacktraceHash.equals(foundHandledErrorHash)) {
				foundHandledErrorCounts = 2;
			}
			
		} else if("Unhandled exception in router".equals(message.getString("message")) && validErrorMessage(message)) {
			
			String stacktraceHash = message.getString("stacktraceHash");
			
			if(foundUnhandledErrorCounts == 0) {
				foundUnhandledErrorCounts = 1;
				foundUnhandledErrorHash = stacktraceHash;
			} else if(foundUnhandledErrorCounts == 1 && stacktraceHash.equals(foundUnhandledErrorHash)) {
				foundUnhandledErrorCounts = 2;
			}
			
		}
	}
	
	private boolean validErrorMessage(JsonObject xMessage) {
		
		if("ERROR".equals(xMessage.getString("level"))
			&& xMessage.getString("host") != null
			&& xMessage.getString("stacktrace") != null
			&& xMessage.getString("stacktraceHash") != null
			&& "STATIC_VALUE".equals(xMessage.getString("STATIC_NAME"))
			&& "STATIC_VALUE_2".equals(xMessage.getString("STATIC_NAME_2"))) {
			return true;
		}
		
		return false;
	}
	
	private boolean validInfoMessage(JsonObject xMessage) {
		
		if("INFO".equals(xMessage.getString("level"))
			&& xMessage.getString("host") != null
			&& xMessage.getString("stacktrace") == null
			&& xMessage.getString("stacktraceHash") == null
			&& "STATIC_VALUE".equals(xMessage.getString("STATIC_NAME"))
			&& "STATIC_VALUE_2".equals(xMessage.getString("STATIC_NAME_2"))) {
			return true;
		}
		
		return false;
	}
	
	private void checkIfWeHaveAllMessages(VertxTestContext testContext) {
		
		if(foundOkMessage
				&& foundErrorMessage2info
				&& foundErrorMessage3info
				&& foundErrorMessage4info
				&& foundErrorMessage5info
				&& foundHandledErrorCounts == 2
				&& foundUnhandledErrorCounts == 2) {
		
			testContext.completeNow();
		
		}
		
	}
	
}
