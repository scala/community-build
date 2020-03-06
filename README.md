# Scala 2 Community Build

In this repository we build and test a large corpus of open-source Scala 2 projects.
Everything is built from source using Lightbend's [dbuild](https://github.com/lightbend/dbuild).

The project is financed and primarily maintained by Lightbend, as part
of our overall maintenance of the Scala compiler and standard library.

**How big is it?**
The 2.13 build is **3.4 million lines** of Scala code, total,
from **209 repos**, and takes about **9 hours** to run,
as of March 2020.

**Why do this?** The main goal is to guard against regressions in new
versions of Scala (language, standard library, and modules). This complements
the regression test suite we maintain in [scala/scala](https://github.com/scala/scala).
It's also a service to the open source community, providing early notice of
issues and incompatibilities.

**Can I run it myself?** Sure, just clone the repo and run `./run.sh`.

## News

* February 20, 2020: [Scala 2 community build reaches goals](https://www.scala-lang.org/2020/02/20/community-build.html)
* January 13, 2020: [2.13.x build gets big upgrade](https://contributors.scala-lang.org/t/community-build-progress-report-august-2019/3573/9)
* October 17, 2019: [Maintainability improvements made](https://contributors.scala-lang.org/t/community-build-progress-report-august-2019/3573/8)
* August 4, 2019: [Community build progress report](https://contributors.scala-lang.org/t/community-build-progress-report-august-2019/3573/6)
* January 31, 2019: [Community build progress report](https://contributors.scala-lang.org/t/community-build-progress-report/2792)
* January 18, 2019: [Scala community build grows, adds Scala 2.13 and JDK 11](https://www.scala-lang.org/2019/01/18/community-build.html)
* January 16, 2018: [Community build grows to 141 projects, 2.8 million lines of code](http://scala-lang.org/2018/01/16/community-build-growth.html)

## Read more

[Further documentation is in the wiki](https://github.com/scala/community-builds/wiki).
