package com.mdac.logback.vertx.elasticsearch.appender.config;

import java.util.ArrayList;
import java.util.Collection;

public class Properties {

	private Collection<Property> properties = new ArrayList<>();

	public Collection<Property> getProperties() {

		return properties;

	}

	public void setProperty(Property property) {

		properties.add(property);

	}

}
