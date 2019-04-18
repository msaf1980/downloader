#!/bin/sh

DOWNLOADER=downloader-1.0.0-SNAPSHOT.jar
JSOUP=jsoup-1.9.2.jar

DIR="`dirname $0`"

java -cp ${DIR}/${JSOUP}:${DIR}/${DOWNLOADER} downloader/Downloader $@
