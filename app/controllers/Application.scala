package controllers

import play.api._
import play.api.mvc._
import models.{Group, Chart, ComputerNode, RealTimeChart}
import scala.concurrent.Future
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Play.current
import java.nio.file.{Paths, Files}
import scala.sys.process._

object Application extends Controller {

  val success = Ok("success\n")
  val error = BadRequest("error\n")
  val pointsMax = 100

  object InitAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
      if (RealTimeChart.readyInfo == "")
        Future.successful(error)
      else
        block(request)
    }
  }

  object RuntimeAction extends ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
      if (RealTimeChart.isReady)
        block(request)
      else
        Future.successful(Ok(views.html.error(RealTimeChart.readyInfo)))
    }
  }

  def index = RuntimeAction {
    Ok(views.html.nodes(RealTimeChart.getServerNames, RealTimeChart.getClientNames))
  }

  private def handleNone(f: => Unit) = {
    try {
      f
      success
    } catch {
      case _: NoSuchElementException => error
    }
  }

  def initNodes(nodeType: String, names: String) = InitAction {
    RealTimeChart.addNodes(names.split(",").map(ComputerNode(_, nodeType)).toList)
    success
  }

  def initGroups(nodeType: String, name: String, groups: String) = InitAction {
    handleNone {
      RealTimeChart.getNode(name, nodeType).setGroups(groups.split(",").map(Group).toList)
    }
  }

  def initCharts(nodeType: String, name: String, group: String, charts: String) = InitAction {
    handleNone {
      RealTimeChart.getNode(name, nodeType).getGroup(group).setCharts(charts.split(",").map(Chart).toList)
    }
  }

  def initChart(nodeType: String, name: String, group: String, chart: String, series: String, title: String, yAxisTitle: String) = InitAction {
    handleNone {
      RealTimeChart.getNode(name, nodeType).getGroup(group).getChart(chart).setSeries(series.split(",").toList)
        .setTitle(title)
        .setYAxisTitle(yAxisTitle)
    }
  }

  def ingestData(nodeType: String, name: String, group: String, chart: String, data: String) = RuntimeAction {
    handleNone {
      RealTimeChart.getNode(name, nodeType).getGroup(group).getChart(chart).addPoints(data.split(",").map(_.toDouble))
    }
  }

  def showChart(nodeType: String, name: String, group: String, chart: String, pointsTotal: Int) = RuntimeAction {
    try {
      val node = RealTimeChart.getNode(name, nodeType)
      val theGroup = node.getGroup(group)
      val theChart = theGroup.getChart(chart)
      val urlPrefix = s"/$nodeType/"
      Ok(views.html.charts(node.getGroups, theGroup, theChart, urlPrefix + name + '/',
        if (pointsTotal > pointsMax) pointsMax else pointsTotal))
    } catch {
      case _: NoSuchElementException => error
    }
  }

  def showNodeIndex(nodeType: String, name: String) = RuntimeAction {
    try {
      val node = RealTimeChart.getNode(name, nodeType)
      val theGroup = node.getGroups.head
      val theChart = theGroup.getCharts.head
      val urlPrefix = s"/$nodeType/"
      Ok(views.html.charts(node.getGroups, theGroup, theChart, urlPrefix + name + '/', 20))
    } catch {
      case _: NoSuchElementException => error
    }
  }

  def getData(nodeType: String, name: String, group: String, chart: String, pointsTotal: Int) = RuntimeAction {
    try {
      Ok(RealTimeChart.getNode(name, nodeType).getGroup(group).getChart(chart).lastNPointsToString(pointsTotal))
    } catch {
      case _: NoSuchElementException => error
    }
  }

  def getSocket(nodeType: String, name: String, group: String, chart: String) = WebSocket.using[String] {request =>
    val error = (Iteratee.consume[String](), Enumerator.eof[String])
    if (!RealTimeChart.isReady)
      error
    else {
      try {
        val (out, channel) = Concurrent.broadcast[String]
        val theChart = RealTimeChart.getNode(name, nodeType).getGroup(group).getChart(chart)
        theChart.addChannel(channel)
        val in = Iteratee.skipToEof[String].map(_ => theChart.removeChannel(channel))
        (in, out)
      } catch {
        case _: NoSuchElementException => error
      }
    }
  }

  def finishChart(path: String) = RuntimeAction {
    val dataDir = s"$path/data"
    val imageDir = s"$path/images"
    prepareResult(path)
    Files.createDirectory(Paths.get(dataDir))
    Files.createDirectory(Paths.get(imageDir))
    RealTimeChart.generateTopology(dataDir)
    RealTimeChart.generateDataFiles(dataDir)
    RealTimeChart.generateStaticImages(imageDir)
    success
  }

  private def prepareResult(destination: String) {
    clearDir()
    Files.copy(Play.resourceAsStream("public/result.zip").get, Paths.get("/tmp/result.zip"))
    Seq("unzip", "-q", "/tmp/result.zip", "-d", "/tmp").!
    Files.move(Paths.get("/tmp/result"), Paths.get(destination))
    clearDir()
  }

  private def clearDir() {
    "rm -f /tmp/result.zip".!
    "rm -rf /tmp/result".!
  }
}