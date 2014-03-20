package controllers

import play.api._
import play.api.mvc._
import models.{Group, Chart, ComputerNode, RealTimeChart}
import scala.concurrent.Future
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.concurrent.Execution.Implicits._
import java.text.SimpleDateFormat
import java.util.Date
import java.io.File
import play.api.Play.current
import java.nio.file.{Paths, Files}
import scala.sys.process._

object Application extends Controller {

  val success = Ok("success\n")
  val error = BadRequest("error\n")

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
      if (RealTimeChart.readyInfo == "" || RealTimeChart.readyInfo == "clients")
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
      RealTimeChart.getNode(name, nodeType).getGroup(group).getChart(chart).addPoints(data.split(","))
    }
  }

  def showGroupCharts(nodeType: String, name: String, group: String, pointsTotal: Int) = RuntimeAction {
    try {
      val node = RealTimeChart.getNode(name, nodeType)
      val theGroup = node.getGroup(group)
      val urlPrefix = s"/$nodeType/"
      Ok(views.html.charts(node.getGroupNames, group, urlPrefix + name + '/', theGroup.getCharts, pointsTotal))
    } catch {
      case _: NoSuchElementException => error
    }
  }

  def showNodeIndex(nodeType: String, name: String) = RuntimeAction {
    try {
      val node = RealTimeChart.getNode(name, nodeType)
      val theGroup = node.getFirstGroup
      val urlPrefix = s"/$nodeType/"
      Ok(views.html.charts(node.getGroupNames, theGroup.name, urlPrefix + name + '/', theGroup.getCharts, 20))
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
    if (RealTimeChart.readyInfo != "")
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
    val date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date)
    val dataDir = s"$path/$date/data"
    prepareResult(s"$path/$date/")
    Files.createDirectory(Paths.get(dataDir))
    RealTimeChart.generateTopology(dataDir)
    RealTimeChart.generateDataFiles(dataDir)
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