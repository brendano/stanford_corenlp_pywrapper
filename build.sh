#!/bin/zsh
set -eux
rm -rf _build lib/piperunner.jar
mkdir _build

javac -source 7 -target 7 -d _build -cp "$(print lib/*.jar | tr ' ' ':')":../stanford-corenlp-3.3.1.jar javasrc/**/*.java
(cd _build && jar cf ../lib/piperunner.jar .)
ls -l lib/piperunner.jar
