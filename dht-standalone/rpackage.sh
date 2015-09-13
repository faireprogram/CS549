#!/bin/bash
#author Wen Zhang
uri1="54.152.33.229:172.31.35.204";
uri2="54.152.228.213:172.31.33.118";
uri3="54.164.33.58:172.31.39.151";
uri4="54.152.244.166:172.31.43.150";
uri5="52.5.172.64:172.31.34.222";
uri6="52.7.2.6:172.31.40.73";

externdns=(	"$uri1"
			"$uri2"
			"$uri3"
			"$uri4"
			"$uri5"
			"$uri6" )
		  
for dns in "${externdns[@]}" ; do
	public=${dns%%:*}
    private=${dns#*:}
	mvn package  -P remote,not-mac -Dmaven.test.skip=true -Dserver.external-dns=$public -Dserver.private-dns=$private -Dserver.home=D:/CS549_ROOT
done
		  
		  

#mvn package  -P remote,not-mac -Dmaven.test.skip=true -Dserver.external-dns=54.14.32.63 -Dserver.home=D:/CS549_ROOT