package models

import scala.collection.mutable.ArrayBuffer
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable
import play.api.libs.iteratee.Concurrent.Channel
import scala.xml.XML
import java.nio.file.{FileSystems, Files}
import java.nio.charset.Charset

/**
 * Created by cloud on 1/21/14.
 */

class MyContainer[A <: {def name : String; def readyInfo : String}] {

  private[models] var elements: List[A] = Nil

  def addElements(elements: List[A]) {
    this.elements ++= elements
  }

  def getElementByName(name: String) = elements.find(_.name == name).get

  def getElementNames = elements.map(_.name)

  def readyInfo = {
    if (elements == Nil)
      " "
    else
      elements.find(_.readyInfo != "").map(_.readyInfo).getOrElse("")
  }
}

case class ComputerNode(name: String, nodeType: String) {

  private val groups = new MyContainer[Group]()

  def setGroups(groups: List[Group]) {
    this.groups.addElements(groups)
  }

  def getGroup(name: String) = this.groups.getElementByName(name)

  def getGroupNames = this.groups.getElementNames

  def getFirstGroup = this.groups.elements.head

  def readyInfo = {
    this.groups.readyInfo match {
      case "" => ""
      case info => " " + nodeType + " " + name + info
    }
  }

  def xml = {
    <node name={name}>
      {for (group <- groups.elements) yield {
      group.xml
    }}
    </node>
  }

  def writeCSV(dataDir: String) {
    for (group <- groups.elements)
      group.writeCSV(dataDir, nodeType, name)
  }
}

case class Group(name: String) {

  private val charts = new MyContainer[Chart]()

  def setCharts(charts: List[Chart]) {
    this.charts.addElements(charts)
  }

  def getChart(name: String) = {
    this.charts.getElementByName(name)
  }

  def getCharts = this.charts.elements

  def readyInfo = {
    this.charts.readyInfo match {
      case "" => ""
      case info => " group " + name + info
    }
  }

  def xml = {
    <group name={name}>
      {for (chart <- charts.elements if !chart.isEmpty) yield {
      <chart name={chart.name} title={chart.title} yAxisTitle={chart.yAxisTitle}></chart>
    }}
    </group>
  }

  def writeCSV(dataDir: String, nodeType: String, node: String) {
    for (chart <- charts.elements if !chart.isEmpty)
      chart.writeCSV(dataDir, nodeType, node, name)
  }
}

case class Chart(name: String) {

  private val df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

  private var _series: List[String] = Nil

  def series = _series

  private var _title = ""

  def title = _title

  private var _yAxisTitle = ""

  def yAxisTitle = _yAxisTitle

  def readyInfo = if (_series != Nil) "" else " chart " + name

  def isEmpty = dateTimes.length == 0

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
    val x = df.format(new Date)
    dateTimes += x
    pointsForAllSeries += points
    channels.foreach(_.push(x + "#" + points.mkString(",")))
  }

  def lastNPointsToString(n: Int) = if (dateTimes.length == 0)
    "!" + df.format(new Date) + "#" + series.mkString("#")
  else {
    val dropValue = dateTimes.length - n
    series.mkString("#") + "\n" +
      dateTimes.drop(dropValue).mkString(",") + "\n" +
      pointsForAllSeries.drop(dropValue).map(_.mkString(",")).mkString("\n")
  }

  def xml = {
    <chart name={name} title={title} yAxisTitle={yAxisTitle}></chart>
  }

  def writeCSV(dataDir: String, nodeType: String, node: String, group: String) {
    val path = FileSystems.getDefault.getPath(dataDir).resolve(nodeType + "-" + node + "-" + group + "-" + name + ".csv")
    val bw = Files.newBufferedWriter(path, Charset.defaultCharset())
    bw.write(series.mkString(",") + "\n")
    bw.write(dateTimes.mkString(",") + "\n")
    pointsForAllSeries.foreach((data) => bw.write(data.mkString(",") + "\n"))
    bw.close()
  }
}

object RealTimeChart {

  private val nodes = new MyContainer[ComputerNode]()

  def addNodes(nodes: List[ComputerNode]) {
    this.nodes.addElements(nodes)
  }

  def getNode(name: String, nodeType: String) = {
    nodes.elements.find(node => node.name == name && node.nodeType == nodeType).get
  }

  private def getNodesByNodeType(nodeType: String) = nodes.elements.filter(_.nodeType == nodeType)

  private def getNodeNamesByNodeType(nodeType: String) = getNodesByNodeType(nodeType).map(_.name)

  def getServerNames = getNodeNamesByNodeType("server")

  def getClientNames = getNodeNamesByNodeType("client")

  def readyInfo = {
    if (getNodesByNodeType("server") == Nil)
      "servers"
    else {
      nodes.readyInfo match {
        case "" => if (getNodesByNodeType("client") == Nil) "clients" else ""
        case info => info
      }
    }

  }

  def generateTopology(dataDir: String) {
    val xml =
      <root>
        <server>
          {for (node <- getNodesByNodeType("server")) yield {
          node.xml
        }}
        </server>
        <client>
          {for (node <- getNodesByNodeType("client")) yield {
          node.xml
        }}
        </client>
      </root>
    XML.save(dataDir + "/topology.xml", xml, "utf-8", true)
  }

  def generateDataFiles(dataDir: String) {
    for (node <- nodes.elements)
      node.writeCSV(dataDir)
  }
}
