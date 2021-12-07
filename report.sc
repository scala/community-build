#!/usr/bin/env scala-cli shebang -O -source:future

using scala 3.1.1-RC1

object ClocReport:

  // sometimes there's an extra "[info]" in the line, so we have to be careful
  val Regex = """\[([^\]]+)\] (?:\[info\] )?\*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r

  def apply(log: io.Source): Unit =
    val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    for case Regex(project, count) <- log.getLines do
      results(project) += count.toInt
    println("Lines of Scala code recompiled during this run only:")
    for (project, sum) <- results.toSeq.sortBy(_._2).reverse do
      println(f"$sum%9d $project")
    println(f"${results.values.sum}%9d TOTAL")

object SuccessReport:

  // sample inputs:
  //   [info] Project foo-bar-baz---------------: DID NOT RUN (stuck on broken dependencies: frob, akka-grue, zorch)
  //   [info] Project utest---------------------: FAILED (MatchError: d48a6cde+20180920-1730 (of class java.lang.St...)
  // regex features used:
  //   \w    = word character
  //   ?:    = not a capturing group
  //   (?!-) = negative lookahead -- next character is not "-"
  val Regex = """\[info\] Project ((?:\w|-(?!-))+)-*: ([^\(]+) \((?:stuck on broken dependencies: )?(.*)\)""".r

  val jdk8Failures = Set[String](
    "shapeless-java-records",  // requires JDK 15
  )

  val jdk11Failures = Set[String](
    "coursier",  // needs scala/bug#11125 workaround
    "doobie",  // needs scala/bug#11125 workaround
    "multibot",  //  - testScalaInterpreter *** FAILED ***; [java.lang.SecurityException: ("java.lang.RuntimePermission" "accessSystemModules")
    "scala-debugger",  // "object FieldInfo is not a member of package sun.reflect"
    "scala-refactoring",  // needs scala/bug#11125 workaround?
    "scalafix",  // needs scala/bug#11125 workaround
  )

  val jdk17Failures = Set[String](
    "airframe" ,     // runs afoul of JEP 403
    "akka",          // needs newer sbt-osgi
    "classutil",     // runs afoul of JEP 403
    "dispatch",      // java.lang.ExceptionInInitializerError: null
    "fs2",           // needs newer sbt-osgi
    "paradox",       // Unsupported class file major version
    "play-doc",      // Error creating extended parser class: Could not determine whether class 'play.doc.CodeReferenceParser$$parboiled' has already been loaded (Parboiled.java:58)
    "playframework", // Failed tests: play.mvc.HttpFormsTest
    "sbt-io",        // sbt.io.IOSpecification fails
    "scala-async",   // needs newer sbt-scala-module (for newer sbt-osgi)
    "scala-continuations", // needs newer sbt-scala-module (for newer sbt-osgi)
    "scala-logging", // needs newer sbt-osgi
    "scala-swing",   // needs newer sbt-scala-module (for newer sbt-osgi)
    "scalatest-tests", // Error creating extended parser class: Could not determine whether class 'org.pegdown.Parser$$parboiled' has already been loaded
    "scalikejdbc",   // test failure: scalikejdbc.jsr310.StatementExecutorSpec
    "specs2-more",   // Error creating extended parser class: Could not determine whether class 'org.pegdown.Parser$$parboiled' has already been loaded (Parboiled.java:58)
    "squants",       // needs newer sbt-osgi
    "twitter-util",  // Unrecognized VM option 'AggressiveOpts'
  )

  val expectedToFail: Set[String] =
    System.getProperty("java.specification.version") match
      case "1.8" =>
        jdk8Failures
      case "11" =>
        jdk8Failures ++ jdk11Failures
      case _ =>
        jdk11Failures ++ jdk17Failures

  def apply(log: io.Source): Option[Int] =
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun = 0
    val unexpectedSuccesses = collection.mutable.Buffer[String]()
    val unexpectedFailures = collection.mutable.Buffer[String]()
    val blockerCounts = collection.mutable.Map[String, Int]()
    for case Regex(name, status, blockers) <- lines do
      status match
        case "EXTRACTION FAILED" =>
          success = -1000
        case "SUCCESS" =>
          success += 1
          if expectedToFail(name) then
            unexpectedSuccesses += name
        case "FAILED" =>
          failed += 1
          if !expectedToFail(name) then
            unexpectedFailures += name
        case "DID NOT RUN" =>
          didNotRun += 1
          for blocker <- blockers.split(',').map(_.trim) do
            blockerCounts(blocker) = 1 + blockerCounts.getOrElse(blocker, 0)
    if success < 0 then return None
    val total = success + failed + didNotRun
    println(s"SUCCEEDED: $success")
    val sortedFailures =
      unexpectedFailures.sortBy(blockerCounts.withDefaultValue(0)).reverse
    if unexpectedFailures.nonEmpty then
      val uf = sortedFailures.mkString(",")
      println(s"FAILURES (UNEXPECTED): $uf")
    if  didNotRun > 0 then
      val blockers =
        blockerCounts.toList.sortBy(_._2).reverse
          .collect{case (blocker, count) => s"$blocker ($count)"}
          .mkString(", ")
      println(s"BLOCKING DOWNSTREAM: $blockers")
    if unexpectedSuccesses.nonEmpty then
      val us = unexpectedSuccesses.mkString(",")
      println(s"SUCCESSES (UNEXPECTED): $us")
    println(s"FAILED: $failed")
    println(s"BLOCKED, DID NOT RUN: $didNotRun")
    println(s"TOTAL: $total")
    for url <- Option(System.getenv("BUILD_URL")) do
      if unexpectedFailures.nonEmpty then
        println()
        for failed <- sortedFailures do
          println(s"""<a href="${url}artifact/logs/$failed-build.log">$failed</a>""")
    if success == 0 then
      Some(1)
    else
      Some(unexpectedFailures.size)

object SplitLog:

  val BeginDependencies = """\[info\] ---== Dependency Information ===---""".r
  val EndDependencies = """\[info\] ---== ()End Dependency Information ===---""".r
  val BeginExtract = """\[([^\]]+)\] --== Extracting dependencies for .+ ==--""".r
  val EndExtract = """\[([^\]]+)\] --== End Extracting dependencies for .+ ==--""".r
  val BeginBuild = """\[([^\]]+)\] --== Building .+ ==--""".r
  val EndBuild = """\[([^\]]+)\] --== End Building .+ ==--""".r

  def apply(log: io.Source): Unit =
    val dir = java.io.File("logs")
    dir.mkdirs()
    val lines = log.getLines
    while lines.hasNext do
      lines.next match
        case BeginDependencies() =>
          slurp(lines, makeWriter("logs/_dependencies.log"), EndDependencies)
        case BeginExtract(name) =>
          slurp(lines, makeWriter(s"logs/$name-extract.log"), EndExtract)
        case BeginBuild(name) =>
          slurp(lines, makeWriter(s"logs/$name-build.log"), EndBuild)
        case _ =>

  import java.io.PrintWriter

  def makeWriter(path: String): PrintWriter =
    import java.io.*
    val file = File(path)
    val foStream = FileOutputStream(file, false)  // false = overwrite, don't append
    val osWriter = OutputStreamWriter(foStream)
    PrintWriter(osWriter)

  import scala.util.matching.Regex, annotation.tailrec

  def slurp(lines: Iterator[String], writer: java.io.PrintWriter, sentinel: Regex): Unit =
    @tailrec def iterate(): Unit =
      if lines.hasNext then
        lines.next match
          case sentinel(_) =>
            writer.close()
          case line =>
            writer.println(line)
            iterate()
      else
        writer.close()
    iterate()

object UpdateDependencies:
  val Line1 = """\[info\] Project (\S+)""".r
  val Line2 = """\[info\]   depends on: (.*)""".r
  def apply(): Unit =
    val inFile = java.io.File("logs/_dependencies.log")
    if inFile.exists then
      val in = io.Source.fromFile(inFile)
      val out = SplitLog.makeWriter("dependencies.txt")
      val tuples =
        for case Seq(Line1(project), Line2(depends)) <- in.getLines.grouped(2).toSeq
        yield (project, depends)
      for (project, depends) <- tuples.sortBy(_._1).sortBy(_._2.length) do
        out.println(s"$project: $depends")
      out.close()

// main
def log = io.Source.fromFile(args(0))
println("<pre>")
ClocReport(log)
val unexpectedFailureCount = SuccessReport(log)
println("</pre>")
SplitLog(log)
UpdateDependencies()
sys.exit(unexpectedFailureCount.getOrElse(1))
