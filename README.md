# 实时画图工具
此工具主要是为了解决性能测试过程中的画图问题。有时候一个性能测试要跑好几天，我们不想等到整个测试跑完再分析日志画出例如CPU，内存，IO等图表，而是在测试运行过程能通过浏览器动态的看到当前已经产生的日志所能生成的性能图。
## 界面展示
![nodes](http://i41.tinypic.com/x3jgy.png)

通过这个界面选择要查看的节点

![charts](http://i41.tinypic.com/fckzev.png)

通过这个界面查看具体的图表，右上角可以选择查看最近的几个数据点
## 用法
我使用了play2框架来开发这个工具，故实际上此工具就是一个web服务器。外部通过HTTP请求来和它进行交互，通过浏览器来浏览性能图。同时，它主要是针对集群性能测试进行设计的，比如，用有2个节点的YCSB client集群来测试有3个节点的Cassandra server集群。

该工具提供了一个zip包，解压之后，进入目录，运行：bin/realtime-chart即可。运行时会在目录下生成一个RUNNING_PID文件，里面记录了这个server的进程号，所以，你可以在脚本里很容易的kill它。
### 图表组织结构
上面提到了此工具的设计目标是为了集群性能测试，故组织结构的最上层是集群类型，server还是client？

集群类型下面则是具体的机器节点，比如：lab105， xen139v01

机器节点下面则是图表所属的组，比如system组，包含CPU图，Memory图等。

组下面就是图表了，图表下面则是系列，比如CPU图，包含cpu1，cpu2...等系列，来表示多核CPU所有CPU的运行情况。

一个实际例子：

![example](http://i41.tinypic.com/2a5b2w3.png)
### 如何用HTTP请求来进行交互
显然，想要让这个工具开始工作，首先要让它知道你想画的图的拓扑结构，即上个章节提到的图表组织结构。

以上个章节里为了举例给出的结构图为目标，下面将讲解如何利用HTTP请求让画图工具生成这个图表组织结构。（不同编程语言有不同发送HTTP请求的方式，下面的例子是基于shell的）

    curl "http://localhost:9000/server/init?names=lab105,lab108"
    curl "http://localhost:9000/client/init?names=xen139v01"
    curl "http://localhost:9000/server/lab105/init?groups=system,disk"
    curl "http://localhost:9000/server/lab105/system/init?charts=cpu,mem"
    curl "http://localhost:9000/server/lab105/system/cpu/init?series=cpu1,cpu2,cpu3,cpu4&title=CPU+utilization&yAxisTitle=percentage+(%25)"
    curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,67.6,98.1,45,8"
    ...
    ...
    curl "http://localhost:9000/finish?path=%2fvar%2fwww"
上面的代码虽然并不完整，但已经把所有的HTTP请求的种类都展示了一遍。

* server部分和client部分的请求几乎一样，差别只是URI开头一个是`server`一个是`client`
* 先要给定server/client节点的名字，通过`names`参数指定，多个节点之间用逗号分隔
* 然后要给定每个节点的图表组，通过`groups`参数指定，多个组之间用逗号分隔
* 类似上面，通过`charts`给定每个图表组的图表
* 指定图表的具体信息，通过`series`参数指定系列，`title`参数指定图表标题,`yAxisTitle`参数指定y轴标题
* y轴标题的格式应该是xxxx (unit)，即标题名字后面跟上一个或多个空格，后面再跟上一对括号，括号里是y轴的单位。例子：percentage (%), size (mb)
* 需要注意的是，图表标题和y轴标题经常会包含特殊字符，导致不能放到URL里，需要进行URL编码。可以使用<http://tool.chinaz.com/Tools/URLEncode.aspx>来进行编码（上面的代码例子里就使用了URL编码）
* 最后就是给每个图表持续发送数据，使用`data`参数指定，数据个数必须和对应图表的系列个数一样，并且必须是数值类型
* 考虑到有时候仅仅需要检测服务器的状态，或者是客户端的测试软件不需要画图，比如Hibench，故server节点不能为空，client节点可以为空，即不去初始化client那块的东西
* 当整个性能测试运行完之后，你可以参照上面代码例子的最后一行，给画图工具发送finish命令，同时指定你要保存图表到哪个目录。该工具会在你指定的目录下生成一个以时间戳命名的目录，目录里是一个静态网站，所以你可以把这个目录放到HTTP服务器的数据目录里，通过HTTP服务器访问这个静态网站，就能看到你运行过程中产生的所有图表的所有数据。当然，目录参数也需要URL编码
* 建议使用其他高级编程语言比如Python，自带URL编码库，发送HTTP请求会比较方便
* 最后遗憾的是，该工具还不支持自己关闭，需要用户在使用完之后手动kill，可以使用我在用法里提到的方法，通过RUNNING_PID文件获取进程号，然后kill该进程
## 使用场景
一般的性能测试工具，主要有几个功能。

1. 在server节点进行性能监控，比如运行iostat, ifstat, vmstat等，并将结果保存到日志里
2. 在client节点运行性能测试软件，比如YCSB，并将结果收集到日志中
3. 等到测试跑完，分析收集好的log，画出性能图。

第1，2步收集的log是实时变化的，比如iostat命令可以10秒输出一次，YCSB也可以10秒输出一次状态，所以，我们可以不用等到最后才分析log，可以在运行过程中监控log文件的更改，一般是不断增加，然后对增量的部分进行分析，把分析出来的数据传递给这个实时画图工具。
## 限制
目前此实时画图工具并不是线程安全的，即你不能同时用多个线程去初始化同一个节点的信息。初始化完之后，也不能多线程去给同一个图表塞数据。由于此工具是面向开发者的，开发者在编写性能测试工具的时候，可以一个日志文件一个分析线程，同时这个线程负责给自己的日志文件对应的那些图表做初始化和塞数据，这样就不会有多线程冲突的问题了。
