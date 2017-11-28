[![Build Status](https://travis-ci.org/qucosa/qucosa-fcrepo-migration.png)](https://travis-ci.org/qucosa/qucosa-fcrepo-migration)

## Description

ZipDisseminator-HTTP Servlet examines Download-Resources from METS documents (GET-Request-Parameter), compresses the Download-Resources
and responds with the compressed Zip-File.

## Building

The qucosa-fcrepo-zipdisseminator program is a Maven project and as such can be build with the Maven package command:
```
$ mvn package
```

This will generate a deployable WAR file `target/qucosa-fcrepo-zipfiledisseminator-<VERSION>.war` for deployment into a servlet container.

## Usage

The deployed service accepts a `metsurl` parameter, which must contain the URL
encoded location of a valid METS file.

## Licence

The program is licenced under [GPLv3](http://www.gnu.org/licenses/gpl.html). See the COPYING file for details.

