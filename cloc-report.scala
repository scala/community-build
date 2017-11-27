#!/usr/bin/env sbt -Dsbt.version=0.13.16 -Dsbt.main.class=sbt.ScriptMain --error

val Regex = """\[(.+)\] \*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r
val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
for (line <- io.Source.fromFile(args(0)).getLines)
  line match {
    case Regex(project, count) =>
      results(project) = results(project) + count.toInt
    case _ =>
  }

println("Lines of Scala code recompiled during this run only:")

for ((project, sum) <- results.toSeq.sortBy(_._2).reverse)
  println(f"$sum%9d $project")

println(f"${results.values.sum}%9d TOTAL")


