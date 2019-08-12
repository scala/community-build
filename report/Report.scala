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

  val jdk8Failures = Set(
    "algebra",          // needs ScalaTest 3.1
    "circe-jackson",    // needs ScalaTest 3.1
    "coursier",         // weird git submodule problem when I tried to unfreeze to get 2.13 support. try again I guess
    "curryhoward",      // no 2.13 upgrade (checked Aug 6 2019)
    "doobie",           // needs newer cats-effect, which needs ScalaTest 3.1
    "finagle",          // no 2.13 upgrade (checked Aug 12 2019)
    "giter8",           // no 2.13 upgrade (checked Aug 6 2019)
    "kittens",          // needs ScalaTest 3.1
    "lift-json",        // no 2.13 upgrade (checked Aug 6 2019)
    "linter",           // no 2.13 upgrade (checked Aug 6 2019)
    "metaconfig",       // no 2.13 upgrade (checked Aug 6 2019)
    "metrics-scala",    // needs ScalaTest 3.1
    "multibot",         // needs ScalaTest 3.1
    "paradox",          // no 2.13 upgrade (checked Aug 6 2019)
    "scalastyle",       // no 2.13 upgrade (checked Aug 6 2019)
    "scrooge-shapes",   // no 2.13 upgrade (checked Aug 12 2019)
    "tsec",             // needs ScalaTest 3.1
  )

  val jdk11Failures: Set[String] = Set(
  )

  val expectedToFail: Set[String] =
    System.getProperty("java.specification.version") match {
      case "1.8" =>
        jdk8Failures
      case "11" =>
        jdk8Failures ++ jdk11Failures
      case _ =>
        jdk8Failures ++ jdk11Failures ++ Set(
          "play-file-watch"  // https://github.com/playframework/play-file-watch/issues/46
        )
    }

  def apply(log: io.Source): Unit = {
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun = 0
    val unexpectedSuccesses = collection.mutable.Buffer[String]()
    val unexpectedFailures = collection.mutable.Buffer[String]()
    val blockerCounts = collection.mutable.Map[String, Int]()
    for (Regex(name, status, blockers) <- lines)
      status match {
        case "SUCCESS" =>
          success += 1
          if (expectedToFail(name))
            unexpectedSuccesses += name
        case "FAILED" =>
          failed += 1
          if (!expectedToFail(name))
            unexpectedFailures += name
        case "DID NOT RUN" =>
          didNotRun += 1
          for (blocker <- blockers.split(',').map(_.trim))
            blockerCounts(blocker) = 1 + blockerCounts.getOrElse(blocker, 0)
      }
    val total = success + failed + didNotRun
    println(s"SUCCEEDED: $success")
    if (!unexpectedFailures.isEmpty) {
      val counts = blockerCounts.withDefaultValue(0)
      val uf = unexpectedFailures.sortBy(counts).reverse.mkString(",")
      println(s"FAILED: $uf")
    }
    if (didNotRun > 0) {
      val blockers =
        blockerCounts.toList.sortBy(_._2).reverse
          .collect{case (blocker, count) => s"$blocker ($count)"}
          .mkString(", ")
      println(s"BLOCKERS: $blockers")
    }
    if (unexpectedSuccesses.nonEmpty) {
      val us = unexpectedSuccesses.mkString(",")
      println(s"UNEXPECTED SUCCESSES: $us")
    }
    println(s"FAILED: $failed")
    println(s"DID NOT RUN: $didNotRun")
    println(s"TOTAL: $total")
    sys.exit(unexpectedFailures.size)
  }

}
