#!/bin/bash
ORAYEN_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR="Orayen.jar"
CLASSPATH=$(echo $ORAYEN_HOME/lib/*.jar | tr ' ' ':'):$ORAYEN_HOME/$JAR
REQUIRED_VERSION=1.7

// Check for Java
if [ $JAVA_HOME ]
then
	JAVA_EXE=$JAVA_HOME/bin/java
	$JAVA_EXE -version 2> tmp.ver
	VERSION=`cat tmp.ver | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
	rm tmp.ver
	VERSION=`echo $VERSION | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
	if [ $VERSION ]
	then
		if [ $VERSION -ge $REQUIRED_VERSION ]
		then
			JAVA_HOME=`echo $JAVA_EXE | awk '{ print substr($1, 1, length($1)-9); }'`
		else
			JAVA_HOME=
		fi
	else
		JAVA_HOME=
	fi
fi

if [ $JAVA_HOME ]
then
	:
else
	for JAVA_EXE in `locate bin/java | grep java$ | xargs echo`
	do
		if [ $JAVA_HOME ] 
		then
			:
		else
			$JAVA_EXE -version 2> tmp.ver 1> /dev/null
			VERSION=`cat tmp.ver | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
			rm tmp.ver
			VERSION=`echo $VERSION | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
			if [ $VERSION ]
			then
				if [ $VERSION -ge $REQUIRED_VERSION ]
				then
					JAVA_HOME=`echo $JAVA_EXE | awk '{ print substr($1, 1, length($1)-9); }'`
				fi
			fi
		fi
	done
fi

cd $ORAYEN_HOME
$JAVA_HOME/bin/java -cp $CLASSPATH\
 -server -Xms1000m -Xmx1000m\
 -Dlogging.dir=$ORAYEN_HOME/logs/\
 -Dorayen_admin_root=file://$ORAYEN_HOME/web/\
 net.pyxzl.orayen.Main