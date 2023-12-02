# AnonyChat UML
![UML Diagram](UML.svg)

# Java Chat Server with SSL/TLS Encryption

This repository contains a Java chat server application with added SSL/TLS encryption for secure data transmission. Below are the instructions to set up and run the server with SSL/TLS encryption.

## Prerequisites

- Java Development Kit (JDK)
- Access to command line tools or terminal

## Generating a Keystore and SSL Certificate

1. Generate a self-signed SSL certificate using Java's keytool utility. For production, obtain a certificate from a trusted Certificate Authority (CA).

   ```shell
   keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
   ```

   Replace `keystore.jks` and `password` with your desired keystore filename and password.

## Server Setup

1. **Import SSL Classes**

   Modify your server code to use SSL classes:

   ```java
   import javax.net.ssl.SSLServerSocketFactory;
   import javax.net.ssl.SSLServerSocket;
   ```

2. **Modify Server Socket**

   Update the server socket creation in the `start` method to use SSL:

   ```java
   SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
   listener = (SSLServerSocket) ssf.createServerSocket(port);
   ```

3. **Configure System Properties**

   Set the system properties for the keystore in your code or as JVM arguments:

   - In code:

     ```java
     System.setProperty("javax.net.ssl.keyStore", "path/to/keystore.jks");
     System.setProperty("javax.net.ssl.keyStorePassword", "password");
     ```

   - As JVM arguments:

     ```
     -Djavax.net.ssl.keyStore=path/to/keystore.jks -Djavax.net.ssl.keyStorePassword=password
     ```

## Client Configuration

- Ensure that your client application also supports SSL. Use `SSLSocket` and `SSLSocketFactory` for client-side socket connections.
- For self-signed certificates, configure the client to trust the server's certificate. For production with a CA-issued certificate, this is usually not needed.

## Testing

Test your server and client applications to ensure SSL/TLS encryption works correctly.

## Production Considerations

- Obtain a certificate from a trusted CA for production use.
- Securely manage keystore and truststore files.
- Review and enhance the SSL configuration, including the choice of cipher suites and protocols.
- Adhere to security best practices when dealing with certificates and private keys.

## Contributing

Instructions for contributing to the project (if applicable).
```
