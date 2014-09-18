Scala Community Build
=====================

This is a repository with configuration files that
let us build many Scala open source projects at once easily.

As of September 18th 2014, there are 77 Scala projects configured
to be built together. This gives us an ability of building over 1.2
million lines of Scala code with a single command.

Community build includes Scala itself. This enables us to validate changes to
Scala compiler, Scala standard library and Scala modules. The [2.11.x community build](https://jenkins-dbuild.typesafe.com:8499/job/Community-2.11.x) runs nightly and checks changes applied to 2.11.x development branch. Builds for
[Scala 2.12.x](https://jenkins-dbuild.typesafe.com:8499/job/Community-2.12.x/) are available too.

Community build is powered by dbuild tool developed by Typesafe. More details about dbuild
are coming soon.

## Adding a new project configuration

Docs should come in a few weeks. Meanwhile, brave souls should look existing configurations
in `common-2.11.x.conf` file.
