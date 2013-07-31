#!/bin/bash
# Find Java (taken and modified from http://www.gimlisys.com/articles-detect-java.html)
#
REQUIRED_VERSION=1.7

# Search for JAVA_HOME directory and see if Java version is adequate
REQUIRED_VERSION=$(echo $REQUIRED_VERSION | sed "s/\./0/g")
if [ $JAVA_HOME ]; then
	VERSION=$($JAVA_HOME/bin/java -version 2>&1 | grep "java version" | awk '{ print $3 }' | grep -o "[0-9]\.[0-9]" | sed "s/\./0/g")
	if [ $VERSION ]; then
		if [ $VERSION -ge $REQUIRED_VERSION ]; then
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
				if [ $VERSION -ge $REQUIRED_VERSION ]; then
					JAVA_HOME=$(echo $JAVA_EXE | awk '{ print substr($1, 1, length($1)-9); }')
				fi
			fi
		fi
	done
fi

ORAYEN_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

sed "s/%DaemonHome%/$(echo $ORAYEN_HOME | sed -e 's/\//\\\//g')/g;s/%JavaHome%/$(echo $JAVA_HOME | sed -e 's/\//\\\//g')/g"\
 $ORAYEN_HOME/init.template > /etc/init.d/orayen

#TODO create orayen user