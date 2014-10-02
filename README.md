Scala Community Build
=====================

This repository contains configuration files that
enable us to build many Scala open source projects all at once.

As of September 18th 2014, there are 77 Scala projects configured
to be built together. With [dbuild](https://github.com/typesafehub/dbuild), 
we have the ability to build over 1.2
million lines of Scala code with a single command!

The Scala community build includes Scala itself. This enables the Scala team to validate changes to
the Scala compiler, the Scala standard library and Scala modules. The [2.11.x community build](https://jenkins-dbuild.typesafe.com:8499/job/Community-2.11.x) runs nightly and checks changes applied to the 2.11.x development branch. Builds covering
[Scala 2.12.x](https://jenkins-dbuild.typesafe.com:8499/job/Community-2.12.x/) development branch are available too.

The community build is powered by [dbuild](https://github.com/typesafehub/dbuild), a tool developed by Typesafe. More details about dbuild are coming soon.

## Adding a new project configuration

Docs should come in a few weeks. Meanwhile, brave souls should look at existing configurations
in `common-2.11.x.conf` file.
