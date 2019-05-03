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
    // old list of reasons for the failures are at https://github.com/scala/bug/issues/11453
    // could be reapplied here but would need updating
    "akka-contrib-extra",  // no 2.13 upgrade attempted afaict
    "akka-persistence-cassandra",  // no 2.13 upgrade attempted afaict
    "akka-persistence-jdbc",  // overloading-related compilation errors?
    "algebra",  // source incompatibility involving catalysts?
    "breeze",
    "boopickle",
    "case-app",
    "circe-jackson",
    "coursier",
    "eff",
    "elastic4s",
    "enumeratum",
    "grizzled",
    "http4s-parboiled2",
    "jawn-0-10",
    "kafka",
    "kittens",
    "lift-json",
    "linter",
    "magnolia",
    "metrics-scala",  // scala.language.postfixOps
    "monix",
    "multibot",
    "nyaya",
    "paiges",  // source incompatibility involving catalysts?
    "paradox",
    "parboiled2",
    "play-file-watch",  // no 2.13 upgrade attempted afaict
    "play-webgoat",  // "com.typesafe.play#play-omnidoc_2.13.0-pre-bec2441;2.7.0: not found" ?!
    "scala-gopher",  // no 2.13 upgrade attempted afaict
    "scala-java-time",
    "scala-logging",
    "scala-refactoring",
    "scala-sculpt",
    "scala-stm",
    "scala-xml-quote",
    "scalajson",
    "scalameter",
    "scalamock",
    "scalapb",
    "scalastyle",
    "scalatest-tests",
    "scapegoat",
    "scribe",
    "tut",
    "twitter-util",
  )

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
    if (unexpectedFailures.isEmpty)
      println(s"SUCCESS $success")
    else {
      val uf = unexpectedFailures.mkString(",")
      println(s"SUCCESS $success FAILED?! $uf")
    }
    if (didNotRun > 0) {
      val blockers =
        blockerCounts.toList.sortBy(_._2).reverse
          .collect{case (blocker, count) => s"$count $blocker"}
          .mkString(", ")
      println(s"BLOCKERS: $blockers")
    }
    if (unexpectedSuccesses.nonEmpty) {
      val us = unexpectedSuccesses.mkString(",")
      println(s"UNEXPECTED SUCCESSES: $us")
    }
    println(s"FAILED $failed DID NOT RUN $didNotRun TOTAL $total")
  }

}
