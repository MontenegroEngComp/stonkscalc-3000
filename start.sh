#!/usr/bin/env sh
# Compila tudo e sobe a aplicação completa em http://localhost:8080
# Uso:  ./start.sh
set -e
cd "$(dirname "$0")"

echo ">> Frontend (React)..."
cd frontend
[ -d node_modules ] || npm install --no-fund --no-audit
npm run build
cd ..

echo ">> Backend (Java)..."
sh backend/build.sh

echo ">> Abrindo http://localhost:8080 ..."
java -jar backend/target/stonkscalc.jar
