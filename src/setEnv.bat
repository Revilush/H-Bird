rem ## change below line to specify your folder
set webinf=C:\eclipse\workspace\Downloader\WEB-INF


rem ## You typically won't need to change below this line, unless need to add more jars or folders to classpath..
set lib=%webinf%\lib
set classpath=%classpath%;%lib%\commons-lang-2.5.jar;%lib%\mysql-connector-java-5.1.22-bin.jar;%lib%\httpclient-4.2.5.jar;%lib%\httpclient-cache-4.2.5.jar;%lib%\jsoup-1.7.2.jar;%lib%\httpcore-4.2.4.jar;%lib%\commons-logging-1.1.jar;%lib%\commons-net-3.3.jar;%lib%\jwnl-1.3.3.jar;%lib%\opennlp-maxent-3.0.3.jar;%lib%\opennlp-tools-1.5.3.jar;%lib%\opennlp-uima-1.5.3.jar;%lib%\jcommon-1.0.21.jar;%lib%\jfreechart-1.0.17.jar;%lib%\jfreesvg-1.4.jar;%lib%\commons-compress-1.7.jar;
set classpath=%classpath%;%webinf%\..\images
