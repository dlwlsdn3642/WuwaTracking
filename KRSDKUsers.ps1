$path = Join-Path $env:APPDATA 'KR_G153\A1730\KRSDKUserLauncherCache.json'

if (-not (Test-Path $path)) {
    Write-Error "File not found: $path"
    return
}

function Decode-Xor5([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return $s }
    -join ([char[]]$s | ForEach-Object { [char](([int][char]$_) -bxor 5) })
}

# JSON 로드
try {
    $root = Get-Content -Raw -Path $path | ConvertFrom-Json -ErrorAction Stop
} catch {
    Write-Error "JSON parsing failed: $($_.Exception.Message)"
    return
}

if ($root -is [System.Collections.IEnumerable] -and $root.GetType().Name -ne 'String') {
    $items = $root
} elseif ($root.PSObject.Properties.Name -contains 'list') {
    $items = $root.list
} elseif ($root.PSObject.Properties.Name -contains 'items') {
    $items = $root.items
} elseif ($root.PSObject.Properties.Name -contains 'data') {
    $items = $root.data
} else {
    $items = @($root)
}

$items | ForEach-Object {
    $name = if ([string]::IsNullOrWhiteSpace($_.email)) { $_.thirdNickName } else { $_.email }
    [pscustomobject]@{
        User      = $name
        OauthCode = (Decode-Xor5 $_.oauthCode)
    }
} | Format-Table -AutoSize