param(
    [string]$HostAddress = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$Port = 8002,
    [ValidateSet("openai", "google", "anthropic", "mock")]
    [string]$Provider = "openai",
    [string]$Model = "",
    [string]$AllowedDirectories = "",
    [string]$OutputDirectory = "",
    [ValidateRange(30, 7200)]
    [int]$GenerationTimeoutSeconds = 900,
    [switch]$Offline,
    [switch]$Restart,
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

if ($Restart) {
    Write-Host "Stopping PPT MCP server processes using port $Port..."
    $PptConnections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    $PptProcessIds = $PptConnections |
        Select-Object -ExpandProperty OwningProcess -Unique |
        Where-Object { $_ -and $_ -ne $PID }

    if ($PptProcessIds) {
        foreach ($PptProcessId in $PptProcessIds) {
            $PptProcess = Get-Process -Id $PptProcessId -ErrorAction SilentlyContinue
            if ($PptProcess) {
                Write-Host "Stopping process $($PptProcess.ProcessName) ($PptProcessId)"
                Stop-Process -Id $PptProcessId -Force
            }
        }
        Start-Sleep -Seconds 2
    } else {
        Write-Host "No running PPT MCP server process found on port $Port."
    }
}

$VenvPython = Join-Path $ScriptDir ".venv-ppt/Scripts/python.exe"
if (-not (Test-Path $VenvPython)) {
    Write-Host "Creating PPT Python virtual environment..."
    python -m venv .venv-ppt
}

if (-not $SkipInstall) {
    Write-Host "Installing PPT MCP dependencies..."
    & $VenvPython -m pip install -r requirements-ppt-mcp.txt
}

$InboxDirectory = Join-Path $ScriptDir "inbox"
$ProjectUploadsDirectory = Join-Path (Split-Path -Parent $ScriptDir) "uploads"
$ResolvedOutputDirectory = if ($OutputDirectory) {
    [System.IO.Path]::GetFullPath($OutputDirectory)
} else {
    Join-Path $ScriptDir "output/ppt"
}
$JobDirectory = Join-Path $ScriptDir "data/ppt-jobs"
foreach ($Directory in @($InboxDirectory, $ProjectUploadsDirectory, $ResolvedOutputDirectory, $JobDirectory)) {
    if (-not (Test-Path $Directory)) {
        New-Item -ItemType Directory -Path $Directory -Force | Out-Null
    }
}

$ResolvedAllowedDirectories = if ($AllowedDirectories) {
    $AllowedDirectories
} else {
    $InboxDirectory + [System.IO.Path]::PathSeparator + $ProjectUploadsDirectory
}
$ResolvedProvider = if ($Offline) { "mock" } else { $Provider }

$env:PPT_MCP_TRANSPORT = "streamable-http"
$env:PPT_MCP_HOST = $HostAddress
$env:PPT_MCP_PORT = [string]$Port
$env:PPT_MCP_PROVIDER = $ResolvedProvider
$env:PPT_MCP_MODEL = $Model
$env:PPT_MCP_ALLOWED_DIRS = $ResolvedAllowedDirectories
$env:PPT_MCP_OUTPUT_DIR = $ResolvedOutputDirectory
$env:PPT_MCP_GENERATION_TIMEOUT_SECONDS = [string]$GenerationTimeoutSeconds
if ($Offline) {
    $env:AUTOPPT_OFFLINE = "1"
}

$Action = if ($Restart) { "Restarting" } else { "Starting" }
Write-Host ("{0} PPT MCP server at http://{1}:{2}/ppt" -f $Action, $HostAddress, $Port)
Write-Host "Provider: $ResolvedProvider"
Write-Host "Model: $Model"
Write-Host "Allowed directories: $ResolvedAllowedDirectories"
Write-Host "Output directory: $ResolvedOutputDirectory"

& $VenvPython -m app.ppt.ppt_server
