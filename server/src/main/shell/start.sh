#!/bin/bash
ORAYEN_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR="Orayen.jar"
CLASSPATH=$(echo $ORAYEN_HOME/lib/*.jar | tr ' ' ':'):$ORAYEN_HOME/$JAR
java -cp $CLASSPATH\
 -server -Xms1000m -Xmx1000m\
 -Dlogging.dir=$ORAYEN_HOME/logs\
 -Dorayen_admin_root=file://$ORAYEN_HOME/web/\
 net.pyxzl.orayen.Main