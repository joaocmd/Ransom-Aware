#!/bin/bash

# docker run -p 27017:27017 --name mongodb mongo
docker start mongodb
mvn clean compile exec:java
