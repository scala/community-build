@main def report(logPath: String) =
  def log = io.Source.fromFile(logPath)
  println("<pre>")
  ClocReport(log)
  val unexpectedFailureCount = SuccessReport(log)
  println("</pre>")
  SplitLog(log)
  UpdateDependencies()
  sys.exit(unexpectedFailureCount.getOrElse(1))

object ClocReport:

  // sometimes there's an extra "[info]" in the line, so we have to be careful
  val Regex = """\[([^\]]+)\] (?:\[info\] )?\*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r

  def apply(log: io.Source): Unit =
    val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    for Regex(project, count) <- log.getLines do
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

  val requiresJdk11Plus = Set[String](
    "airframe",  // they require JDK 11 for building
    "fs2",       // they require JDK 11 for building
  )

  val requiresJdk15Plus = Set[String](
    "shapeless-java-records",  // inherently requires JDK 15+
  )

  val jdk11Failures = Set[String](
    "kamon",              // uses com.sun stuff
    "scala-debugger",     // "object FieldInfo is not a member of package sun.reflect"
    "scala-refactoring",  // needs scala/bug#11125 workaround?
  )

  val jdk16Failures = Set[String](
    "akka-persistence-cassandra", // needs newer sbt-osgi
    "avro4s",        // test failure: com.sksamuel.avro4s.github.GithubIssue387
    "elastic4s",     // Unrecognized VM option 'CMSClassUnloadingEnabled'
    "ip4s",          // needs newer sbt-osgi
    "mockito-scala", // reflection-related test failures
    "play-doc",                 // Error creating extended parser class: Could not determine whether class 'play.doc.CodeReferenceParser$$parboiled' has already been loaded (Parboiled.java:58)
    "playframework", // Failed tests: play.mvc.HttpFormsTest
    "requests-scala",           // requests.RequestTests fails, unclear why
    "specs2-more",              // Error creating extended parser class: Could not determine whether class 'org.pegdown.Parser$$parboiled' has already been loaded (Parboiled.java:58)
    "twitter-util",  // Unrecognized VM option 'AggressiveOpts'
    "zinc",          // sbt.inc.Doc$JavadocGenerationFailed
  )

  val expectedToFail: Set[String] =
    System.getProperty("java.specification.version") match
      case "1.8" =>
        requiresJdk11Plus ++ requiresJdk15Plus
      case "11" =>
        jdk11Failures ++ requiresJdk15Plus
      case _ =>
        jdk11Failures ++ jdk16Failures

  def apply(log: io.Source): Option[Int] =
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun = 0
    val unexpectedSuccesses = collection.mutable.Buffer[String]()
    val unexpectedFailures = collection.mutable.Buffer[String]()
    val blockerCounts = collection.mutable.Map[String, Int]()
    for Regex(name, status, blockers) <- lines do
      status match
        case "EXTRACTION FAILED" =>
          return None
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
    val dir = java.io.File("../logs")
    dir.mkdirs()
    val lines = log.getLines
    while lines.hasNext do
      lines.next match
        case BeginDependencies() =>
          slurp(lines, makeWriter("../logs/_dependencies.log"), EndDependencies)
        case BeginExtract(name) =>
          slurp(lines, makeWriter(s"../logs/$name-extract.log"), EndExtract)
        case BeginBuild(name) =>
          slurp(lines, makeWriter(s"../logs/$name-build.log"), EndBuild)
        case _ =>

  import java.io.PrintWriter

  def makeWriter(path: String): PrintWriter =
    import java.io._
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
    val inFile = java.io.File("../logs/_dependencies.log")
    if inFile.exists then
      val in = io.Source.fromFile(inFile)
      val out = SplitLog.makeWriter("../dependencies.txt")
      val tuples =
        for Seq(Line1(project), Line2(depends)) <- in.getLines.grouped(2).toSeq
        yield (project, depends)
      for (project, depends) <- tuples.sortBy(_._1).sortBy(_._2.length) do
        out.println(s"$project: $depends")
      out.close()
