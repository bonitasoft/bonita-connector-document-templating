# bonita-connector-document-templating
![](https://github.com/bonitasoft/bonita-connector-document-templating/workflows/Build/badge.svg)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=bonitasoft_bonita-connector-document-templating&metric=alert_status)](https://sonarcloud.io/dashboard?id=bonitasoft_bonita-connector-document-templating)
[![GitHub release](https://img.shields.io/github/v/release/bonitasoft/bonita-connector-document-templating?color=blue&label=Release)](https://github.com/bonitasoft/bonita-connector-document-templating/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.bonitasoft.connectors/bonita-connector-document-templating.svg?label=Maven%20Central&color=orange)](https://search.maven.org/search?q=g:%22org.bonitasoft.connectors%22%20AND%20a:%22bonita-connector-document-templating%22)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-yellow.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

Insert document properties inside .docx template

## Bonita compatibility

[v2.1.0](https://github.com/bonitasoft/bonita-connector-document-templating/releases/2.1.0) is compatible with Bonita version 7.11.x and above

## Build

__Clone__ or __fork__ this repository, then at the root of the project run:

`./mvnw`

## Release

In order to create a new release push a `release-<version>` branch with the desired version in pom.xml.
Update the `master` with the next SNAPSHOT version.


## How to design report

### Using Word (docx)

* Insertion > QuickPart > Field...
* Select FusionField and use a template (see [Velocity templating language](http://velocity.apache.org/)) as **field name** (eg: ${name}, ${user.Name}...etc)
* Click OK

### Using LibreOffice (odt)

* Insert > Fields > More fields...
* Go to Variables tab, select UserField and use a template (see [Velocity templating language](http://velocity.apache.org/)) as **value** (eg: ${name}, ${user.Name}...etc)
* Choose Text format
* Click Insert

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation in this project are released under the [GPLv2 License](LICENSE)
