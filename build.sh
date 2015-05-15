#!/bin/zsh
set -eux
cd $(dirname $0)/stanford_corenlp_pywrapper

jarfile=lib/corenlpwrapper.jar
rm -rf _build $jarfile
mkdir _build

CORENLP_JAR=/home/sw/corenlp/stanford-corenlp-full-2015-04-20/stanford-corenlp-3.5.2.jar

javac -source 7 -target 7 -d _build -cp "$(print lib/*.jar | tr ' ' ':')":$CORENLP_JAR javasrc/**/*.java
(cd _build && jar cf ../$jarfile .)
ls -l $jarfile

rm -rf _build
