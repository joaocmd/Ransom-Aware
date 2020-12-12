#!/bin/bash

mkdir root-ca
cd root-ca

# generate root CA certificate
openssl genrsa -out root-ca.key
openssl rsa -in root-ca.key -pubout > root-ca.pubkey
openssl req -new -key root-ca.key -out root-ca.csr -subj "/C=PT/ST=Lisbon/L=Lisbon/O=TrustedCA/OU=TRUST/CN=trustedca"
openssl x509 -req -days 365 -in root-ca.csr -signkey root-ca.key -out root-ca.pem
echo 01 > root-ca.srl

cd ..
# generate server certificate
openssl genrsa -out server-ssl.key
openssl rsa -in server-ssl.key -pubout > server-ssl.pubkey
openssl req -new -key server-ssl.key -out server-ssl.csr -subj "/C=PT/ST=Cartaxo/L=Coimbra/O=RansomAware/OU=TRUST/CN=localhost"
openssl x509 -req -days 365 -in server-ssl.csr -CA root-ca/root-ca.pem -CAkey root-ca/root-ca.key -out server-ssl.pem

# put in pkcs12
openssl pkcs12 -export -name server-ssl -in server-ssl.pem -inkey server-ssl.key -out server-ssl.p12 -password pass:changeme
