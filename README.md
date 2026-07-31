# AESTextCrypt - Version 1.2

__Note__

This repository is a fork of the Chris Wood's original __TextCrypt__ project (see: __AESTextCrypt - Version 1.1__).  
The new `1.2` branch adds functions to loading and saving data to a txt file.  
Graphical improvements have been carried out with this repository.  
The "lighweight" __BouncyCastle__ package is provided to generate a smaller JAR file. 
All class files (only 66 files) are located in the `bin` directory.

Please __Read__ the [doc.txt](./doc.txt) for more information about this software (original author: __Chris Wood__).


![window_screenshot](./TextCrypt.png)

__Authors__: Chris Wood (2014) - Eric Normandin (2026)  

## Files source tree

```
TextCrypt/
|-- META-INF/
|   \-- MANIFEST.MF
|-- resources/
|   \-- file_locked.png
|-- bin/
|   \-- bouncycastle/
|       \-- ...
|-- src/
|   |-- com/
|   |   \-- ceperman/
|   |       \-- textcrypt/
|   |           |-- Base64.java
|   |           |-- CryptUtils.java
|   |           |-- ExpandableByteBuffer.java
|   |           |-- Messages.java
|   |           |-- Strings.java
|   |           |-- TextCrypt.java
|   |           |-- messages.properties
|   |           \-- version.properties
|   \-- org/
|       \-- mindrot/
|           \-- jbcrypt/
|               \-- BCrypt.java
|-- doc.txt
|-- history.txt
\-- legal.txt

```

## Compiling the source code

```bash
javac -cp "bin/" -d bin src/com/ceperman/textcrypt/*.java src/org/mindrot/jbcrypt/*.java
```

## How to execute the "TextCrypt.class"

```bash
java -cp "bin/:resources/" com.ceperman.textcrypt.TextCrypt
```
\|
\| __Note__: On Windows, replace `:` with `;` in the classpath.
\|

## Creating the `TextCrypt.jar` file

```bash
jar cvfm TextCrypt.jar META-INF/MANIFEST_old.MF -C bin/ . -C resources/ .
```

***

Enjoy it !
