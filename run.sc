#!/usr/local/bin/env -S scala-cli shebang

// TODO: re-add `--quiet` above

// This script is suitable for local use.
// It is also invoked by Jenkins (from scripts/jobs/integrate/community-build).

// An older run script is in the Git history under the name `run.sh`.
// I didn't port every feature of that script to this version.
// We can add things back later as needed, I figure.

//> using scala "3.2.1"
//> using option "-source:future"
//> using lib "com.lihaoyi::os-lib:0.9.0"

println(s"""JAVA_HOME=${sys.env("JAVA_HOME")}""")
print("which java: ")
os.proc("which", "java").call(stdout = os.Inherit)
os.proc("java", "-version").call(stdout = os.Inherit)
println()

val dbuildVersion = "0.9.20"
println(s"dbuild version: $dbuildVersion")
def readNightly(): String =
  val prop = new java.util.Properties
  prop.load(os.read.inputStream(os.pwd / "nightly.properties"))
  Option(prop.getProperty("nightly")).get
val scalaVersion = sys.env.get("version").getOrElse(readNightly())
println(s"Scala version: $scalaVersion")
println()

def removeProjectBuilds(): Unit =
  println("removing temporary files...")
  println()
  os.remove.all(os.pwd / s"target-$dbuildVersion" / "project-builds")

// redundant to delete both at start and end, but just in case this was
// left lying around, let's free up the space (and inodes) now
removeProjectBuilds()

// Download and extract dbuild if we haven't already got it
if !os.isDir(os.pwd / s"dbuild-$dbuildVersion") then
  val url = s"https://github.com/lightbend-labs/dbuild/releases/download/v$dbuildVersion/dbuild-$dbuildVersion.tgz"
  os.proc("curl", "-L", "-O", url).call(stdout = os.Inherit)
  os.proc("tar", "xfz", s"dbuild-$dbuildVersion.tgz").call(stdout = os.Inherit)
  os.remove(os.pwd / s"dbuild-$dbuildVersion.tgz")

// use -n by default since running locally you don't want notifications sent,
// and on our Jenkins setup it doesn't actually work (for now anyway)
val dbuildArgs = Seq("-n", "community.conf")

// sigh, Ubuntu has `nodejs` but MacOS has `node`
def onPath(cmd: String): Boolean =
  os.proc("sh", "-c", s"hash $cmd")
    .call(stderr = os.Pipe, check = false)
    .exitCode == 0
val node =
  if onPath("nodejs")
  then "nodejs"
  else "node"

// we're ready to run dbuild
val command =
  s"dbuild-$dbuildVersion/bin/dbuild" +:
  "-n" +: "./community.conf" +:
  args
println(command.mkString(" "))
println()
val dbuildOut = s"dbuild-$dbuildVersion/dbuild.out"
val dbuild =
  os.proc(command).spawn(
    stdout = os.Pipe,
    mergeErrIntoOut = true,
    env = Map(
      "version" -> scalaVersion,
      "NODE" -> node),
  )
val tee = os.proc("tee", dbuildOut).call(stdin = dbuild.stdout, stdout = os.Inherit)
dbuild.join()

// helpfully repeat the build's UUID at the end of the log
// [info]  uuid = 03c94c9f9b185c7bbdb4b66be9ecf99b3cd8d73e
val idOption = os.read.lines(os.pwd / os.SubPath(dbuildOut)).collectFirst {
  case s"[info]  uuid = $id" => id
}
for id <- idOption do
  println(s"The repeatable UUID of this build was: $id")
println()

// free up space (and inodes) on Jenkins
removeProjectBuilds()

// report summary information (line counts, green project counts, ...?)
val reporter = os.proc("./report.sc", dbuildOut).call(check = false)
os.write.over(os.pwd / "report.html", reporter.out.text())
println(reporter.out.text())
sys.exit(reporter.exitCode)
