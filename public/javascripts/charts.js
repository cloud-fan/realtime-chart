function makeChart(title, yAxisTitle, pointsTotal, urlPrefix) {
    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    })
    var timeInterval = 10000
    var chart

    var yAxisValueSuffix = " "
    var match = yAxisTitle.match(/\s\((.+)\)$/)
    if (match != null)
        yAxisValueSuffix = match[1]
    var nodeName = urlPrefix.match(/^\/(?:server|client)\/(\w+)/)[1]
    var chartName = urlPrefix.match(/(\w+)\/$/)[1]

    $.get(urlPrefix + "data", {pointsTotal: pointsTotal}).done(function(initData){
        chart = new Highcharts.Chart({
            chart: {
                type: 'spline',
                renderTo: chartName,
                events: {
                    load: function() {
                        if(initData.charAt(0) != "!") {
                            var series = produceAllSeries(initData)
                            for(var i = 0; i < series.length; i++) {
                                this.addSeries(series[i])
                            }
                        }else {
                            var tmp = initData.split("#")
                            var systemTime = Number(tmp[0])
                            var names = tmp.slice(1)
                            for(var index in names) {
                                this.addSeries(produceEmptySeries(systemTime, names[index]))
                            }
                        }
                    }
                }
            },
            title: {
                text: nodeName + " -- " + title
            },
            credits: {
                enabled: false
            },
            xAxis: {
                type: 'datetime'
            },
            yAxis: {
                title: {
                    text: yAxisTitle
                },
            },
            tooltip: {
                crosshairs: true,
                shared: true,
                shadow: false,
                formatter: function() {
                    var s = Highcharts.dateFormat('%H:%M:%S', this.x)
                    $.each(this.points, function(i, point) {
                        s += '<br/><b>'+ point.series.name +'</b>: '+ Highcharts.numberFormat(point.y) + " " + yAxisValueSuffix
                    })
                    return s
                }
            },
            legend: {
                layout: 'vertical',
                align: 'right',
                verticalAlign: 'top',
                borderWidth: 0
            }
        })


        var websocket = new WebSocket("ws://" + location.host + urlPrefix + "socket")
        websocket.onclose = onSocketClose
        websocket.onmessage = onSocketMessage
        websocket.onerror = onSocketError
        function onSocketClose() {
            alert("socket is closed,please refresh")
        }

        function onSocketError(evt) {
            alert(evt.data)
        }

        function onSocketMessage(socketEvent) {
            var tmp = socketEvent.data.split("#");
            var yList = tmp[1].split(",");
            for(var index in chart.series) {
                chart.series[index].addPoint([Number(tmp[0]), parseFloat(yList[index])], true, true);
            }
        }

    })

    function produceAllSeries(initData) {
        var data = initData.split("\n")
        var seriesNames = data[0].split("#")
        var dateTimes = data[1].split(",")
        data.splice(0,2);
        var seriesCount = seriesNames.length
        var pointsCount = dateTimes.length
        var seriesArray = new Array(seriesCount)
        for(var i = 0; i < seriesCount; i++) {
            seriesArray[i] = {}
            seriesArray[i]["name"] = seriesNames[i]
            seriesArray[i]["data"] = []
        }
        if(pointsCount < pointsTotal) {
            var baseDateTime = Number(dateTimes[0])
            for(var i = pointsCount - pointsTotal; i < 0; i++) {
                var xValue = baseDateTime + i * timeInterval
                for(var j = 0; j < seriesCount; j++) {
                    seriesArray[j]["data"].push([xValue, 0])
                }
            }
        }
        for(var i = 0; i < pointsCount; i++) {
            var yList = data[i].split(",")
            for(var j = 0; j < seriesCount; j++) {
                seriesArray[j]["data"].push([Number(dateTimes[i]), parseFloat(yList[j])])
            }
        }
        return seriesArray
    }

    function produceEmptySeries(systemTime, seriesName) {
        var series = []
        var data = []
        series["name"] = seriesName
        for (i = 1 - pointsTotal; i <= 0; i++) {
            data.push([systemTime + i * timeInterval, 0])
        }
        series["data"] = data
        return series

    }

    function converStringToCoordinate(coordinateString) {
        var temp = coordinateString.split(",")
        return [Number(temp[0]), parseFloat(temp[1])]
    }
}
