#! /bin/bash

# Usage example:
# ./generate-certificates.sh daniel ../../resources/root-ca/
# ./generate-certificates.sh <username> <pathToRootCA>

if ! [ $# -eq 2 ] ; then
    echo "Wrong number of arguments: Supply user name and path to Root CA certificate."
    exit 1
fi

# Get arguments
name=$1
rootca=$2

echo "$name"
echo "$rootca"

# Generate folder
[ -d "$name" ] && rm -rf "$name"  # check if folder exists and removes it
mkdir "$name"

pathName="$name"/"$name"

# Generate private and public keys
openssl genrsa -out "$pathName".key
#openssl pkcs8 -topk8 -inform PEM -outform DER -in "$pathName".key -out "$pathName".key -nocrypt
openssl rsa -in "$pathName".key -pubout > "$pathName".pubkey

# Generate certificate signing request
openssl req -new -key "$pathName".key -out "$pathName".csr \
  -subj "/C=PT/ST=Lisbon/L=Lisbon/O=Ransom-Aware/OU=IT/CN=$name"

# Get signature by root CA
openssl x509 -req -days 365 -in "$pathName".csr -CA "$rootca"/root-ca.pem -CAkey "$rootca"/root-ca.key -outform PEM -out "$pathName".pem