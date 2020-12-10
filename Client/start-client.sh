#!/bin/bash
mvn exec:java -Dexec.args="-u https://ransom-aware:8443 -d ransom-aware/$1-encrypt.key -s ransom-aware/$1-decrypt.key"
