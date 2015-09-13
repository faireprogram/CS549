#!/bin/bash
export JARFILE=${ftp.dir}/${client.name}.jar
export POLICY=${ftp.dir}/client.policy

if [ ! -e $JARFILE ] ; then
	echo "Missing jar file: $JARFILE"
	echo "Please assemble the ftpclient jar file."
	exit
fi

if [ ! -e $POLICY ] ; then
	pushd ${ftp.dir}
	jar xf "$JARFILE" client.policy
	popd
fi

echo "Running client"
echo "java -Djava.security.policy=$POLICY -jar $JARFILE"
java -Djava.security.policy=$POLICY -jar $JARFILE