# Compila, testa e empacota o backend usando só o JDK (não precisa de Maven).
# Uso:  .\build.ps1          -> compila + testa + gera target\stonkscalc.jar
#       .\build.ps1 -Run     -> idem e já sobe o servidor
param([switch]$Run)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {

if (Test-Path target) { Remove-Item -Recurse -Force target }

$main = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -d target/classes $main
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar o backend' }

$tests = Get-ChildItem -Recurse src/test/java -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -cp target/classes -d target/test-classes $tests
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compilar os testes' }

java -cp "target/classes;target/test-classes" com.stonkscalc.EngineTest
if ($LASTEXITCODE -ne 0) { throw 'Testes falharam' }

jar --create --file target/stonkscalc.jar --main-class com.stonkscalc.Main -C target/classes .
Write-Host "`nJar gerado em backend\target\stonkscalc.jar"

if ($Run) { java -jar target/stonkscalc.jar }

} finally {
    Pop-Location  # devolve o terminal para a pasta onde ele estava
}
