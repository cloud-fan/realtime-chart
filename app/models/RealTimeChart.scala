package models

import scala.collection.mutable.ArrayBuffer
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable
import play.api.libs.iteratee.Concurrent.Channel

/**
 * Created by cloud on 1/21/14.
 */

class MyContainer[A <: {def name : String; def readyInfo : String}] {

  private[models] var elements: List[A] = Nil

  def setElements(elements: List[A]) {
    this.elements = elements
  }

  def getElementByName(name: String) = elements.find(_.name == name).get

  def getElementNames = elements.map(_.name)

  def readyInfo(info: String) = {
    if (elements == Nil)
      " "
    else
      elements.find(_.readyInfo != "").map(element => info + element.name + element.readyInfo).getOrElse("")
  }
}

case class ComputerNode(name: String) {

  private val groups = new MyContainer[Group]()

  def setGroups(groups: List[Group]) {
    this.groups.setElements(groups)
  }

  def getGroup(name: String) = this.groups.getElementByName(name)

  def getGroupNames = this.groups.getElementNames

  def getFirstGroup = this.groups.elements.head

  def readyInfo = {
    this.groups.readyInfo(", group ")
  }
}

case class Group(name: String) {

  private val charts = new MyContainer[Chart]()

  def setCharts(charts: List[Chart]) {
    this.charts.setElements(charts)
  }

  def getChart(name: String) = {
    this.charts.getElementByName(name)
  }

  def getCharts = this.charts.elements

  def readyInfo = {
    this.charts.readyInfo(", chart ")
  }
}

case class Chart(name: String) {

  object MyDateFormatter {
    private val df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    def formatDate(date: Date) = df.format(date)
  }

  private var _series: List[String] = Nil

  def series = _series

  private var _title = ""

  def title = _title

  private var _yAxisTitle = ""

  def yAxisTitle = _yAxisTitle

  def readyInfo = if (_series != Nil) "" else " "

  private val dateTimes = new ArrayBuffer[String]()
  private val pointsForAllSeries = new ArrayBuffer[Array[String]]()

  private val channels = new mutable.HashSet[Channel[String]]()

  def addChannel(channel: Channel[String]) {
    channels += channel
  }

  def removeChannel(channel: Channel[String]) {
    channels -= channel
  }

  def setSeries(series: List[String]) = {
    _series = series
    this
  }

  def setTitle(title: String) = {
    _title = title
    this
  }

  def setYAxisTitle(yAxisTitle: String) = {
    _yAxisTitle = yAxisTitle
    this
  }

  def addPoints(points: Array[String]) {
    val x = MyDateFormatter.formatDate(new Date)
    dateTimes += x
    pointsForAllSeries += points
    channels.foreach(_.push(x + "#" + points.mkString(",")))
  }

  def lastNPointsToString(n: Int) = if (dateTimes.length == 0)
    "!" + MyDateFormatter.formatDate(new Date) + "#" + series.mkString("#")
  else {
    val dropValue = dateTimes.length - n
    series.mkString("#") + "\n" +
      dateTimes.drop(dropValue).mkString(",") + "\n" +
      pointsForAllSeries.drop(dropValue).map(_.mkString(",")).mkString("\n")
  }
}

object RealTimeChart {

  private val servers = new MyContainer[ComputerNode]()
  private val clients = new MyContainer[ComputerNode]()

  def setNodes(nodes: List[ComputerNode], nodeType: String) {
    nodeType match {
      case "server" => servers.setElements(nodes)
      case "client" => clients.setElements(nodes)
    }
  }

  def getNode(name: String, nodeType: String) = {
    nodeType match {
      case "server" => servers.getElementByName(name)
      case "client" => clients.getElementByName(name)
    }
  }

  def getServerNames = servers.getElementNames

  def getClientNames = clients.getElementNames

  def readyInfo = {
    if (servers.elements == Nil)
      "servers"
    else if (clients.elements == Nil)
      "clients"
    else {
      servers.readyInfo("server ") match {
        case "" => clients.readyInfo("client ")
        case s: String => s
      }
    }
  }
}
