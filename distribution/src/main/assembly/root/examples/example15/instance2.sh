#!/bin/sh

# Change this to your JDK installation root
#
#JAVA_HOME=/usr/java/jdk1.6.0_18

if [ "$JAVA_HOME" == "" ]; then
  echo "Please set JAVA_HOME"
  exit 1
fi

workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`
QUARTZ=${workdir}/../..

. ${QUARTZ}/examples/bin/buildcp.sh

#
# Set the path to your Terracotta server home here
INSTALL_DIR=${workdir}/../../..

if [ ! -f $INSTALL_DIR/server/bin/start-tc-server.sh ]; then
  echo "Modify the script to set INSTALL_DIR" 
  exit -1
fi

for jarfile in $INSTALL_DIR/apis/toolkit/lib/terracotta-toolkit*.jar; do
  TC_CP=$TC_CP:$jarfile
done

QUARTZ_CP=$QUARTZ_CP:$TC_CP

LOGGING_PROPS="-Dlog4j.configuration=file:${workdir}/log4j.xml"

# Set the name and location of the quartz.properties file
QUARTZ_PROPS="-Dorg.quartz.properties=${workdir}/instance2.properties"

$JAVA_HOME/bin/java -classpath $QUARTZ_CP $QUARTZ_PROPS $LOGGING_PROPS -Dtc.install-root=$INSTALL_DIR org.quartz.examples.example15.ClusterExample dontScheduleJobs

