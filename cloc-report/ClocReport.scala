object ClocReport extends App {

  // there's sometimes an extra "[info]" in the line, so we have to be careful
  val Regex = """\[([^\]]+)\] (\[info\] )?\*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r

  val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
  for (line <- io.Source.fromFile(args(0)).getLines)
    line match {
      case Regex(project, _, count) =>
        results(project) = results(project) + count.toInt
      case _ =>
    }

  println("Lines of Scala code recompiled during this run only:")

  for ((project, sum) <- results.toSeq.sortBy(_._2).reverse)
    println(f"$sum%9d $project")

  println(f"${results.values.sum}%9d TOTAL")

}
