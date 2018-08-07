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
    "acyclic", "akka-actor", "akka-contrib-extra", "akka-http", "akka-http-cors",
    "akka-http-json", "akka-http-session", "akka-more", "akka-persistence-cassandra", "akka-persistence-jdbc",
    "akka-stream", "ammonite", "ammonite-ops", "autowire", "base64", "better-files", "better-monadic-for",
    "blaze", "breeze", "cachecontrol", "case-app", "cats-effect", "circe", "circe-config", "classutil",
    "conductr-lib", "coursier", "curryhoward", "dispatch", "eff", "elastic4s", "expecty", "fansi", "fastparse",
    "fs2", "fs2-reactive-streams", "geny", "giter8", "github4s", "grizzled", "http4s", "http4s-parboiled2",
    "jackson-module-scala", "jawn-0-10", "jawn-0-11", "jawn-fs2", "lagom", "lift-json", "linter", "log4s",
    "magnolia", "metaconfig", "metrics-scala", "minitest", "monix", "multibot", "nyaya", "paradox", "parboiled",
    "parboiled2", "pcplod", "play-core", "play-doc", "play-json", "play-webgoat", "play-ws", "pprint",
    "pureconfig", "refined", "sbinary", "sbt", "sbt-io", "sbt-librarymanagement", "sbt-util", "scala-async",
    "scala-collections-laws", "scala-continuations", "scala-debugger", "scala-gopher", "scala-js",
    "scala-refactoring", "scala-sculpt", "scala-ssh", "scala-stm", "scala-swing", "scala-xml-quote",
    "scalacheck-shapeless", "scalachess", "scaladex", "scalafix", "scalafmt", "scalaj-http", "scalajson",
    "scalameta", "scalameter", "scalamock", "scalapb", "scalasti", "scalastyle", "scalatags", "scalatest-tests",
    "scalatex", "scalaz8", "scalikejdbc", "scapegoat", "scodec", "scopt", "sjson-new", "sksamuel-exts",
    "slick", "specs2-more", "spire", "sttp", "tut", "twirl", "twitter-util", "upickle", "utest", "zinc",
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
          if (!expectedToFail(name)) {
            println(s"unexpected DID NOT RUN: $name")
            unexpected += 1
          }
      }
    val total = success + failed + didNotRun
    println(s"SUCCESS $success FAILED $failed DID NOT RUN $didNotRun TOTAL $total")
    println(s"UNEXPECTED $unexpected")
  }

}
