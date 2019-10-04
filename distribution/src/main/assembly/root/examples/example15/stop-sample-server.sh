#!/bin/sh
#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#
workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`

# Set the path to your Terracotta server home here
TC_HOME=${workdir}/../../../server

if [ ! -f $TC_HOME/bin/stop-tc-server.sh ]; then
  echo "Modify the script to set TC_HOME" 
  exit -1
fi

exec $TC_HOME/bin/stop-tc-server.sh&
