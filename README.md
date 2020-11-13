# Ransom-Aware

## Setup

Import the root-ca certificate into your JVM truststore:

`sudo keytool -importcert -alias trustedca.pem -keystore cacerts -file /home/joao/Documents/Ransom-Aware/resources/root-ca/root-ca.pem -storepass changeit`

To create the keystore from previously generated rsa key pair:
`openssl pkcs12 -export -name <key_name> -in <server_cert> -inkey <private_key> -out <out.p12>`

The password for generated keystores/keys is `changeme`
