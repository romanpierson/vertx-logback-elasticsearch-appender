package com.romanpierson.vertx.test.verticle;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class SimpleJsonResponseVerticle extends AbstractVerticle {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass().getName());
	
	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		final Router router = Router.router(vertx);

		router.route().handler(routingContext -> {

			// This handler will be called for every request
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/plain");

			LOG.info("Got request for [{}]", routingContext.request().uri());

			if (routingContext.request().getParam("handledError") != null) {
				String s = null;
				try {
					s.trim();
				} catch (Exception ex) {
					LOG.error("This is a handled error", ex);
				}
			} else if (routingContext.request().getParam("unhandledError") != null) {
				String s = null;
				s.trim();
			}

			// Write to the response and end it
			response.end("Hello World from Vert.x-Web!");
		});

		vertx.createHttpServer().requestHandler(router).listen(8080);

		startPromise.complete();

	}

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
    	
    	stopPromise.complete();
    	
    }
}
