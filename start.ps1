# Compila tudo e sobe a aplicação completa em http://localhost:8080
# Uso:  .\start.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$jar = Join-Path $root 'backend\target\stonkscalc.jar'
$url = 'http://localhost:8080'

$busy = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($busy) {
    throw "A porta 8080 já está em uso (PID $($busy.OwningProcess | Select-Object -First 1)). Feche o outro programa ou o servidor antigo e tente de novo."
}

Write-Host '>> Frontend (React)...' -ForegroundColor Yellow
Push-Location (Join-Path $root 'frontend')
try {
    if (-not (Test-Path node_modules)) { npm install --no-fund --no-audit }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Falha no build do frontend' }
} finally {
    Pop-Location
}

Write-Host '>> Backend (Java)...' -ForegroundColor Yellow
& (Join-Path $root 'backend\build.ps1')

# Abre o navegador assim que o servidor responder
Start-Job -ArgumentList $url -ScriptBlock {
    param($u)
    for ($i = 0; $i -lt 60; $i++) {
        try {
            Invoke-WebRequest "$u/api/health" -UseBasicParsing -TimeoutSec 1 | Out-Null
            Start-Process $u
            break
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
} | Out-Null

Write-Host ">> Subindo em $url (Ctrl+C para parar)..." -ForegroundColor Green
Push-Location $root  # o servidor procura frontend\dist a partir daqui
try {
    java -jar $jar
} finally {
    Pop-Location
}
