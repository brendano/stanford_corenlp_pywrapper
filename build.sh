#!/bin/zsh
set -eux
cd $(dirname $0)/stanford_corenlp_pywrapper

rm -rf _build lib/piperunner.jar
mkdir _build

CORENLP_JAR=$HOME/stanford-corenlp/stanford-corenlp-3.4.jar

javac -source 7 -target 7 -d _build -cp "$(print lib/*.jar | tr ' ' ':')":$CORENLP_JAR javasrc/**/*.java
(cd _build && jar cf ../lib/piperunner.jar .)
ls -l lib/piperunner.jar

rm -rf _build
