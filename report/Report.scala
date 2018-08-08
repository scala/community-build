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

  // sample input:
  //   [info] Project foo-bar-baz---------------: DID NOT RUN (frob grue zorch)
  // regex features used:
  //   \w    = word character
  //   ?:    = not a capturing group
  //   (?!-) = negative lookahead -- next character is not "-"
  val Regex = """^\[info\] Project ((?:\w|-(?!-))+)-*: (.+) \(""".r.unanchored

  val expectedToFail = Set[String](
    "akka-actor",
    "cachecontrol",
    "case-app",
    "cats-effect",
    "grizzled",
    "http4s-parboiled2",
    "jackson-module-scala",
    "jawn-0-10",
    "jawn-0-11",
    "lift-json",
    "linter",
    "log4s",
    "magnolia",
    "minitest",
    "paradox",
    "parboiled",
    "parboiled2",
    "pcplod",
    "play-json",
    "scala-async",
    "scala-collections-laws",
    "scala-continuations",
    "scala-js",
    "scala-refactoring",
    "scala-sculpt",
    "scala-stm",
    "scala-swing",
    "scalajson",
    "scalamock",
    "scalastyle",
    "scalatest-tests",
    "scalaz8",
    "scalikejdbc",
    "scapegoat",
    "scodec",
    "sksamuel-exts",
    "slick",
    "specs2-more",
    "spire",
    "tut",
    "twirl",
    "utest",
  )

  def apply(log: io.Source): Unit = {
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun, unexpected = 0
    var unexpectedFailures = collection.mutable.Buffer[String]()
    for (Regex(name, status) <- lines)
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
      }
    val total = success + failed + didNotRun
    println(s"SUCCESS $success FAILED $failed DID NOT RUN $didNotRun TOTAL $total")
    println(s"UNEXPECTED $unexpected")
  }

}
