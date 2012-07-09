hbci4javaserver
===============

Requements
----------
- ant
- tomcat

Quickstart
-----------

###Deploy
Adjust _build.properties_ and run 
> $ ant dist

###Configuration
Copy _dist/demo/server-data-template_ to _dist/demo/server-data_

###Start Server
Go to _dist/_ and run
> $ java -cp ../lib/hbci4java.jar:deploy/WEB-INF/lib/hbci4java-server.jar:demo/deploy/WEB-INF/lib/hbci4java-server-demo.jar org.kapott.demo.hbci.server.TestServer demo/server-data

###Create Admin-Interface
1. Got to _dist/demo/deploy_
2. Edit _WEB-INF/web.xml_ and adjust rmiServer value
3. run:
> $ jar cfv hbci.war .
4. Run war file in tomcat
Please Check [README.DemoServer](README.DemoServer) for More Information

###Create PinTanServlet
1. Got to _dist/deploy_
2. Edit _WEB-INF/web.xml_ and adjust rmiServer value
3. run:
> $ jar cfv pintan.war .
4. Run war file in tomcat
Please Check [README.DemoServer](README.PinTan) for More Information


