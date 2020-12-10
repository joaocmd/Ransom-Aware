#!/bin/bash
docker start mongodb
mvn clean compile exec:java -Dexec.args="-r 192.168.0.2:rsync/"
