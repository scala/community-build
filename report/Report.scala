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
    "airframe",
    "akka",
    "base64",
    "blaze",  // as of Feb 1, doesn't look a 2.13 upgrade has been attempted
    "boopickle",
    "cachecontrol",
    "case-app",
    "coursier",  // typer crash
    "elastic4s",  // not investigated
    "enumeratum",
    "grizzled",
    "http4s-parboiled2",
    "jawn-0-10",
    "kafka",  // doesn't look a 2.13 upgrade has been attempted
    "lift-json",
    "linter",
    "magnolia",
    "mima",  // ran afoul of some scala.tools.nsc change, it looks like
    "monix",
    "nyaya",
    "paradox",
    "parboiled2",
    "pcplod",
    "pprint",
    "scala-collection-contrib",  // needs M4->M5 changes
    "scala-continuations",
    "scala-java-time",  // not investigated
    "scala-java8-compat",
    "scala-refactoring",
    "scala-sculpt",
    "scala-stm",
    "scala-swing",
    "scala-xml-quote",
    "scalajson",
    "scallop",  // trivial post-M5 `remove` vs `removed` fix needed
    "scalameter",
    "scalamock",
    "scalapb",
    "scalariform",  // not investigated
    "scalastyle",
    "scalatest-tests",
    "scalaz8",
    "scapegoat",
    "scodec",
    "scopt",  // no arguments allowed for nullary method
    "scribe",
    "silencer",  // not investigated
    "slick",
    "specs2-more",
    "spire",
    "tut",  // deps.ignore thing misses the CrossVersion.full’ed version number :-/
    "twitter-util",
    "twotails",  // "null" in scala.tools.nsc.classpath.FileBasedCache.getOrCreate
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
