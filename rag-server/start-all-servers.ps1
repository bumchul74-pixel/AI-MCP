param(
    [string]$RagHostAddress = "0.0.0.0",
    [string]$McpHostAddress = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$RagPort = 8000,
    [ValidateRange(1, 65535)]
    [int]$EasyOcrPort = 8001,
    [ValidateRange(1, 65535)]
    [int]$PptPort = 8002,
    [ValidateRange(10, 3600)]
    [int]$ReadyTimeoutSeconds = 600,
    [switch]$Restart,
    [switch]$InstallDependencies,
    [switch]$SkipInstall,
    [switch]$RagNoReload,
    [switch]$PptOffline
)

$ErrorActionPreference = "Stop"

if ($InstallDependencies -and $SkipInstall) {
    throw "InstallDependencies and SkipInstall cannot be used together."
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDirectory = Join-Path $ScriptDir "logs/server-start"
$PowerShellExecutable = (Get-Command powershell.exe -ErrorAction Stop).Source

if (-not (Test-Path -LiteralPath $LogDirectory)) {
    New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
}

function Get-PortOwningProcessIds {
    param([int]$Port)

    return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique)
}

function Test-PortListening {
    param([int]$Port)

    return (Get-PortOwningProcessIds -Port $Port).Count -gt 0
}

function Get-LogTail {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return ""
    }

    return (Get-Content -LiteralPath $Path -Tail 30 -ErrorAction SilentlyContinue) -join [Environment]::NewLine
}

function Start-ServiceScript {
    param(
        [string]$Name,
        [string]$ScriptName,
        [int]$Port,
        [string]$Endpoint,
        [string[]]$Arguments
    )

    $ScriptPath = Join-Path $ScriptDir $ScriptName
    if (-not (Test-Path -LiteralPath $ScriptPath)) {
        throw "[$Name] Start script not found: $ScriptPath"
    }

    $InitialOwnerIds = @(Get-PortOwningProcessIds -Port $Port)
    if (($InitialOwnerIds.Count -gt 0) -and -not $Restart) {
        Write-Host "[$Name] ALREADY RUNNING - $Endpoint"
        return [pscustomobject]@{
            Name = $Name
            Status = "ALREADY RUNNING"
            Endpoint = $Endpoint
            LauncherPid = $null
        }
    }

    $Timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
    $SafeName = $Name.ToLowerInvariant() -replace "[^a-z0-9]+", "-"
    $StdoutPath = Join-Path $LogDirectory "$Timestamp-$SafeName.stdout.log"
    $StderrPath = Join-Path $LogDirectory "$Timestamp-$SafeName.stderr.log"

    $ChildArguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", ('"{0}"' -f $ScriptPath)
    ) + $Arguments

    Write-Host "[$Name] STARTING - $Endpoint"
    $Launcher = Start-Process `
        -FilePath $PowerShellExecutable `
        -ArgumentList $ChildArguments `
        -WorkingDirectory $ScriptDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput $StdoutPath `
        -RedirectStandardError $StderrPath `
        -PassThru

    $Deadline = (Get-Date).AddSeconds($ReadyTimeoutSeconds)
    while ((Get-Date) -lt $Deadline) {
        $CurrentOwnerIds = @(Get-PortOwningProcessIds -Port $Port)
        $IsReady = $CurrentOwnerIds.Count -gt 0
        if ($Restart -and $InitialOwnerIds.Count -gt 0) {
            $ReplacementOwnerIds = @($CurrentOwnerIds | Where-Object { $_ -notin $InitialOwnerIds })
            $IsReady = $ReplacementOwnerIds.Count -gt 0
        }

        if ($IsReady) {
            Write-Host "[$Name] STARTED - $Endpoint"
            return [pscustomobject]@{
                Name = $Name
                Status = "STARTED"
                Endpoint = $Endpoint
                LauncherPid = $Launcher.Id
            }
        }

        $Launcher.Refresh()
        if ($Launcher.HasExited) {
            break
        }

        Start-Sleep -Milliseconds 500
    }

    $Stdout = Get-LogTail -Path $StdoutPath
    $Stderr = Get-LogTail -Path $StderrPath
    throw "[$Name] failed to listen on port $Port within $ReadyTimeoutSeconds seconds.`nstdout:`n$Stdout`nstderr:`n$Stderr"
}

$CommonArguments = @()
if ($Restart) {
    $CommonArguments += "-Restart"
}
if (-not $InstallDependencies) {
    $CommonArguments += "-SkipInstall"
}

if ($InstallDependencies) {
    Write-Host "Dependency installation: ENABLED"
} else {
    Write-Host "Dependency installation: SKIPPED (use -InstallDependencies to install)"
}

$RagArguments = @(
    "-HostAddress", $RagHostAddress,
    "-Port", [string]$RagPort
) + $CommonArguments
if ($RagNoReload) {
    $RagArguments += "-NoReload"
}

$EasyOcrArguments = @(
    "-HostAddress", $McpHostAddress,
    "-Port", [string]$EasyOcrPort
) + $CommonArguments

$PptArguments = @(
    "-HostAddress", $McpHostAddress,
    "-Port", [string]$PptPort
) + $CommonArguments
if ($PptOffline) {
    $PptArguments += "-Offline"
}

$Results = @()
$Results += Start-ServiceScript `
    -Name "RAG" `
    -ScriptName "start-rag-server.ps1" `
    -Port $RagPort `
    -Endpoint "http://localhost:$RagPort/" `
    -Arguments $RagArguments
$Results += Start-ServiceScript `
    -Name "EasyOCR MCP" `
    -ScriptName "start-easyocr-server.ps1" `
    -Port $EasyOcrPort `
    -Endpoint "http://${McpHostAddress}:$EasyOcrPort/ocr" `
    -Arguments $EasyOcrArguments
$Results += Start-ServiceScript `
    -Name "PPT MCP" `
    -ScriptName "start-ppt-server.ps1" `
    -Port $PptPort `
    -Endpoint "http://${McpHostAddress}:$PptPort/ppt" `
    -Arguments $PptArguments

Write-Host ""
Write-Host "All Python servers are ready."
$Results | Format-Table Name, Status, Endpoint, LauncherPid -AutoSize
Write-Host "Logs: $LogDirectory"
