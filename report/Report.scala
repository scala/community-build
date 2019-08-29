object Report extends App {
  def log = io.Source.fromFile(args(0))
  ClocReport(log)
  val unexpectedFailureCount = SuccessReport(log)
  SplitLog(log)
  sys.exit(unexpectedFailureCount)
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

  val jdk11Failures = Set(
    "coursier",  // needs scala/bug#11125 workaround
    "doobie",  // needs scala/bug#11125 workaround
    "multibot",  //  - testScalaInterpreter *** FAILED ***; [java.lang.SecurityException: ("java.lang.RuntimePermission" "accessSystemModules")
    "sbt-util",  // needs scala/bug#11125 workaround
    "scala-debugger",  // "object FieldInfo is not a member of package sun.reflect"
    "scala-refactoring",  // needs scala/bug#11125 workaround?
    "scalafix",  // needs scala/bug#11125 workaround
    "splain",  // needs scala/bug#11125 workaround
  )

  val expectedToFail: Set[String] =
    System.getProperty("java.specification.version") match {
      case "1.8" =>
        Set(
        )
      case _ =>
        jdk11Failures
    }

  def apply(log: io.Source): Int = {
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
    unexpectedFailures.size
  }

}

object SplitLog {

  val BeginDependencies = """\[info\] ---== Dependency Information ===---""".r
  val EndDependencies = """\[info\] ---== ()End Dependency Information ===---""".r
  val BeginExtract = """\[([^\]]+)\] --== Extracting dependencies for .+ ==--""".r
  val EndExtract = """\[([^\]]+)\] --== End Extracting dependencies for .+ ==--""".r
  val BeginBuild = """\[([^\]]+)\] --== Building .+ ==--""".r
  val EndBuild = """\[([^\]]+)\] --== End Building .+ ==--""".r

  def apply(log: io.Source): Unit = {
    val dir = new java.io.File("../logs")
    dir.mkdirs()
    val lines = log.getLines
    while (lines.hasNext)
      lines.next match {
        case BeginDependencies() =>
          slurp(lines, makeWriter("../dependencies.log"), EndDependencies)
        case BeginExtract(name) =>
          slurp(lines, makeWriter(s"../logs/$name-extract.log"), EndExtract)
        case BeginBuild(name) =>
          slurp(lines, makeWriter(s"../logs/$name-build.log"), EndBuild)
        case _ =>
      }
  }

  import java.io.PrintWriter

  private def makeWriter(path: String): PrintWriter = {
    import java.io._
    val file = new File(path)
    val foStream = new FileOutputStream(file, false)  // false = overwrite, don't append
    val osWriter = new OutputStreamWriter(foStream)
    new PrintWriter(osWriter)
  }

  import scala.util.matching.Regex, annotation.tailrec

  def slurp(lines: Iterator[String], writer: java.io.PrintWriter, sentinel: Regex): Unit = {
    @tailrec def iterate(): Unit =
      if (lines.hasNext)
        lines.next match {
          case sentinel(_) =>
            writer.close()
          case line =>
            writer.println(line)
            iterate()
        }
      else
        throw new IllegalStateException(s"missing end: $sentinel")
    iterate()
  }

}
