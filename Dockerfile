FROM tomcat:latest

COPY conf/*.xml /usr/local/tomcat/conf/

COPY xsl/ /usr/local/tomcat/xsl

COPY users.xml /usr/local/tomcat/

COPY importerconfig.json /usr/local/tomcat

COPY config /usr/local/tomcat/config

RUN apt-get update

RUN apt-get install unattended-upgrades apt-listchanges -y

COPY target/*.war /usr/local/tomcat/webapps/