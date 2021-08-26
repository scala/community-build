import better.files._
import scala.collection.parallel.CollectionConverters._  // for .par

@main def advance(args: String*) =
  // regexes for matching first line of each project's config
  // note that we require an explicit tag or branch name,
  // or a SHA (which must be the whole 40 characters).
  // (dbuild defaults to `master`, but I prefer to make it explicit,
  // to avoid confusion over whether the default is `master` or
  // the repo's own default branch, which might be called something else)
  val GitHub = """// (https://github.com/\S*.git)#(\S*)(\s*.*)""".r
  def filesIn(dir: String): Seq[File] =
    File(dir).list(_.extension == Some(".conf")).toSeq
  for
    file <- (filesIn("core") ++ filesIn("proj")).par
    if args.isEmpty || args.contains(file.nameWithoutExtension)
  do
    val lines = file.lines.to(Vector)
    lines.head match
      case GitHub(repo, ref, comment) =>
        val uri = s"$repo#${getSha(repo, ref)}"
        println(uri)  // indicate progress
        file.clear()
        file.printLines(lines.map(munge(_, uri)))
      case bad =>
        throw new IllegalArgumentException(bad)

def munge(l: String, replacement: String): String =
  if (l.startsWith("""  uri: "https://github"""))
  then s"""  uri: "$replacement""""
  else l

def getSha(repo: String, ref: String): String =
  if (ref.matches("\\p{XDigit}{40}"))
  then ref
  else
    import scala.sys.process._
    val cmd = s"git ls-remote $repo $ref"
    def matches(s: String) =
      s.containsSlice("refs/heads/") || s.containsSlice("refs/tags/")
    Process(cmd).lazyLines.find(matches) match
      case Some(line) =>
        line.split("\\s").head
      case None =>
        throw new RuntimeException(s"$repo $ref")
