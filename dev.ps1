# Короткие команды для локальной разработки.
# Примеры:
#   .\dev.ps1 postgres up
#   .\dev.ps1 worker start
#   .\dev.ps1 bot stop
#   .\dev.ps1 verify

param(
    [Parameter(Position = 0)]
    [ValidateSet('postgres', 'worker', 'bot', 'verify', 'test', 'help')]
    [string] $Component = 'help',

    [Parameter(Position = 1)]
    [ValidateSet('up', 'down', 'start', 'stop', 'status', 'restart')]
    [string] $Action
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$RunDir = Join-Path $Root '.run'

function Write-Help {
    @"
Badminton LAB — локальные команды

  .\dev.ps1 postgres up|down|status
  .\dev.ps1 worker  start|stop|status|restart
  .\dev.ps1 bot     start|stop|status|restart
  .\dev.ps1 verify
  .\dev.ps1 test

Читает .env из корня репозитория (если есть).
"@ | Write-Host
}

function Ensure-RunDir {
    if (-not (Test-Path $RunDir)) {
        New-Item -ItemType Directory -Path $RunDir | Out-Null
    }
}

function Import-DotEnv {
    $envFile = Join-Path $Root '.env'
    if (-not (Test-Path $envFile)) {
        return
    }
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*?)\s*$') {
            Set-Item -Path "env:$($matches[1])" -Value $matches[2]
        }
    }
}

function Set-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        return
    }
    $candidates = @(
        'C:\Program Files\Eclipse Adoptium\jdk-18.0.2.101-hotspot',
        'C:\Program Files\Eclipse Adoptium\jdk-17*',
        'C:\Program Files\Microsoft\jdk-17*',
        'C:\Program Files\Java\jdk-17*'
    )
    foreach ($pattern in $candidates) {
        $match = Get-Item -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($match) {
            $env:JAVA_HOME = $match.FullName
            return
        }
    }
}

function Get-PidFile([string] $Name) {
    Join-Path $RunDir "$Name.pid"
}

function Test-Running([string] $Name) {
    $pidFile = Get-PidFile $Name
    if (-not (Test-Path $pidFile)) {
        return $false
    }
    $processId = [int](Get-Content $pidFile -Raw)
    return $null -ne (Get-Process -Id $processId -ErrorAction SilentlyContinue)
}

function Stop-OrphanBotApplications {
    $orphans = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*ru.badmintonlab.bot.BotApplication*' }
    foreach ($proc in $orphans) {
        taskkill /PID $proc.ProcessId /T /F | Out-Null
        Write-Host "Остановлен BotApplication (PID $($proc.ProcessId))."
    }
}

function Stop-Component([string] $Name) {
    $pidFile = Get-PidFile $Name
    if (-not (Test-Path $pidFile)) {
        if ($Name -eq 'bot') {
            Stop-OrphanBotApplications
        }
        Write-Host "$Name не запущен."
        return
    }
    $processId = [int](Get-Content $pidFile -Raw)
    if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
        taskkill /PID $processId /T /F | Out-Null
        Write-Host "$Name остановлен (PID $processId)."
    } else {
        Write-Host "$Name уже не работает."
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    if ($Name -eq 'bot') {
        Stop-OrphanBotApplications
    }
}

function Start-Component([string] $Name, [string] $Module) {
    if ($Name -eq 'bot') {
        Stop-OrphanBotApplications
    }
    if (Test-Running $Name) {
        Write-Host "$Name уже запущен."
        return
    }

    Ensure-RunDir
    Set-JavaHome
    Import-DotEnv

    $mvnw = Join-Path $Root 'mvnw.cmd'
    $logFile = Join-Path $RunDir "$Name.log"
    $errLogFile = Join-Path $RunDir "$Name.err.log"

    # Из корня реактора: модуль + зависимости (-am), с тестами — иначе bot/worker
    # подхватят устаревший core из ~/.m2 (NoSuchMethodError на LastTournamentView и т.п.).
    Push-Location $Root
    try {
        & $mvnw @('-pl', $Module, '-am', 'install')
        if ($LASTEXITCODE -ne 0) {
            throw "Сборка модуля $Module (с зависимостями) не удалась."
        }
    } finally {
        Pop-Location
    }

    $proc = Start-Process `
        -FilePath $mvnw `
        -ArgumentList @('spring-boot:run') `
        -WorkingDirectory (Join-Path $Root $Module) `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError $errLogFile `
        -PassThru `
        -WindowStyle Hidden

    $proc.Id | Set-Content (Get-PidFile $Name)
    Write-Host "$Name запущен (PID $($proc.Id)). Лог: $logFile"
}

function Show-Status([string] $Name) {
    if (Test-Running $Name) {
        $processId = [int](Get-Content (Get-PidFile $Name) -Raw)
        Write-Host "${Name}: работает (PID $processId)"
    } else {
        Write-Host "${Name}: не запущен"
    }
}

function Invoke-Postgres([string] $Action) {
    Push-Location $Root
    try {
        switch ($Action) {
            'up' {
                docker compose up -d postgres
                Write-Host 'PostgreSQL поднят (порт из .env / docker-compose, по умолчанию 5433).'
            }
            'down' {
                docker compose down
                Write-Host 'PostgreSQL остановлен.'
            }
            'status' {
                docker compose ps postgres
            }
            default { throw "Для postgres доступны: up, down, status" }
        }
    } finally {
        Pop-Location
    }
}

function Invoke-Maven([string] $Goal) {
    Set-JavaHome
    Push-Location $Root
    try {
        switch ($Goal) {
            'verify' { & .\mvnw.cmd clean verify }
            'test' { & .\mvnw.cmd -pl worker test }
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Maven завершился с кодом $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

switch ($Component) {
    'help' { Write-Help }
    'verify' { Invoke-Maven 'verify' }
    'test' { Invoke-Maven 'test' }
    'postgres' {
        if (-not $Action) { throw 'Укажите действие: up, down, status' }
        Invoke-Postgres $Action
    }
    'worker' {
        if (-not $Action) { throw 'Укажите действие: start, stop, status, restart' }
        switch ($Action) {
            'start' { Start-Component 'worker' 'worker' }
            'stop' { Stop-Component 'worker' }
            'status' { Show-Status 'worker' }
            'restart' { Stop-Component 'worker'; Start-Component 'worker' 'worker' }
        }
    }
    'bot' {
        if (-not $Action) { throw 'Укажите действие: start, stop, status, restart' }
        switch ($Action) {
            'start' { Start-Component 'bot' 'bot' }
            'stop' { Stop-Component 'bot' }
            'status' { Show-Status 'bot' }
            'restart' { Stop-Component 'bot'; Start-Component 'bot' 'bot' }
        }
    }
}
