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
  // regex features used:
  //   \w    = word character
  //   ?:    = not a capturing group
  //   (?!-) = negative lookahead -- next character is not "-"
  val Regex = """\[info\] Project ((?:\w|-(?!-))+)-*: (.+) \((?:stuck on broken dependencies: )?(.*)\)""".r

  val expectedToFail: Set[String] =
    System.getProperty("java.specification.version") match {
      case "1.8" =>
        Set(
        )
      case "9" =>
        Set(
          "akka-persistence-cassandra",
          "github4s",
          "kxbmap-configs",
          "paradox",
          "play-core",
          "scala-debugger",
          "twitter-util",
          "zinc",
        )
      case "10" =>
        Set(
          "akka-stream",
          "breeze",
          "curryhoward",
          "jawn-0-10",
          "jawn-0-11",
          "jsoniter-scala",
          "kxbmap-configs",
          "log4s",
          "paradox",
          "scala-async",
          "scala-partest",
          "scalaz8",
          "slick",
          "twitter-util",
        )
    }

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
