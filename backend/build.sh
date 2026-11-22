#!/usr/bin/env sh
# Compila, testa e empacota o backend usando só o JDK (não precisa de Maven).
# Uso:  ./build.sh        -> compila + testa + gera target/stonkscalc.jar
#       ./build.sh run    -> idem e já sobe o servidor
set -e
cd "$(dirname "$0")"

rm -rf target
javac --release 21 -encoding UTF-8 -d target/classes $(find src/main/java -name '*.java')
javac --release 21 -encoding UTF-8 -cp target/classes -d target/test-classes $(find src/test/java -name '*.java')
java -cp "target/classes:target/test-classes" com.stonkscalc.EngineTest
jar --create --file target/stonkscalc.jar --main-class com.stonkscalc.Main -C target/classes .
echo "Jar gerado em backend/target/stonkscalc.jar"

if [ "$1" = "run" ]; then
  java -jar target/stonkscalc.jar
fi
