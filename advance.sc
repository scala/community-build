#!/usr/local/bin/env -S scala-cli shebang -O -source:future

// to advance all projects:
//   % ./advance.sc scalacheck
// to advance just selected ones:
//   % ./advance.sc scalacheck scalatest specs2

using scala 3.1.1-RC1

using lib org.scala-lang.modules::scala-parallel-collections:1.0.3
using lib com.github.pathikrit:better-files_2.13:3.9.1

import scala.collection.parallel.CollectionConverters.*  // for .par
import better.files.*

def munge(l: String, replacement: String): String =
  if (l.startsWith("""  uri: "https://github"""))
  then s"""  uri: "$replacement""""
  else l

def getSha(repo: String, ref: String): String =
  if (ref.matches("\\p{XDigit}{40}"))
  then ref
  else
    import scala.sys.process.*
    val cmd = s"git ls-remote $repo $ref"
    def matches(s: String) =
      s.containsSlice("refs/heads/") || s.containsSlice("refs/tags/")
    Process(cmd).lazyLines.find(matches) match
      case Some(line) =>
        line.split("\\s").head
      case None =>
        throw new RuntimeException(s"$repo $ref")

// regexes for matching first line of each project's config
// note that we require an explicit tag or branch name,
// or a SHA (which must be the whole 40 characters).
// (dbuild defaults to `master`, but I prefer to make it explicit,
// to avoid confusion over whether the default is `master` or
// the repo's own default branch, which might be called something else)
object Regexes:
  // I would prefer to just have these as top-level `val`s but the script
  // deadlocks if I do that. some problem with parallel collections and
  // script wrappers, I guess. I didn't investigate
  val GitHub = """// (https://github.com/\S*.git)#(\S*)(\s*.*)""".r
  val Ivy = """// ivy:.*""".r
import Regexes.*

for
  file <- File("proj").list(_.extension == Some(".conf")).toSeq.par
  if args.isEmpty || args.contains(file.nameWithoutExtension)
do
  val lines = file.lines.to(Vector)
  lines.head match
    case GitHub(repo, ref, comment) =>
      val uri = s"$repo#${getSha(repo, ref)}"
      println(uri)  // indicate progress
      file.clear()
      file.printLines(lines.map(munge(_, uri)))
    case Ivy() =>
      // okay to skip
    case bad =>
      throw new IllegalArgumentException(bad)
