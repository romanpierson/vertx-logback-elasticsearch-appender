/*
 * Copyright (c) 2016-2024 Roman Pierson
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
package com.romanpierson.logback.vertx.elasticsearch.appender;

public interface Constants {

	static final String EVENTBUS_EVENT_NAME = "es.indexer.event";
	
	interface Message{
	
		interface Structure{
			
			static enum Field{
				
				META("meta"),
				MESSAGE("message"),
				TIMESTAMP("timestamp"),
				INSTANCE_IDENTIFIER("instance_identifier");
				
				private final String fieldName;
				
				private Field(String fieldName) {
					this.fieldName = fieldName;
				}
				
				public String getFieldName() {
					return this.fieldName;
				}
			}
			
			
		}
	}
	
}