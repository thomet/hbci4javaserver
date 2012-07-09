hbci4javaserver
===============

Requements
----------
- ant
- tomcat

Deploy
------
Go to <root-dir> and run

> ant dist

Start Server
------------
Go to <root-dir>/dist/ and run

> java -cp ../lib/hbci4java.jar:deploy/WEB-INF/lib/hbci4java-server.jar:demo/deploy/WEB-INF/lib/hbci4java-server-demo.jar org.kapott.demo.hbci.server.TestServer demo/server-data
