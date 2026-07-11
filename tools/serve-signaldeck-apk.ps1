param(
    [string]$Root = 'D:\SignalDeck\apk',
    [string]$Prefix = 'http://+:8099/'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Root)) {
    New-Item -ItemType Directory -Force -Path $Root | Out-Null
}

$rootPath = (Resolve-Path -LiteralPath $Root).Path
$listener = [System.Net.HttpListener]::new()
$listener.Prefixes.Add($Prefix)
$listener.Start()

Write-Host "SignalDeck APK server listening on $Prefix"
Write-Host "Root: $rootPath"

function Send-Text {
    param($Response, [int]$StatusCode, [string]$Text, [string]$ContentType = 'text/plain; charset=utf-8', [bool]$IncludeBody = $true)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
    $Response.StatusCode = $StatusCode
    $Response.ContentType = $ContentType
    $Response.ContentLength64 = $bytes.Length
    if ($IncludeBody) {
        $Response.OutputStream.Write($bytes, 0, $bytes.Length)
    }
    $Response.OutputStream.Close()
}

function Get-ContentType {
    param([string]$Path)
    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        '.apk' { 'application/vnd.android.package-archive' }
        '.json' { 'application/json; charset=utf-8' }
        '.txt' { 'text/plain; charset=utf-8' }
        default { 'application/octet-stream' }
    }
}

while ($listener.IsListening) {
    $context = $listener.GetContext()
    $request = $context.Request
    $response = $context.Response

    try {
        if ($request.HttpMethod -ne 'GET' -and $request.HttpMethod -ne 'HEAD') {
            Send-Text $response 405 'Method not allowed'
            continue
        }

        $path = [System.Net.WebUtility]::UrlDecode($request.Url.AbsolutePath).TrimStart('/')
        if ([string]::IsNullOrWhiteSpace($path)) {
            $files = Get-ChildItem -LiteralPath $rootPath -File | Sort-Object LastWriteTime -Descending
            $items = foreach ($file in $files) {
                "<li><a href=""$([System.Net.WebUtility]::HtmlEncode($file.Name))"">$([System.Net.WebUtility]::HtmlEncode($file.Name))</a> <small>$([Math]::Round($file.Length / 1KB, 1)) KB</small></li>"
            }
            Send-Text $response 200 "<!doctype html><html><head><meta name=""viewport"" content=""width=device-width, initial-scale=1""><title>SignalDeck APK</title></head><body><h1>SignalDeck APK</h1><ul>$($items -join "`n")</ul></body></html>" 'text/html; charset=utf-8' ($request.HttpMethod -ne 'HEAD')
            continue
        }

        $candidate = [System.IO.Path]::GetFullPath((Join-Path $rootPath $path))
        if (-not $candidate.StartsWith($rootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
            Send-Text $response 403 'Forbidden' 'text/plain; charset=utf-8' ($request.HttpMethod -ne 'HEAD')
            continue
        }

        if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
            Send-Text $response 404 'Not found' 'text/plain; charset=utf-8' ($request.HttpMethod -ne 'HEAD')
            continue
        }

        $bytes = [System.IO.File]::ReadAllBytes($candidate)
        $response.StatusCode = 200
        $response.ContentType = Get-ContentType $candidate
        $response.ContentLength64 = $bytes.Length
        $response.AddHeader('Content-Disposition', 'attachment; filename="' + [System.IO.Path]::GetFileName($candidate) + '"')
        if ($request.HttpMethod -ne 'HEAD') {
            $response.OutputStream.Write($bytes, 0, $bytes.Length)
        }
        $response.OutputStream.Close()
    } catch {
        Send-Text $response 500 $_.Exception.Message
    }
}
