#!/bin/sh
mkdir -p out
javac -encoding UTF-8 -d out -cp "lib/*" $(find src/main/java -name "*.java") || exit 1
cp src/main/resources/schema.sql out/
java -cp "out:lib/*" com.rbac.Main
