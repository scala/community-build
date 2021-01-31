scalaVersion := "3.0.0-M3"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.8.0" cross(CrossVersion.for3Use2_13),
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.0",
)

// run.sh wants an exit code
trapExit := false
