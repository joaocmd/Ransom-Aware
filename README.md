# Ransom-Aware

## Setup

### Root CA
Generate a rsa key_pair:
```shell script
openssl genrsa -out root-ca.key
openssl rsa -in root-ca.key -pubout > root-ca.pubkey
openssl req -new -key root-ca.key -out root-ca.csr
openssl x509 -req -days 365 -in root-ca.csr -signkey root-ca.key -out root-ca.pem
echo 01 > root-ca.srl
```
Then import the root-ca certificate on the JVM truststore:

Go to your Java Security folder (should be here):

```shell script
cd $(readlink -f /usr/bin/java | sed "s:bin/java::")/lib/security
```

Import the root-ca certificate into your JVM truststore:

```shell script
sudo keytool -importcert -alias trustedca.pem -keystore cacerts -file /path/to/root-ca.pem -storepass changeit
```

### Server

#### HTTPS
Repeat the steps above for generate a rsa key pair, but sign with root CA:
```shell script
openssl genrsa -out server-ssl.key
openssl rsa -in server-ssl.key -pubout > server-ssl.pubkey
openssl req -new -key server-ssl.key -out server-ssl.csr
openssl x509 -req -days 365 -in server-ssl.csr -signkey root-ca.key -out server-ssl.pem
```

For java to be able to use it, we need to put the private key in a `pkcs12` (or `jks`) keystore.
```shell script
openssl pkcs12 -export -name server -in server.pem -inkey server.key -out server-ssl.p12
```

#### SSH
The server requires a keypair for using rsync over ssh:
```shell script
ssh-keygen -t ed25519 -C "ransom-aware-server"
```

This requires the server to have `ssh-askpass` installed.
The server uses `id_rsync` as a key name for that file.

#### Running the server
The server requires a `MongoDB` server. Which can be easily run through a docker container.
The server supports the following arguments:
```
-p/--path: Path to folder where data is kept (by default it is ransom-aware)
-P/--Port: Port to bind the server to
-db/--db-url: Mongo DB url
```

To use the defaults:
Create a folder `ransom-aware` and move `id_rsync` and `server-tls.p12` inside that folder. Then run:
```shell script
mvn compile exec:java
```

### Backup Server

The backup server needs to be running a `ssh` server.
Edit the `/etc/ssh/sshd_config` file, add/uncomment the following lines:
```
PubkeyAuthentication yes
AuthorizedKeysFile /etc/ssh/authorized_keys
PasswordAuthentication no
```
Then, create a `/etc/ssh/authorized_keys` file and put the previously
generated `id_rsync.pub` there. It should look like
something like this:

```
ssh-ed25519 KEY-HERE-RANDOM-CHARACTERS ransom-aware-server
```

### Client

A client requires 2 key pairs and each pair serves a different purpose:
- Encryption pair: to cipher and decipher symmetric keys which in turn encrypt the files.
- Signing pair: to sign the files to provide integrity and authentication.

To generate the required files for a new user run the script on the `Client/ransom-aware` folder:
```shell script
generate-certificates.sh <username> <path-to-rootCA>
```

When running the client, the path to the private keys must be given:
```shell script
mvn compile exec:java -Dexec.args="-d <path-to-user-encrypt-key> -s <path-to-user-sign-key>"`
```

When first registering a client, the certificates must be given to the server, and so they will be prompted,
the client should reply with the given certificate:
```
Path to encryption certificate: ransom-aware/<user>-encrypt.pem
Path to signing certificate: ransom-aware/<user>-sign.pem
```
