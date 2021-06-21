scalaVersion := "3.0.1-RC1"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.9.1" cross(CrossVersion.for3Use2_13),
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.3",
)

// run.sh wants an exit code
trapExit := false
