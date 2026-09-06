param(
    [switch] $Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $scriptDir "updater-config.json"
$settingsPath = Join-Path $scriptDir "local-settings.json"
$logPath = Join-Path $scriptDir "updater.log"

function Write-Log {
    param([string] $Message)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Write-Host $Message
    Add-Content -Path $logPath -Value $line
}

function Show-Message {
    param(
        [string] $Message,
        [string] $Title = "Harmfy Updater"
    )
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    [System.Windows.Forms.MessageBox]::Show($Message, $Title, "OK", "Information") | Out-Null
}

function Confirm-Message {
    param(
        [string] $Message,
        [string] $Title = "Harmfy Updater"
    )
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    $result = [System.Windows.Forms.MessageBox]::Show($Message, $Title, "OKCancel", "Warning")
    return $result -eq [System.Windows.Forms.DialogResult]::OK
}

function Read-JsonFile {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "No existe el archivo: $Path"
    }
    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
}

function Save-JsonFile {
    param(
        [string] $Path,
        [object] $Value
    )
    $Value | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Test-PackFolder {
    param([string] $Path)
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Container)) {
        return $false
    }
    return (Test-Path -LiteralPath (Join-Path $Path "mods") -PathType Container) -or
           (Test-Path -LiteralPath (Join-Path $Path "config") -PathType Container) -or
           (Test-Path -LiteralPath (Join-Path $Path "resourcepacks") -PathType Container)
}

function Get-CandidateFolders {
    param([string] $ProfileFolderName)

    $paths = New-Object System.Collections.Generic.List[string]
    $appData = [Environment]::GetFolderPath("ApplicationData")
    $localAppData = [Environment]::GetFolderPath("LocalApplicationData")
    $userProfile = [Environment]::GetFolderPath("UserProfile")

    $directCandidates = @(
        (Join-Path $appData "ModrinthApp\profiles\$ProfileFolderName"),
        (Join-Path $appData ".minecraft"),
        (Join-Path $appData "SKLauncher\instances\$ProfileFolderName"),
        (Join-Path $appData ".sklauncher\instances\$ProfileFolderName"),
        (Join-Path $appData "TLauncher\.minecraft"),
        (Join-Path $appData ".tlauncher\legacy\Minecraft\game"),
        (Join-Path $appData "PrismLauncher\instances\$ProfileFolderName\.minecraft"),
        (Join-Path $appData "PolyMC\instances\$ProfileFolderName\.minecraft"),
        (Join-Path $localAppData "Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Roaming\.minecraft")
    )

    foreach ($path in $directCandidates) {
        if (Test-PackFolder $path) {
            $paths.Add((Resolve-Path -LiteralPath $path).Path)
        }
    }

    $searchRoots = @(
        (Join-Path $appData "ModrinthApp\profiles"),
        (Join-Path $appData "SKLauncher\instances"),
        (Join-Path $appData ".sklauncher\instances"),
        (Join-Path $appData "PrismLauncher\instances"),
        (Join-Path $appData "PolyMC\instances")
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path -LiteralPath $root -PathType Container)) {
            continue
        }
        Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $candidate = $_.FullName
            $minecraftChild = Join-Path $candidate ".minecraft"
            if ($_.Name -like "*$ProfileFolderName*" -and (Test-PackFolder $candidate)) {
                $paths.Add((Resolve-Path -LiteralPath $candidate).Path)
            }
            if ($_.Name -like "*$ProfileFolderName*" -and (Test-PackFolder $minecraftChild)) {
                $paths.Add((Resolve-Path -LiteralPath $minecraftChild).Path)
            }
        }
    }

    return $paths | Select-Object -Unique
}

function Select-PackFolder {
    param([string] $Description)

    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = $Description
    $dialog.ShowNewFolderButton = $true
    $result = $dialog.ShowDialog()
    if ($result -ne [System.Windows.Forms.DialogResult]::OK) {
        throw "No se selecciono carpeta del modpack."
    }
    return $dialog.SelectedPath
}

function Get-TargetFolder {
    param($Config)

    if (Test-Path -LiteralPath $settingsPath) {
        $settings = Read-JsonFile $settingsPath
        if ($settings.PSObject.Properties.Name -contains "target_path" -and (Test-PackFolder $settings.target_path)) {
            Write-Log "Usando carpeta guardada: $($settings.target_path)"
            return $settings.target_path
        }
    }

    $candidates = @(Get-CandidateFolders -ProfileFolderName $Config.profile_folder_name)
    if ($candidates.Count -eq 1) {
        $target = $candidates[0]
        Write-Log "Carpeta detectada automaticamente: $target"
        Save-JsonFile -Path $settingsPath -Value ([pscustomobject]@{ target_path = $target })
        return $target
    }

    if ($candidates.Count -gt 1) {
        Write-Log "Se encontraron varias carpetas posibles:"
        $i = 1
        foreach ($candidate in $candidates) {
            Write-Log "  [$i] $candidate"
            $i++
        }
    }

    $selected = Select-PackFolder -Description "Selecciona la carpeta del perfil/modpack $($Config.pack_name). Debe ser la carpeta que contiene mods y config."
    if (-not (Test-PackFolder $selected)) {
        $ok = Confirm-Message "La carpeta seleccionada no parece tener mods/config/resourcepacks. Si es un perfil nuevo, se crearan carpetas. ¿Continuar?`n`n$selected"
        if (-not $ok) {
            throw "Carpeta cancelada por el usuario."
        }
    }
    Save-JsonFile -Path $settingsPath -Value ([pscustomobject]@{ target_path = $selected })
    return $selected
}

