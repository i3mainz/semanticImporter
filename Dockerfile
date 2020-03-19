FROM tomcat:latest

COPY conf/*.xml /usr/local/tomcat/conf/

COPY xsl/ /usr/local/tomcat/xsl

COPY users.xml /usr/local/tomcat/

RUN apt-get update

RUN apt-get install unattended-upgrades apt-listchanges -y

COPY target/*.war /usr/local/tomcat/webapps/