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
openssl genrsa -out "$pathName"_priv.key
openssl rsa -in "$pathName"_priv.key -pubout > "$pathName"_pub.key

# Generate certificate signing request
openssl req -new -key "$pathName"_priv.key -out "$pathName"_csr.csr \
  -subj "/C=PT/ST=Lisbon/L=Lisbon/O=Ransom-Aware/OU=IT/CN=$name"

# Get signature by root CA
openssl x509 -req -days 365 -in "$pathName"_csr.csr -CA "$rootca"/root-ca.pem -CAkey "$rootca"/root-ca.key -outform PEM -out "$pathName"_crt.pem