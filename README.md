[![Build Status](https://travis-ci.org/romanpierson/vertx-logback-elasticsearch-appender.svg?branch=master)](https://travis-ci.org/romanpierson/vertx-logback-elasticsearch-appender) 
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=romanpierson_vertx-logback-elasticsearch-appender&metric=coverage)](https://sonarcloud.io/dashboard?id=romanpierson_vertx-logback-elasticsearch-appender)
![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)

# vertx-logback-elasticsearch-appender

A logback appender that writes its data to a [vertx-elasticsearch-indexer](https://github.com/romanpierson/vertx-elasticsearch-indexer) instance. For details check out the documentation there.

## Technical Usage

The artefact is published on bintray / jcenter (https://bintray.com/romanpierson/maven/com.mdac.vertx-logback-elasticsearch-appender)

Just add it as a dependency to your project (gradle example)

```xml
dependencies {
	compile 'com.mdac:vertx-logback-elasticsearch-appender:1.2.0_RC1'
}
```

## Usage

### Configure appender

```xml

<appender name="ES" class="com.mdac.logback.vertx.elasticsearch.appender.LogbackElasticSearchAppender" level="info">
  	<instanceIdentifier>applicationlog</instanceIdentifier>
  	<properties>
            <property>
                <name>host</name>
                <value>${HOSTNAME}</value>
            </property>
            <property>
                <name>level</name>
                <value>%level</value>
            </property>
            <property>
                <name>thread</name>
                <value>%thread</value>
            </property>
            <property>
                <name>message</name>
                <value>%message</value>
            </property>
            <property>
                <name>stacktrace</name>
                <value>%ex</value>
            </property>
            <property>
                <name>stacktrace</name>
                <value>%exhash</value>
            </property>
            <property>
                <name>logger</name>
                <value>%logger</value>
            </property>
            <property>
                <name>app</name>
                <value>myapp</value>
            </property>
        </properties>
</appender>

```
The instance identifier refers to the index configuration on the vertx-elasticsearch-indexer verticle.

Environment properties can be defined as ${propname}. Static values can be just maintaned setting the value directly.

## Changelog

### 1.0.0 (2019-02-14)

* Initial version

### 1.1.0 (2019-06-23)

* Values are configurable
* Stacktrace hash added as an option

### 1.2.0_RC1 (2019-07-28)

* Upgrade to Vertx 4
