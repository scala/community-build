#!/usr/local/bin/env -S scala-cli shebang

// to narrow projs.conf to just one project and its dependencies:
//   % ./narrow.sc scalacheck
// or several
//   % ./narrow.sc scalacheck scalatest specs2

// dependencies.txt is the source of truth for what dependencies to use

//> using scala "3.1.3"
//> using option "-source:future"
//> using lib "com.github.pathikrit:better-files_2.13:3.9.1"

import better.files.*

// read dependency information
val tree: Map[String, Set[String]] =
  val deps = File("dependencies.txt")
  val Regex = """(\S+): (.*)""".r
  deps.lines.map {
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
  targets.toSeq.sorted.iterator.map(p => "  ${vars.proj." + p + "}"))
projs.append("\n]}\n")
