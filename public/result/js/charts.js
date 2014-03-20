function makeChart (chartDom, title, yAxisTitle, dataFile) {
    Highcharts.setOptions({
        global: {
            useUTC: false
        }
    });

    var nodeName = dataFile.split("-")[1]
    var yAxisValueSuffix = " "
    var match = yAxisTitle.match(/\s\((.+)\)$/)
    if (match != null)
        yAxisValueSuffix = match[1]

    var options = {
        chart: {
            type: 'spline',
            animation: false,
            zoomType: 'x',
        },
        title: {
            text: nodeName + " -- " + title
        },
        credits: {
            enabled: false
        },
        yAxis: {
            title: {
                text: yAxisTitle
            }
        },
        rangeSelector: {
            buttons: [{
                type: 'minute',
                count: 3,
                text: '3m'
            }, {
                type: 'minute',
                count: 10,
                text: '10m'
            }, {
                type: 'hour',
                count: 1,
                text: '1h'
            }, {
                type: 'hour',
                count: 3,
                text: '3h'
            }, {
                type: 'all',
                text: 'All'
            }],
            inputDateFormat: '%H:%M:%S',
            inputEditDateFormat: '%H %M %S',
            inputPosition: {
                align: "center"
            },
            selected: 4
        },
        tooltip: {
            crosshairs: true,
            shared: true,
            shadow: false,
            formatter: function() {
                var s = Highcharts.dateFormat('%H:%M:%S', this.x);
                $.each(this.points, function(i, point) {
                    s += '<br/><b>'+ point.series.name +'</b>: '+ Highcharts.numberFormat(point.y) + " " + yAxisValueSuffix;
                });
                return s;
            }
        },
        legend: {
            layout: 'vertical',
            align: 'right',
            verticalAlign: 'top',
            borderWidth: 0,
            enabled: true
        },
        series: []
    }

    $.get("data/"+dataFile).done(function(data){
        var seriesArray = produceAllSeries(data)
        for(var i=0;i<seriesArray.length;i++) {
            options.series.push(seriesArray[i])
        }
        chartDom.highcharts('StockChart',options)
    })

    function produceAllSeries (initData) {
        var data = initData.split("\n")
        var seriesNames = data[0].split(",")
        var dateTimes = data[1].split(",")
        data.splice(0,2)
        var seriesCount = seriesNames.length
        var pointsCount = dateTimes.length
        var seriesArray = new Array(seriesCount)
        for(var i=0;i<seriesCount;i++) {
            seriesArray[i] = {}
            seriesArray[i]["name"] = seriesNames[i]
            seriesArray[i]["data"] = []
        }
        for(var i=0;i<pointsCount;i++) {
            var yList = data[i].split(",")
            for(var j=0;j<seriesCount;j++) {
                seriesArray[j]["data"].push({
                    x:new Date(dateTimes[i]).getTime(),
                    y:parseFloat(yList[j])
                })
            }
        }
        return seriesArray
    }
}