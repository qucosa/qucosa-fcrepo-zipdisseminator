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

### DNB XMetaDissPlus file name filter

If a `xmdpfilter=true` query parameter is given, all filenames are filtered for the DNB XMetaDissPlus transfer file protocol. All whitespace characters are replaced by `-` and round brackets are removed. Thus the file name `Hello (World)` would be changed to `Hello-World` in the resulting ZIP file. Furthermore `/` will be replaced by `-` to avoid having folders in the ZIP. Also if files with a content-type `application/pdf` are present only these files remain in the ZIP. Every other file will be filtered out.

## Licence

The program is licenced under [GPLv3](http://www.gnu.org/licenses/gpl.html). See the COPYING file for details.

