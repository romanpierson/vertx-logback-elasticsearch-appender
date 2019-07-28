package com.mdac.logback.vertx.elasticsearch.appender.config;

import java.util.ArrayList;
import java.util.Collection;

public class Properties {

	private Collection<Property> props = new ArrayList<>();

	public Collection<Property> getProperties() {

		return props;

	}

	public void setProperty(Property property) {

		props.add(property);

	}

}
