name := "realtime-chart"

version := "1.0"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

play.Project.playScalaSettings

val zipResult = taskKey[Unit]("zip the result html files")

zipResult := {
  import scala.sys.process._
  import java.nio.file.{Files, Paths}
  import java.io.File
  if (Files.isDirectory(Paths.get((classDirectory in Compile).value.getCanonicalPath))) {
    val zipPath = new File((classDirectory in Compile).value, "public/result.zip").getCanonicalPath
    Files.deleteIfExists(Paths.get(zipPath))
    Process(Seq("zip", "-rq", zipPath, "result"), new File("public")) !
  }
}

compile in Compile := {
  val result = (compile in Compile).value
  zipResult.value
  result
}