function Get-GoogleDriveFileId {
    param([string] $Url)
    if ($Url -match "/file/d/([^/]+)") {
        return $Matches[1]
    }
    if ($Url -match "[?&]id=([^&]+)") {
        return $Matches[1]
    }
    return $null
}

function Test-ZipFile {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        if ($stream.Length -lt 4) {
            return $false
        }
        $buffer = New-Object byte[] 4
        [void] $stream.Read($buffer, 0, 4)
        return $buffer[0] -eq 0x50 -and $buffer[1] -eq 0x4B
    } finally {
        $stream.Dispose()
    }
}

function Join-Query {
    param([hashtable] $Params)
    return ($Params.GetEnumerator() | ForEach-Object {
        "{0}={1}" -f [Uri]::EscapeDataString($_.Key), [Uri]::EscapeDataString([string]$_.Value)
    }) -join "&"
}

function Download-GoogleDriveFile {
    param(
        [string] $FileId,
        [string] $OutputPath
    )

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $firstUrl = "https://drive.google.com/uc?export=download&id=$FileId&confirm=t"
    $tempResponse = "$OutputPath.download"
    Invoke-WebRequest -Uri $firstUrl -WebSession $session -UseBasicParsing -OutFile $tempResponse

    if (Test-ZipFile $tempResponse) {
        Move-Item -LiteralPath $tempResponse -Destination $OutputPath -Force
        return
    }

    $html = Get-Content -LiteralPath $tempResponse -Raw
    $action = $null
    if ($html -match '<form[^>]+id="download-form"[^>]+action="([^"]+)"') {
        $action = $Matches[1].Replace("&amp;", "&")
    }
    if ($null -eq $action) {
        throw "Google Drive no entrego un zip descargable. Revisa que el enlace sea publico o usa un enlace de descarga directa."
    }

    $params = @{}
    foreach ($match in [regex]::Matches($html, '<input[^>]+type="hidden"[^>]+name="([^"]+)"[^>]+value="([^"]*)"')) {
        $params[$match.Groups[1].Value] = $match.Groups[2].Value.Replace("&amp;", "&")
    }
    if (-not $params.ContainsKey("id")) {
        $params["id"] = $FileId
    }
    $downloadUrl = $action + "?" + (Join-Query -Params $params)
    Invoke-WebRequest -Uri $downloadUrl -WebSession $session -UseBasicParsing -OutFile $OutputPath

    if (-not (Test-ZipFile $OutputPath)) {
        throw "La descarga termino, pero no parece ser un zip valido."
    }
}

function Download-Pack {
    param(
        [string] $Url,
        [string] $OutputPath
    )

    if ([string]::IsNullOrWhiteSpace($Url) -or $Url -like "PEGA_AQUI*") {
        throw "Falta configurar download_url en updater-config.json."
    }

    $driveId = Get-GoogleDriveFileId -Url $Url
    if ($driveId) {
        Write-Log "Descargando pack desde Google Drive..."
        Download-GoogleDriveFile -FileId $driveId -OutputPath $OutputPath
    } else {
        Write-Log "Descargando pack desde enlace directo..."
        Invoke-WebRequest -Uri $Url -UseBasicParsing -OutFile $OutputPath
    }

    if (-not (Test-ZipFile $OutputPath)) {
        throw "El archivo descargado no es un zip valido."
    }
}

function Verify-HashIfConfigured {
    param(
        [string] $Path,
        $Config
    )

    if (-not ($Config.PSObject.Properties.Name -contains "sha256")) {
        return
    }
    $expected = [string] $Config.sha256
    if ([string]::IsNullOrWhiteSpace($expected)) {
        return
    }

    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected.ToLowerInvariant()) {
        throw "El SHA256 del zip no coincide. Esperado: $expected Actual: $actual"
    }
    Write-Log "SHA256 verificado."
}

function Get-PayloadRoot {
    param(
        [string] $ExtractPath,
        [string[]] $Folders
    )

    foreach ($folder in $Folders) {
        if (Test-Path -LiteralPath (Join-Path $ExtractPath $folder) -PathType Container) {
            return $ExtractPath
        }
    }

    $children = @(Get-ChildItem -LiteralPath $ExtractPath -Directory)
    if ($children.Count -eq 1) {
        foreach ($folder in $Folders) {
            if (Test-Path -LiteralPath (Join-Path $children[0].FullName $folder) -PathType Container) {
                return $children[0].FullName
            }
        }
    }

    throw "El zip no tiene una estructura reconocible. Debe contener carpetas como mods/config/datapacks."
}

