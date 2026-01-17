# ====== 최신(JSON) 경로 ======
$jsonPath = Join-Path $env:APPDATA 'KR_G153\A1730\KRSDKUserLauncherCache.json'

# ====== 구버전(LevelDB) 경로 ======
$base = [Environment]::GetFolderPath('ApplicationData')
$levelDbDir = Join-Path $base 'KRLauncher\G153\C50004\KRWebViewUserData\EBWebView\Default\Local Storage\leveldb'

function Decode-Xor5([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return $s }
    -join ([char[]]$s | ForEach-Object { [char](([int][char]$_) -bxor 5) })
}

function Remove-ControlChars([string]$s) {
    -join ($s.ToCharArray() | ForEach-Object {
        $cp = [int][char]$_
        if ($cp -ge 32 -and $cp -ne 127) { $_ }
    })
}

function Try-Uuid([string]$s, [switch]$Xor5) {
    $t = Remove-ControlChars $s
    if ($Xor5) {
        $t = -join ($t.ToCharArray() | ForEach-Object { [char](([byte][char]$_) -bxor 5) })
    }
    $hexOnly = -join ($t.ToCharArray() | Where-Object { $_ -match '[0-9a-fA-F-]' })
    $m = [regex]::Match($hexOnly, '[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}')
    if ($m.Success) { $m.Value } else { $null }
}

function Get-OauthFromJson([string]$path) {
    if (-not (Test-Path $path)) { return @() }

    try {
        $root = Get-Content -Raw -Path $path | ConvertFrom-Json -ErrorAction Stop
    } catch {
        return @()
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

    $out = foreach ($it in $items) {
        $name = if ([string]::IsNullOrWhiteSpace($it.email)) { $it.thirdNickName } else { $it.email }

        [pscustomobject]@{
            User      = $name
            OauthCode = (Decode-Xor5 $it.oauthCode)
        }
    }
    @($out)
}

function Get-OauthFromLevelDb([string]$dir) {
    if (-not (Test-Path $dir)) { return @() }

    $encLatin1 = [Text.Encoding]::GetEncoding(28591)
    $encU16    = [Text.Encoding]::Unicode
    $re = [regex]::new('(?is)"oauthCode"\s*:\s*"([^"]{36,512})"', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

    $set = New-Object System.Collections.Generic.HashSet[string]

    Get-ChildItem -Path $dir -File |
      Where-Object { $_.Extension -match '^\.(ldb|log)$' } |
      ForEach-Object {
        try {
            $bytes = [IO.File]::ReadAllBytes($_.FullName)
        } catch {
            return
        }

        $t1 = Remove-ControlChars ($encLatin1.GetString($bytes))
        $t2 = Remove-ControlChars ($encU16.GetString($bytes))

        foreach ($text in @($t1, $t2)) {
            if ([string]::IsNullOrWhiteSpace($text)) { continue }

            foreach ($m in $re.Matches($text)) {
                $raw = $m.Groups[1].Value

                $uuid = Try-Uuid $raw -Xor5
                if (-not $uuid) { $uuid = Try-Uuid $raw }
                if ($uuid) { $null = $set.Add($uuid) }
            }
        }
      }
    $out = foreach ($v in ($set | Sort-Object)) {
        [pscustomobject]@{
            User      = 'Unknown'
            OauthCode = $v
        }
    }

    @($out)
}

# ====== 실행: 최신 → 실패/빈값이면 폴백 ======
$newResults = Get-OauthFromJson $jsonPath
$validNew   = @($newResults | Where-Object { -not [string]::IsNullOrWhiteSpace($_.OauthCode) })

if ($validNew.Count -gt 0) {
    $newResults | Format-Table -AutoSize
} else {
    $oldResults = Get-OauthFromLevelDb $levelDbDir
    $validOld   = @($oldResults | Where-Object { -not [string]::IsNullOrWhiteSpace($_.OauthCode) })

    if ($validOld.Count -gt 0) {
        $oldResults | Format-Table -AutoSize
    } else {
        Write-Error "OauthCode not found. (JSON: $jsonPath / LevelDB: $levelDbDir)"
    }
}
