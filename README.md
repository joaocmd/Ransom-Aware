# Ransom-Aware

## Setup

### Import root CA certificate

Go to your Java Security folder:

`cd $(readlink -f /usr/bin/java | sed "s:bin/java::")/lib/security`

Import the root-ca certificate into your JVM truststore:

`sudo keytool -importcert -alias trustedca.pem -keystore cacerts -file /home/joao/Documents/Ransom-Aware/resources/root-ca/root-ca.pem -storepass changeit`

To create the keystore from previously generated rsa key pair:
`openssl pkcs12 -export -name <key_name> -in <server_cert> -inkey <private_key> -out <out.p12>`

The password for generated keystores/keys is `changeme`

### Generate a user signing key pair and certificate and a encrypt key pair and certificate

On Client/ransom-aware folder, run 

`./generate-certificates.sh <username> <path-to-rootCA>`

The path to root CA must be the folder with the keys and certificate

## Usage

To run the client, use the command

`mvn compile exec:java -Dexec.args="-d <path-to-user-encrypt-key> -s <path-to-user-sign-key>"`

To run the server, use the command

`mvn compile exec:java`