function Wait-ForMinecraftClosed {
    $processNames = @("java", "javaw", "Minecraft", "MinecraftLauncher", "TLauncher", "SKLauncher", "Modrinth App")
    $running = @(Get-Process -ErrorAction SilentlyContinue | Where-Object { $processNames -contains $_.ProcessName })
    if ($running.Count -eq 0) {
        return
    }

    $ok = Confirm-Message "Parece que Minecraft o un launcher esta abierto. Cierra el juego antes de actualizar y presiona OK.`n`nSi continuas con el juego abierto, algunos archivos pueden no reemplazarse."
    if (-not $ok) {
        throw "Actualizacion cancelada."
    }
}

function Backup-And-ReplaceFolder {
    param(
        [string] $SourceRoot,
        [string] $TargetRoot,
        [string] $FolderName,
        [string] $BackupRoot,
        [bool] $DoBackup
    )

    $source = Join-Path $SourceRoot $FolderName
    if (-not (Test-Path -LiteralPath $source -PathType Container)) {
        Write-Log "No viene '$FolderName' en el zip; se omite."
        return
    }

    $target = Join-Path $TargetRoot $FolderName
    if (Test-Path -LiteralPath $target -PathType Container) {
        if ($DoBackup) {
            $backupTarget = Join-Path $BackupRoot $FolderName
            New-Item -ItemType Directory -Path (Split-Path -Parent $backupTarget) -Force | Out-Null
            Copy-Item -LiteralPath $target -Destination $backupTarget -Recurse -Force
            Write-Log "Backup creado: $FolderName"
        }
        Remove-Item -LiteralPath $target -Recurse -Force
    }

    Copy-Item -LiteralPath $source -Destination $target -Recurse -Force
    Write-Log "Actualizado: $FolderName"
}

try {
    Write-Log "=== Iniciando Harmfy Updater ==="
    $config = Read-JsonFile -Path $configPath
    $target = Get-TargetFolder -Config $config

    $versionFile = Join-Path $target ".harmfy-version"
    if (-not $Force -and $config.skip_if_same_version -and (Test-Path -LiteralPath $versionFile)) {
        $currentVersion = (Get-Content -LiteralPath $versionFile -Raw).Trim()
        if ($currentVersion -eq $config.pack_version) {
            Show-Message "Ya tienes Harmfy $currentVersion instalado.`n`nNo se hizo ningun cambio."
            exit 0
        }
    }

    Wait-ForMinecraftClosed

    $workRoot = Join-Path $env:TEMP ("HarmfyUpdater-" + [guid]::NewGuid().ToString("N"))
    $zipPath = Join-Path $workRoot "pack.zip"
    $extractPath = Join-Path $workRoot "extract"
    New-Item -ItemType Directory -Path $workRoot -Force | Out-Null
    New-Item -ItemType Directory -Path $extractPath -Force | Out-Null

    Download-Pack -Url $config.download_url -OutputPath $zipPath
    Verify-HashIfConfigured -Path $zipPath -Config $config
    Write-Log "Extrayendo zip..."
    Expand-Archive -LiteralPath $zipPath -DestinationPath $extractPath -Force

    $replaceFolders = @($config.replace_folders)
    $optionalFolders = @()
    if ($config.PSObject.Properties.Name -contains "also_replace_if_present") {
        $optionalFolders = @($config.also_replace_if_present)
    }
    $allKnownFolders = @($replaceFolders + $optionalFolders) | Select-Object -Unique
    $payloadRoot = Get-PayloadRoot -ExtractPath $extractPath -Folders $allKnownFolders
    Write-Log "Contenido detectado en: $payloadRoot"

    $backupRoot = Join-Path $target (".harmfy-backups\" + (Get-Date -Format "yyyyMMdd-HHmmss"))
    if ($config.backup_before_update) {
        New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
        Write-Log "Backup en: $backupRoot"
    }

    foreach ($folder in $replaceFolders) {
        Backup-And-ReplaceFolder -SourceRoot $payloadRoot -TargetRoot $target -FolderName $folder -BackupRoot $backupRoot -DoBackup ([bool]$config.backup_before_update)
    }
    foreach ($folder in $optionalFolders) {
        if (Test-Path -LiteralPath (Join-Path $payloadRoot $folder) -PathType Container) {
            Backup-And-ReplaceFolder -SourceRoot $payloadRoot -TargetRoot $target -FolderName $folder -BackupRoot $backupRoot -DoBackup ([bool]$config.backup_before_update)
        }
    }

    Set-Content -LiteralPath $versionFile -Value $config.pack_version -Encoding UTF8
    Write-Log "Version instalada: $($config.pack_version)"
    Show-Message "Harmfy se actualizo correctamente a la version $($config.pack_version).`n`nCarpeta:`n$target"
    exit 0
} catch {
    Write-Log "ERROR: $($_.Exception.Message)"
    Show-Message "No se pudo actualizar Harmfy.`n`n$($_.Exception.Message)`n`nRevisa updater.log junto al script." "Harmfy Updater - Error"
    exit 1
}
