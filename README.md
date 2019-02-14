[![Build Status](https://travis-ci.org/romanpierson/vertx-logback-elasticsearch-appender.svg?branch=master)](https://travis-ci.org/romanpierson/vertx-logback-elasticsearch-appender) ![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)

# vertx-logback-elasticsearch-appender

A logback appender that writes its data to a [vertx-elasticsearch-indexer](https://github.com/romanpierson/vertx-elasticsearch-indexer) instance. For details check out the documentation there.

## Technical Usage

The artefact is published on bintray / jcenter (https://bintray.com/romanpierson/maven/com.mdac.vertx-logback-elasticsearch-appender)

Just add it as a dependency to your project (gradle example)

```xml
dependencies {
	compile 'com.mdac:vertx-logback-elasticsearch-appender:1.0.0'
}
```

## Usage

### Configure appender

```xml

<appender name="ES" class="com.mdac.logback.vertx.elasticsearch.appender.LogbackElasticSearchAppender" level="info">
  	<instanceIdentifier>applicationlog</instanceIdentifier>
</appender>

```
The instance identifier refers to the index configuration on the vertx-elasticsearch-indexer verticle

## Changelog

### 1.0.0 (2019-02-14)

* Initial version


