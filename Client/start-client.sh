#!/bin/bash

if ! [ $# -eq 1 ] ; then
    echo "Supply one argument: username"
    echo "This assumes that the keys are in ransom-aware/\$1-{encrypt,sign}.key"
    exit 1
fi

mvn clean compile exec:java -Dexec.args="-d ransom-aware/$1-encrypt.key -s ransom-aware/$1-sign.key"
