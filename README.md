spotify-logging
===============

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.spotify/logging/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.spotify/logging) [![Build Status](https://img.shields.io/circleci/project/github/spotify/logging-java)](https://circleci.com/gh/spotify/logging-java)


A small set of utility classes for setting up logback and a helper to
create logging messages suitable for the Spotify internal log parsing
infrastructure.


# Logstash Logback Encoder


The [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder) is used to log
in the JSON format.


The primary motivation is provide any Java service running inside a container with Kubernetes (k8s) or Helios a way
to log in a structured format.

The general flow for a log message running on k8s is as follows.

1. Log a message as part of your service code (e.g. `LOG.info()`).
1. Logback configuration will output to the logs to stdout.
1. Docker captures messages from stdout and logs to file.
1. Fluentd reads from log file and sends to a logging service (e.g. elk stack, stackdriver, splunk, etc).

Instead of writing directly to a given logging service we chose this approach to more easily switch between
providers without needing to modify as little code as possible.


## License

This software is released under the Apache License 2.0. More information
in the file LICENSE distributed with this project.
