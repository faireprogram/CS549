#!/bin/bash
export JARFILE=C:\\Users\\faire_000/tmp/cs549/ftp-test/ftp.jar
export POLICY=C:\\Users\\faire_000/tmp/cs549/ftp-test/client.policy

if [ ! -e $JARFILE ] ; then
	echo "Missing jar file: $JARFILE"
	echo "Please assemble the ftpclient jar file."
	exit
fi

if [ ! -e $POLICY ] ; then
	pushd C:\\Users\\faire_000/tmp/cs549/ftp-test
	jar xf "$JARFILE" client.policy
	popd
fi

echo "Running client"
echo "java -Djava.security.policy=$POLICY -jar $JARFILE"
java -Djava.security.policy=$POLICY -jar $JARFILE