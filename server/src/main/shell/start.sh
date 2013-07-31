#!/bin/bash
ORAYEN_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAR="Orayen.jar"
CLASSPATH=$(echo $ORAYEN_HOME/lib/*.jar | tr ' ' ':'):$ORAYEN_HOME/$JAR
REQUIRED_VERSION=1.7

# Check for Java
MIN_VERSION=$(echo $REQUIRED_VERSION | sed "s/\./0/g")
if [ $JAVA_HOME ]; then
	VERSION=$($JAVA_HOME/bin/java -version 2>&1 | grep "java version" | awk '{ print $3 }' | grep -o "[0-9]\.[0-9]" | sed "s/\./0/g")
	if [ $VERSION ]; then
		if [ $VERSION -ge $MIN_VERSION ]; then
			JAVA_HOME=$(echo $JAVA_EXE | awk '{ print substr($1, 1, length($1)-9); }')
		else
			JAVA_HOME=
		fi
	else
		JAVA_HOME=
	fi
fi

if [ ! $JAVA_HOME ]; then
	for JAVA_EXE in `locate bin/java | grep java$ | xargs echo`
	do
		if [ ! $JAVA_HOME ]; then
			VERSION=$($JAVA_EXE -version 2>&1 | grep "java version" | awk '{ print $3 }' | grep -o "[0-9]\.[0-9]" | sed "s/\./0/g")
			if [ $VERSION ]; then
				if [ $VERSION -ge $MIN_VERSION ]; then
					JAVA_HOME=$(echo $JAVA_EXE | awk '{ print substr($1, 1, length($1)-9); }')
				fi
			fi
		fi
	done
fi

if [ ! $JAVA_HOME ]; then
	echo "Unable to find suitable java binary with version $REQUIRED_VERSION or higher"
	exit 1
fi

cd $ORAYEN_HOME
$JAVA_HOME/bin/java -cp $CLASSPATH\
 -server -Xms1000m -Xmx1000m\
 -Dlogging.dir=$ORAYEN_HOME/logs/\
 -Dorayen_admin_root=file://$ORAYEN_HOME/web/\
 net.pyxzl.orayen.Main