# Digital Resource Manager - Build and Run Script for Windows PowerShell
$ErrorActionPreference = "Stop"

# Change directory to the project root where this script resides
Set-Location $PSScriptRoot

# Ensure bin directory exists
if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Compiling Digital Resource Manager from Main.java..." -ForegroundColor Cyan
javac -cp "lib/*" -sourcepath src/main/java -d bin src/main/java/Main.java

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed."
    exit $LASTEXITCODE
}

Write-Host "Compilation successful. Launching application..." -ForegroundColor Green
java -cp "bin;lib/*" Main $args
