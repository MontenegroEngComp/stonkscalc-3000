# Compila tudo e sobe a aplicação completa em http://localhost:8080
# Uso:  .\start.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host '>> Frontend (React)...' -ForegroundColor Yellow
Push-Location frontend
if (-not (Test-Path node_modules)) { npm install --no-fund --no-audit }
npm run build
if ($LASTEXITCODE -ne 0) { throw 'Falha no build do frontend' }
Pop-Location

Write-Host '>> Backend (Java)...' -ForegroundColor Yellow
& .\backend\build.ps1

Write-Host '>> Abrindo http://localhost:8080 ...' -ForegroundColor Green
java -jar backend/target/stonkscalc.jar
