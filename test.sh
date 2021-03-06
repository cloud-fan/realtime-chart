curl "http://localhost:9000/server/init?names=lab105,lab108"
curl "http://localhost:9000/server/lab108/init?groups=ga"
curl "http://localhost:9000/server/lab108/ga/init?charts=ca"
curl "http://localhost:9000/server/lab108/ga/ca/init?series=sa&title=a&yAxisTitle=a"
curl "http://localhost:9000/server/lab105/init?groups=system,disk"
curl "http://localhost:9000/server/lab105/system/init?charts=cpu,mem"
curl "http://localhost:9000/server/lab105/system/cpu/init?series=cpu1,cpu2,cpu3,cpu4&title=CPU+utilization&yAxisTitle=percentage+(%25)"
curl "http://localhost:9000/server/lab105/system/mem/init?series=swaped,free,buff,cached&title=MEM+utilization&yAxisTitle=percentage+(%25)"
curl "http://localhost:9000/server/lab105/disk/init?charts=ca"
curl "http://localhost:9000/server/lab105/disk/ca/init?series=sa&title=a&yAxisTitle=a"
curl "http://localhost:9000/client/init?names=xen139v01"
curl "http://localhost:9000/client/xen139v01/init?groups=ga"
curl "http://localhost:9000/client/xen139v01/ga/init?charts=ca"
curl "http://localhost:9000/client/xen139v01/ga/ca/init?series=sa&title=a&yAxisTitle=a"

curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=98.8,78.4,86.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=61.2,18.8,1.3,14.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,88.4,66.7,78,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=63.2,13.8,5.3,12.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=88.8,48.4,76.7,68,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=86.2,1.2,3.3,7.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=89.8,78.4,79.7,78,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=96.2,3.2,4.3,8.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=91.8,80.4,69.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=86.2,13.2,4.8,9.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=89.8,78.4,79.7,78,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=96.2,3.2,4.3,8.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=79.8,68.4,69.7,58,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=66.2,23.2,4.8,9.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=91.8,80.4,69.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=86.2,13.2,4.8,9.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=98.8,78.4,86.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=61.2,18.8,1.3,14.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=91.8,80.4,69.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=86.2,13.2,4.8,9.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=98.8,78.4,86.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=61.2,18.8,1.3,14.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=91.8,80.4,69.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=86.2,13.2,4.8,9.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=98.8,78.4,86.7,88,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=61.2,18.8,1.3,14.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=78.8,68.4,56.7,48,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=71.2,8.8,11.3,10.8"
sleep 3
curl "http://localhost:9000/server/lab105/system/cpu/ingest?data=89.8,78.4,79.7,78,9"
curl "http://localhost:9000/server/lab105/system/mem/ingest?data=96.2,3.2,4.3,8.8"
sleep 3

rm -rf /var/www/test
curl "http://localhost:9000/finish?path=%2fvar%2fwww%2ftest"
