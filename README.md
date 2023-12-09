[![Build Status (5.x)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-5.x.yml/badge.svg)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-5.x.yml)
[![Build Status (4.x)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-4.x.yml/badge.svg)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-4.x.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=romanpierson_vertx-logback-elasticsearch-appender&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=romanpierson_vertx-logback-elasticsearch-appender)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/vert-x3/vertx-awesome)

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
  	<indexProperties>host,${HOSTNAME},level,%level,thread,%thread,message,%message,stacktrace,%ex,stacktraceHash,%exhash,logger,%logger</indexProperties>
</appender>

```
The instance identifier refers to the index configuration on the vertx-elasticsearch-indexer verticle.

Index properties contains a comma separated list of name / value pairs. See example above for the built in values.
Environment properties can be defined as ${propname}. Static values can be just maintained setting the value directly.

## Changelog

Detailed changelog can be found [here](https://github.com/romanpierson/vertx-elasticsearch-indexer/blob/master/CHANGELOG.md).

