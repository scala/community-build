#!/usr/local/bin/env -S scala-cli shebang --quiet

// to narrow projs.conf to just one project and its dependencies:
//   % ./narrow.sc scalacheck
// or several
//   % ./narrow.sc scalacheck scalatest specs2

// dependencies.txt is the source of truth for what dependencies to use

//> using scala "3.2.1"
//> using option "-source:future"
//> using lib "com.lihaoyi::os-lib:0.9.0"

// read dependency information
val tree: Map[String, Set[String]] =
  val deps = os.pwd / "dependencies.txt"
  val Regex = """(\S+): (.*)""".r
  os.read.lines(deps).map {
    case Regex(proj, deps) =>
      proj -> deps.split(", ").toSet
  }.toMap

// build list of targets
val targets: Set[String] =
  (args.toSet
    ++ args.flatMap(tree)
    -- Set("scala", "cloc-plugin"))

// write results
val projs = os.pwd / "projs.conf"
os.write.over(projs,
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
os.write.append(projs,
  targets.toSeq.sorted.iterator.map(p => "  ${vars.proj." + p + "}\n"))
os.write.append(projs,
  "\n]}\n")
