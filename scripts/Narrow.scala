import better.files._

@main def narrow(args: String*) =

  // read dependency information
  val tree: Map[String, Set[String]] =
    val deps = File("dependencies.txt")
    val Regex = """(\S+): (.*)""".r
    deps.lines.map{
      case Regex(proj, deps) =>
        proj -> deps.split(", ").toSet
    }.toMap

  // build list of targets
  val targets: Set[String] =
    (args.toSet
      ++ args.flatMap(tree)
      -- Set("scala", "cloc-plugin"))

  // write results
  val projs = File("projs.conf")
  projs.clear()
  projs.append(
    """|build += {
       |
       |  space: scala.main
       |
       |  // true = build project-level dependencies from source
       |  // false = get build-level dependencies as binaries
       |  check-missing: [ true, false ]
       |
       |  cross-version: [ disabled, standard ]
       |  extraction-version: ${vars.scala-version}
       |  sbt-version: ${vars.sbt-version}
       |  sbt-java-options: ${vars.sbt-java-options}
       |
       |  projects: [
       |
       |""".stripMargin)
  projs.printLines(
    targets.toSeq.sorted.iterator.map(
      p => "  ${vars.proj." + p + "}"))
  projs.append("\n]}\n")
