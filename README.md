# Scala Community Build

[![Join the chat at https://gitter.im/scala/community-builds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scala/community-builds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This repository contains configuration files that enable us to build and test
many Scala open source projects together using Typesafe's
[dbuild](https://github.com/typesafehub/dbuild).

How much is many?  It's over a million lines of Scala code, total.

Why? The main goal of this is to guard against regressions in new
versions of Scala (language, standard library, and modules).

## Continuous integration

The community build is typically run as part of Scala's
CI infrastructure, as documented in the
[scala-jenkins-infra README](https://github.com/scala/scala-jenkins-infra/blob/master/README.md).

Build results are viewable on [scala-ci.typesafe.com](https://scala-ci.typesafe.com).
(For example, see the [2.11.x results](https://scala-ci.typesafe.com/job/scala-2.11.x-integrate-community-build/).)

(There's also https://jenkins-dbuild.typesafe.com:8499, but we're working
on phasing that out.)

## Running it locally

You might want to set JAVA_HOME first.  The 2.11.x community build assumes
Java 6; 2.12.x, Java 8.

Then run:

    ./scripts/jobs/integrate/community-build -l

While you wait, make yourself a sandwich.  (If you have time,
make yourself a sandwich-making machine.)

## Adding your project

On the appropriate branch or branches (2.11.x, 2.12.x, etc),
edit `common.conf`.

The [dbuild documentation](http://typesafehub.github.com/dbuild) might help.
Especially the "Building a single target project" [part](http://typesafehub.github.io/dbuild/0.9.1/dbuild.html#building-a-single-target-project).

## Is my project eligible?

see https://github.com/scala/community-builds/wiki
