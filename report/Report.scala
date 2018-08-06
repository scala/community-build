object Report extends App {
  def log = io.Source.fromFile(args(0))
  ClocReport(log)
  SuccessReport(log)
}

object ClocReport {
  def apply(log: io.Source): Unit = {
    // there's sometimes an extra "[info]" in the line, so we have to be careful
    val Regex = """\[([^\]]+)\] (\[info\] )?\*\* COMMUNITY BUILD LINE COUNT: (\d+)""".r
    val results = collection.mutable.Map.empty[String, Int].withDefaultValue(0)
    for (Regex(project, _, count) <- log.getLines)
      results(project) += count.toInt
    println("Lines of Scala code recompiled during this run only:")
    for ((project, sum) <- results.toSeq.sortBy(_._2).reverse)
      println(f"$sum%9d $project")
    println(f"${results.values.sum}%9d TOTAL")
  }
}

object SuccessReport {
  def apply(log: io.Source): Unit = {
    val lines = log.getLines.dropWhile(!_.contains("---==  Execution Report ==---"))
    var success, failed, didNotRun = 0
    for (line <- lines if line.startsWith("[info] Project "))
      if (line.contains(": SUCCESS "))
        success += 1
      else if (line.contains(": FAILED "))
        failed += 1
      else if (line.contains(": DID NOT RUN "))
        didNotRun += 1
    val total = success + failed + didNotRun
    println(s"SUCCESS $success FAILED $failed DID NOT RUN $didNotRun TOTAL $total")
  }
}
