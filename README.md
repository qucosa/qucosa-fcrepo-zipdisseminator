[![Build Status](https://travis-ci.org/qucosa/qucosa-fcrepo-migration.png)](https://travis-ci.org/qucosa/qucosa-fcrepo-migration)

## Description

ZipDisseminator-HTTP Servlet examines Download-Resources from METS documents (GET-Request-Parameter), compresses the Download-Resources
and responds with the compressed Zip-File.

## Building

The qucosa-fcrepo-zipdisseminator program is a Maven project and as such can be build with the Maven package command:
```
$ mvn package
```

This will generate a runnable JAR file `target/qucosa-fcrepo-zipfiledisseminator-<VERSION>.jar` for execution on the command line.

## Usage

You will have to provide a suitable JDBC driver via the Java Classpath in order to connect to databases.

a. Either run the packed Jar-File with the `java -jar` command
b. Or just run `mvn exec:java`

## Licence

The program is licenced under [GPLv3](http://www.gnu.org/licenses/gpl.html). See the COPYING file for details.

