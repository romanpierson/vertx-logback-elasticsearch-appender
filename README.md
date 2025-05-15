[![Build Status (5.x)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-5.x.yml/badge.svg)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-5.x.yml)
[![Build Status (4.x)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-4.x.yml/badge.svg)](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/actions/workflows/ci-vert.x-4.x.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=romanpierson_vertx-logback-elasticsearch-appender&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=romanpierson_vertx-logback-elasticsearch-appender)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/vert-x3/vertx-awesome)

# vertx-logback-elasticsearch-appender

A logback appender that writes its data to a [vertx-elasticsearch-indexer](https://github.com/romanpierson/vertx-elasticsearch-indexer) instance. For details check out the documentation there.

## Technical Usage

The artefact is published on maven central.

Just add it as a dependency to your project (gradle example)

```xml
dependencies {
	compile 'com.mdac:vertx-logback-elasticsearch-appender:1.3.0'
}
```

## Compatibility with Vert.x core

Since introduction of `vert.x 5` due to some architectural changes the master contains `vert.5` compatible version and its `vert.x 4` compatible counterpart continues on branch `vert.x-4.x`. 

Those two versions are functional equivalent and you should just be able to switch to `vert.5` without any code changes. The plan is also to keep the two versions with same functionality.

Therefore minor version will stay identical but major version will identify if the library is targeted to be used with `vert.x 4` (1) or `vert.x 5` (2)

Logback ES Appender version 4.x / 5.x | Vertx version
----|------
1.3.0 / 2.3.0  | 4.5.1 > / 5.0.0 >

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

Detailed changelog can be found [here](https://github.com/romanpierson/vertx-logback-elasticsearch-appender/blob/master/CHANGELOG.md).

