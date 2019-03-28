object Report extends App {
  def log = io.Source.fromFile(args(0))
  ClocReport(log)
  SuccessReport(log)
}

object ClocReport {

  // sometimes there's an extra "[info]" in the line, so we have to be careful
  val Regex = """\[([^\]]+)\] (?:\[info\] )?\*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r

  def apply(log: io.Source): Unit = {
    val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    for (Regex(project, count) <- log.getLines)
      results(project) += count.toInt
    println("Lines of Scala code recompiled during this run only:")
    for ((project, sum) <- results.toSeq.sortBy(_._2).reverse)
      println(f"$sum%9d $project")
    println(f"${results.values.sum}%9d TOTAL")
  }

}

object SuccessReport {

  // sample inputs:
  //   [info] Project foo-bar-baz---------------: DID NOT RUN (stuck on broken dependencies: frob, akka-grue, zorch)
  //   [info] Project utest---------------------: FAILED (MatchError: d48a6cde+20180920-1730 (of class java.lang.St...)
  // regex features used:
  //   \w    = word character
  //   ?:    = not a capturing group
  //   (?!-) = negative lookahead -- next character is not "-"
  val Regex = """\[info\] Project ((?:\w|-(?!-))+)-*: ([^\(]+) \((?:stuck on broken dependencies: )?(.*)\)""".r

  val expectedToFail = Set[String](
    "airframe",  // one test failure, looks collections related, could possibly be a collections regression
    "base64",  // StringBuilder stopped allowing assignment to its `.length` member? is that an intentional change we made?
    "blaze",  // no 2.13 upgrade attempted?
    "breeze",  // spire dep org change; but prob not worth messing with until they attempt 2.13 upgrade themselves
    "boopickle", // no 2.13 upgrade attempted?
    "cachecontrol", // looks like maybe we're not picking up their 2.13 version-specific sources?
    "case-app",  // ignore; we're forked from an old version before M5 work happened
    "circe-jackson",  // appears to have run afoul of recent (Feb? Mar?) changes to overloading resolution? could possibly be a Scala regression
    "coursier",  // ignore; we're forked from an old version before M5 work happened
    "eff",  // looks like we made source-incompatible changes to scala.concurrent.Batchable? no 2.13 upgrade attempted, it seems
    "elastic4s",  // no 2.13 upgrade attempted?
    "enumeratum",  // strange missing dependency in circe subproject, we should probably not worry for now
    "grizzled",  // M5 compat is in place, but there are collections-related compile errors, maybe just needs post-M5 tweaks?
    "http4s-parboiled2",  // diverging implicit expansion -- could be a Scala regression, looks tricky to troubleshoot
    "jawn-0-10",  // we can ignore unless/until we're trying to get sbt green
    "kafka",  // doesn't look a 2.13 upgrade has been attempted
    "kittens",  // test failures; could be collections regressions, could just be over-sensitive tests
    "lift-json",  // no 2.13 upgrade attempted? JavaConversions removed, scala-xml dependency missing
    "linter", // no 2.13 upgrade attempted? not essential
    "magnolia",  // they're on M4 but not M5 yet, probably not worth investigating
    "monix",  // silly compile error, I submitted https://github.com/monix/monix/pull/854/files with a fix
    "multibot",  // doesn't find scalaz-zio and linter deps, no idea why
    "nyaya",  // no 2.13 upgrade attempted?
    "paradox",  // no 2.13 upgrade (beyond M1) attempted?
    "parboiled2",  // diverging implicit expansion -- could be a Scala regression, looks tricky to troubleshoot
    "pcplod",  // no 2.13 upgrade attempted?
    "scala-collection-contrib",  // needs M4->M5 changes
    "scala-continuations",  // no 2.13 upgrade attempted
    "scala-java-time",  // scalatest dependency somehow not being picked up
    "scala-java8-compat",  // for 2.13 might become just some shims for cross-compiling, but hasn't yet
    "scala-js",  // needs post-M5 tweak for knownSize override
    "scala-logging",  // runs afoul of new rules about varargs overloading
    "scala-refactoring",  // no 2.13 upgrade attempted
    "scala-sculpt",  // test failure, some silly ordering thing, fine to investigate after RC1 is out
    "scala-stm",  // no 2.13 upgrade attempted
    "scala-swing",  // looks like it needs tweaks for post-M5 changes
    "scala-xml-quote",  // accidental use of `Unit` for `()`
    "scalachess",  // no 2.13 upgrade attempted
    "scalajson",  // looks like it might be hitting Som's change where `import` shadows local identifiers?
    "scallop",  // trivial post-M5 `remove` vs `removed` fix needed
    "scalameter",  // no 2.13 upgrade attempted?
    "scalamock",  // no 2.13 upgrqde attempted (only as far as M3)
    "scalapb",  // dilemma: on master they have M5 support, but they also require Fastparse 2. what to do?
    "scalariform",  // looks like a trivial compile error perhaps related to removal of `foo bar ()` syntax
    "scalatest-tests",  // missing dependency, probably some dbuild phantom rather than a real issue
    "scapegoat",  // bad option: '-Xmax-classfile-name'
    "scodec",  // collections related compile error. may just need small post-M5 change? not clear
    "scopt",  // no arguments allowed for nullary method
    "scribe",  // bahahahahahaha no more octal escapes! get out of the stone age!
    "silencer",  // test failure, maybe just a changed error message wording?
    "slick",  // no 2.13 upgrade attempted?
    "tut",  // deps.ignore thing misses the CrossVersion.full’ed version number :-/
    "twitter-util",  // no 2.13 upgrade attempted
    "twotails",  // "no arguments allowed for nullary method"
  )

  def apply(log: io.Source): Unit = {
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun, unexpected = 0
    var unexpectedFailures = collection.mutable.Buffer[String]()
    val blockerCounts = collection.mutable.Map[String, Int]()
    for (Regex(name, status, blockers) <- lines)
      status match {
        case "SUCCESS" =>
          success += 1
          if (expectedToFail(name)) {
            println(s"unexpected SUCCESS: $name")
            unexpected += 1
          }
        case "FAILED" =>
          failed += 1
          if (!expectedToFail(name)) {
            println(s"unexpected FAILED: $name")
            unexpected += 1
          }
        case "DID NOT RUN" =>
          didNotRun += 1
          for (blocker <- blockers.split(',').map(_.trim))
            blockerCounts(blocker) = 1 + blockerCounts.getOrElse(blocker, 0)
      }
    val total = success + failed + didNotRun
    println(s"SUCCESS $success FAILED $failed DID NOT RUN $didNotRun TOTAL $total")
    println(s"UNEXPECTED $unexpected")
    if (didNotRun > 0) {
      println("BLOCKERS:")
      for ((blocker, count) <- blockerCounts.toList.sortBy(_._2).reverse)
        println(s"$count $blocker")
    }
  }

}
