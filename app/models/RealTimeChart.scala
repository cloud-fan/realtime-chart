package models

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import play.api.libs.iteratee.Concurrent.Channel
import scala.xml.XML
import java.nio.file.{Paths, Files}
import java.nio.charset.Charset
import org.jfree.chart.{ChartUtilities, ChartFactory}
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import java.io.File

/**
 * Created by cloud on 1/21/14.
 */

class MyContainer[A <: {def name : String; def readyInfo : String}] {

  private[models] var elements: List[A] = Nil

  def addElements(elements: List[A]) {
    this.elements ++= elements
  }

  def getElementByName(name: String) = elements.find(_.name == name).get

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

  def getGroups = this.groups.elements

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
  
  def saveImages(imageDir: String) {
    for (group <- groups.elements)
      group.saveImages(imageDir, nodeType, name)
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

  def saveImages(imageDir: String, nodeType: String, node: String) {
    for (chart <- charts.elements if !chart.isEmpty)
      chart.saveImages(imageDir, nodeType, node, name)
  }
}

case class Chart(name: String) {

  private var _series: List[String] = Nil

  def series = _series

  private var _title = ""

  def title = _title

  private var _yAxisTitle = ""

  def yAxisTitle = _yAxisTitle

  def readyInfo = if (_series != Nil) "" else " chart " + name

  def isEmpty = dateTimes.length == 0

  private val dateTimes = new ArrayBuffer[Long]()
  private val pointsForAllSeries = new ArrayBuffer[Array[Double]]()

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

  def addPoints(points: Array[Double]) {
    val x = System.currentTimeMillis()
    dateTimes += x
    pointsForAllSeries += points
    channels.foreach(_.push(x + "#" + points.mkString(",")))
  }

  def lastNPointsToString(n: Int) = if (dateTimes.length == 0)
    "!" + System.currentTimeMillis() + "#" + series.mkString("#")
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
    val path = Paths.get(dataDir).resolve(nodeType + "-" + node + "-" + group + "-" + name + ".csv")
    val bw = Files.newBufferedWriter(path, Charset.defaultCharset())
    try {
      bw.write(series.mkString(","))
      bw.newLine()
      bw.write(dateTimes.mkString(","))
      bw.newLine()
      pointsForAllSeries.foreach((data) => {
        bw.write(data.mkString(","))
        bw.newLine()
      })
    } finally
      bw.close()
  }

  def saveImages(imageDir: String, nodeType: String, node: String, group: String) {
    val xySeries = series.map( s => new XYSeries(s))
    (0 until pointsForAllSeries.size).foreach { pointsIndex =>
      (0 until xySeries.size).foreach { seriesIndex =>
        xySeries(seriesIndex).add(pointsIndex, pointsForAllSeries(pointsIndex)(seriesIndex), false)
      }
    }
    val data = new XYSeriesCollection()
    xySeries.foreach(data.addSeries)
    val chart = ChartFactory.createXYLineChart(name, null, yAxisTitle, data)
    val render = new XYLineAndShapeRenderer()
    render.setBaseShapesVisible(false)
    chart.getXYPlot.setRenderer(render)
    ChartUtilities.saveChartAsPNG(new File(imageDir, nodeType + "-" + node + "-" + group + "-" + name + ".png"), chart, 600, 400)
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

  def isReady = {
    readyInfo == "" || readyInfo == "clients"
  }

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

  def generateStaticImages(imageDir: String) {
    for (node <- nodes.elements)
      node.saveImages(imageDir)
  }
}
