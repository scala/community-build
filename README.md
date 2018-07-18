# Scala Community Build

[![Join the chat at https://gitter.im/scala/community-builds](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scala/community-builds?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This repository contains configuration files that enable us to build and test
a corpus of Scala open source projects together using Lightbend's
[dbuild](https://github.com/lightbend/dbuild).

**How big is it?** The 2.12 build is **3 million lines** of Scala code, total, from **169 projects** (as of July 2018).

**Why do this?** The main goal is to guard against regressions in new
versions of Scala (language, standard library, and modules). It's also
a service to the open source community, providing early notice of
issues and incompatibilities.

**Can I run it myself?** Sure, just clone the repo and run `./run.sh`.

## News

* January 16, 2018: [Community build grows to 141 projects, 2.8 million lines of code](http://scala-lang.org/2018/01/16/community-build-growth.html)

## Read more

[Further documentation is in the wiki](https://github.com/scala/community-builds/wiki).
