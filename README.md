# Ransom-Aware

## Requirements

The system was developed using JDK 11, and so, Java 11 is necessary to run the project. The scripts use maven for running the program.

### Server
* MongoDB (`docker run -p 27017:27017 --name mongodb mongo`);
* `ssh-askpass` might be needed if the server does not trust the backup server yet or if the ssh is passphrase protected (which it should be).

### Backup Server
* `openssh-server`

## Setup

### Root CA
Generate a rsa key pair, run `generate-keys.sh` in `resources/` or run the following commands:

```shell script
openssl genrsa -out root-ca.key
openssl rsa -in root-ca.key -pubout > root-ca.pubkey
openssl req -new -key root-ca.key -out root-ca.csr
openssl x509 -req -days 365 -in root-ca.csr -signkey root-ca.key -out root-ca.pem
echo 01 > root-ca.srl
```

#### Import Root CA into JVM TrustStore
Go to your Java Security folder (should be here):

```shell script
cd $(readlink -f /usr/bin/java | sed "s:bin/java::")/lib/security
sudo keytool -importcert -alias trustedca.pem -keystore cacerts -file /path/to/root-ca.pem -storepass changeit
```

### Server

Create a folder `ransom-aware` inside `Server/`, the following files should be stored there (`server-ssl.p12` and `id_rsync`)

#### HTTPS

The `generate-keys.sh` script already generated the necessary files, but you can also repeat the procedure yourself, but signing with root CA:

```shell script
openssl genrsa -out server-ssl.key
openssl rsa -in server-ssl.key -pubout > server-ssl.pubkey
openssl req -new -key server-ssl.key -out server-ssl.csr
openssl x509 -req -days 365 -in server-ssl.csr -CA root-ca.pem -CAkey root-ca.key -out server-ssl.pem
```

For java to be able to use it, we need to put the private key in a `pkcs12` (or `jks`) keystore.
```shell script
openssl pkcs12 -export -name server-ssl -in server-ssl.pem -inkey server-ssl.key -out server-ssl.p12
```

#### SSH
The server requires a keypair for using rsync over ssh:
```shell script
ssh-keygen -t ed25519 -C "ransom-aware-server"
```

If you chose to add a passphrase to the password, add it to the ssh agent so that it is not asked everytime:

```shell script
ssh-add id_rsync
```

This requires the server to have `ssh-askpass` installed.
The server uses `id_rsync` as a key name for that file.
The keyfile must have permissions 600:
```shell script
chmod 600 id_rsync
```

#### Running the server
The server requires a `MongoDB` server. Which can be easily run through a docker container.
The server supports the following arguments:
```
-p/--path: Path to folder where data is kept (by default it is ransom-aware)
-P/--Port: Port to bind the server to
-db/--db-url: Mongo DB url
-r/--rsync-uri: URI for rsync server, default is localhost:rsync/
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

A url other than `https://localhost:8443` server url can be chosen with the `-u/--url` option.

Running the `./start-client.sh` script does the same as the command above:

```shell script
./start-client.sh <user>
```

When first registering a client, the certificates must be given to the server, and so they will be prompted, the client should provide the path of the certificates:
```
Path to encryption certificate: ransom-aware/<user>-encrypt.pem
Path to signing certificate: ransom-aware/<user>-sign.pem
```
## Example usage

Having the server running, run the client like explained above, example user could be `joao`:

```
help                     # shows help menu
create a.txt             # creates a.txt on the local fs, and edit it as you wish
save a.txt               # saves a.txt on the server
list                     # shows saved files
list-permissions  a.txt  # lists users with access to the a.txt file
```

Login as a different user `daniel`:
```
get joao/a.txt          # tries to get joao/a.txt but fails because of permissions
```

Grant permissions to `daniel`:
```
grant a.txt daniel
```

Now `daniel` can edit the file:
```
list-permissions joao/a.txt
get joao/a.txt           # stored locally on workspace/joao/a.txt, edit the file
save joao/a.txt
```

`joao` can see the modiffications and roll them back:
```
get a.txt
rollback a.txt 1        # rolls back a.txt 1 version
revoke a.txt daniel     # revokes access permissions from daniel
exit                    # logs out and exits the application
```
