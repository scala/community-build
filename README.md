# Scala 2.12 Community Build

The 2.12.x branch of this repository contains configuration files that
enable us to build and test a corpus of Scala 2.12 open source
projects together using Akka's
[dbuild](https://github.com/lightbend-labs/dbuild).

This project is maintained by the Scala team at Akka Inc., as part of
our overall maintenance of the Scala compiler and standard library.

**How big is it?**
It's **3.6 million lines** of Scala code, total,
from **203 projects** (as of October 2019),
and takes about **15 hours** to run.

**Why do this?** The main goal is to guard against regressions in new
versions of Scala (language, standard library, and modules), as a complement
to the regression test suite that we maintain in scala/scala. It's also
a service to the open source community, providing early notice of
issues and incompatibilities.

**Can I run it myself?** Sure, just clone the repo and `./run`.

## Read more

[Further documentation is in the wiki](https://github.com/scala/community-builds/wiki).
