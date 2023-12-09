/*
 * Copyright (c) 2016-2019 Roman Pierson
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 
 * which accompanies this distribution.
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package com.romanpierson.vertx.web.accesslogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

/**
 * 
 * A simple test route to run and try out the access log
 * 
 * @author Roman Pierson
 *
 */
public class HttpServerVerticle extends AbstractVerticle {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
	
	@SuppressWarnings("null")
	@Override
	public void start() throws Exception {
		
		super.start();
		
		HttpServer server = this.vertx.createHttpServer();
		
		Router router = Router.router(vertx);
		
		
		router.errorHandler(500, handler -> {
			LOG.error("500 error happened", handler);
		});
		
		router
			.route()
				.handler(routingContext -> {
					
					  // This handler will be called for every request
					  HttpServerResponse response = routingContext.response();
					  response.putHeader("content-type", "text/plain");
			
					  LOG.info("Got request for [{}]", routingContext.request().uri());
					  
					  if(routingContext.request().getParam("handledError") != null) {
						  String s = null;
						  try {
							  s.trim();
						  }catch(Exception ex) {
							  LOG.error("error", ex);
						  }
					  } else if(routingContext.request().getParam("unhandledError") != null) {
						  String s = null;
						  s.trim();
					  }
					  
					  // Write to the response and end it
					  response.end("Hello World from Vert.x-Web!");
		});

		long startTS = System.currentTimeMillis();
		
		int port = this.config().getInteger("port");
		
		server.requestHandler(router).listen(port, ar -> {
			if(ar.succeeded()) {
				LOG.info("Successfully started http server on port [{}] in [{}] ms", port, System.currentTimeMillis() - startTS);
			} else {
				LOG.error("Failed to start http server", ar.cause());
			}
		});
		
	}

}
