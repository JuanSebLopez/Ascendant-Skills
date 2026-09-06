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
        [string] $Title = "Harmfy Updater",
        [string] $Icon = "Information"
    )

    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    [System.Windows.Forms.MessageBox]::Show($Message, $Title, "OK", $Icon) | Out-Null
}

function Confirm-Message {
    param(
        [string] $Message,
        [string] $Title = "Harmfy Updater"
    )

    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    $result = [System.Windows.Forms.MessageBox]::Show($Message, $Title, "OKCancel", "Question")
    return $result -eq [System.Windows.Forms.DialogResult]::OK
}

function Get-Prop {
    param(
        [object] $Object,
        [string] $Name,
        [object] $Default = $null
    )

    if ($null -eq $Object) {
        return $Default
    }

    if ($Object.PSObject.Properties.Name -contains $Name) {
        $value = $Object.$Name
        if ($null -ne $value) {
            return $value
        }
    }

    return $Default
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

    $Value | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Test-PackFolder {
    param([string] $Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Container)) {
        return $false
    }

    return (Test-Path -LiteralPath (Join-Path $Path "mods") -PathType Container) -or
           (Test-Path -LiteralPath (Join-Path $Path "config") -PathType Container) -or
           (Test-Path -LiteralPath (Join-Path $Path "defaultconfigs") -PathType Container) -or
           (Test-Path -LiteralPath (Join-Path $Path "datapacks") -PathType Container)
}

function Add-ExistingCandidate {
    param(
        [System.Collections.Generic.List[string]] $List,
        [string] $Path
    )

    if (-not [string]::IsNullOrWhiteSpace($Path) -and (Test-PackFolder $Path) -and -not $List.Contains($Path)) {
        $List.Add($Path) | Out-Null
    }
}

function Add-InstanceCandidates {
    param(
        [System.Collections.Generic.List[string]] $List,
        [string] $Root
    )

    if (-not (Test-Path -LiteralPath $Root -PathType Container)) {
        return
    }

    Get-ChildItem -LiteralPath $Root -Directory -ErrorAction SilentlyContinue | ForEach-Object {
        Add-ExistingCandidate -List $List -Path $_.FullName
        Add-ExistingCandidate -List $List -Path (Join-Path $_.FullName ".minecraft")
    }
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
        (Join-Path $localAppData "Packages\Microsoft.MinecraftUWP_8wekyb3d8bbwe\LocalState\games\com.mojang"),
        (Join-Path $userProfile "curseforge\minecraft\Instances\$ProfileFolderName"),
        (Join-Path $userProfile "Documents\Curse\Minecraft\Instances\$ProfileFolderName")
    )

    foreach ($candidate in $directCandidates) {
        Add-ExistingCandidate -List $paths -Path $candidate
    }

    Add-InstanceCandidates -List $paths -Root (Join-Path $appData "ModrinthApp\profiles")
    Add-InstanceCandidates -List $paths -Root (Join-Path $appData "SKLauncher\instances")
    Add-InstanceCandidates -List $paths -Root (Join-Path $appData ".sklauncher\instances")
    Add-InstanceCandidates -List $paths -Root (Join-Path $appData "PrismLauncher\instances")
    Add-InstanceCandidates -List $paths -Root (Join-Path $appData "PolyMC\instances")
    Add-InstanceCandidates -List $paths -Root (Join-Path $userProfile "curseforge\minecraft\Instances")
    Add-InstanceCandidates -List $paths -Root (Join-Path $userProfile "Documents\Curse\Minecraft\Instances")

    return @($paths)
}

function Select-FolderDialog {
    param([string] $Description)

    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = $Description
    $dialog.ShowNewFolderButton = $false

    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        return $dialog.SelectedPath
    }

    return $null
}

function Select-InstallFolder {
    param(
        [object] $Config,
        [object] $Settings
    )

    $savedFolder = Get-Prop $Settings "game_folder" ""
    if ([string]::IsNullOrWhiteSpace($savedFolder)) {
        $savedFolder = Get-Prop $Settings "target_path" ""
    }
    if (-not $Force -and (Test-PackFolder $savedFolder)) {
        if (Confirm-Message "Usar esta carpeta de Minecraft?`n`n$savedFolder") {
            return $savedFolder
        }
    }

    $profileName = Get-Prop $Config "profile_folder_name" "Harmfy"
    $candidates = @(Get-CandidateFolders $profileName)

    if ($candidates.Count -eq 1) {
        if (Confirm-Message "Detecte esta carpeta de Minecraft:`n`n$($candidates[0])`n`nUsarla?") {
            return $candidates[0]
        }
    } elseif ($candidates.Count -gt 1) {
        $message = "Detecte varias carpetas. Se usara la primera si aceptas:`n`n$($candidates[0])`n`nCancelar permite elegir manualmente."
        if (Confirm-Message $message) {
            return $candidates[0]
        }
    }

    $selected = Select-FolderDialog "Elige la carpeta del perfil/instancia de Minecraft que contiene mods y config."
    if (-not (Test-PackFolder $selected)) {
        throw "La carpeta elegida no parece una carpeta de modpack: $selected"
    }

    return $selected
}

function Select-Variant {
    param(
        [object] $Config,
        [object] $Settings
    )

    $variants = @(Get-Prop $Config "variants" @())
    if ($variants.Count -eq 0) {
        return $null
    }

    if ($variants.Count -eq 1) {
        return $variants[0]
    }

    $askEachTime = [bool](Get-Prop $Config "ask_variant_each_update" $true)
    $savedVariantId = Get-Prop $Settings "variant_id" ""
    if (-not $askEachTime -and -not [string]::IsNullOrWhiteSpace($savedVariantId)) {
        foreach ($variant in $variants) {
            if ((Get-Prop $variant "id" "") -eq $savedVariantId) {
                return $variant
            }
        }
    }

    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    Add-Type -AssemblyName System.Drawing | Out-Null

    $form = New-Object System.Windows.Forms.Form
    $form.Text = "Elegir version de mods"
    $form.StartPosition = "CenterScreen"
    $form.Size = New-Object System.Drawing.Size(560, 260)
    $form.FormBorderStyle = "FixedDialog"
    $form.MaximizeBox = $false
    $form.MinimizeBox = $false
    $form.Topmost = $true

    $label = New-Object System.Windows.Forms.Label
    $label.Text = "Elige que archivo de mods quieres instalar:"
    $label.Location = New-Object System.Drawing.Point(12, 12)
    $label.Size = New-Object System.Drawing.Size(520, 24)
    $form.Controls.Add($label)

    $list = New-Object System.Windows.Forms.ListBox
    $list.Location = New-Object System.Drawing.Point(12, 42)
    $list.Size = New-Object System.Drawing.Size(520, 120)
    $list.Font = New-Object System.Drawing.Font("Segoe UI", 10)
    foreach ($variant in $variants) {
        $display = "{0} - {1}" -f (Get-Prop $variant "label" (Get-Prop $variant "id" "")), (Get-Prop $variant "description" "")
        $list.Items.Add($display) | Out-Null
    }
    if ($list.Items.Count -gt 0) {
        $list.SelectedIndex = 0
    }
    $form.Controls.Add($list)

    $ok = New-Object System.Windows.Forms.Button
    $ok.Text = "Actualizar"
    $ok.Location = New-Object System.Drawing.Point(332, 176)
    $ok.Size = New-Object System.Drawing.Size(95, 30)
    $ok.DialogResult = [System.Windows.Forms.DialogResult]::OK
    $form.AcceptButton = $ok
    $form.Controls.Add($ok)

    $cancel = New-Object System.Windows.Forms.Button
    $cancel.Text = "Cancelar"
    $cancel.Location = New-Object System.Drawing.Point(437, 176)
    $cancel.Size = New-Object System.Drawing.Size(95, 30)
    $cancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
    $form.CancelButton = $cancel
    $form.Controls.Add($cancel)

    $result = $form.ShowDialog()
    if ($result -ne [System.Windows.Forms.DialogResult]::OK -or $list.SelectedIndex -lt 0) {
        return $null
    }

    return $variants[$list.SelectedIndex]
}

function Get-GoogleDriveFileId {
    param([string] $Url)

    if ($Url -match "/file/d/([^/]+)") {
        return $Matches[1]
    }

    if ($Url -match "[?&]id=([^&]+)") {
        return [Uri]::UnescapeDataString($Matches[1])
    }

    if ($Url -match "^[A-Za-z0-9_-]{20,}$") {
        return $Url
    }

    return $null
}

function Get-ArchiveKind {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $null
    }

    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $bytes = New-Object byte[] 8
        $read = $stream.Read($bytes, 0, $bytes.Length)
        if ($read -ge 4 -and $bytes[0] -eq 0x50 -and $bytes[1] -eq 0x4B) {
            return "zip"
        }

        if ($read -ge 4 -and $bytes[0] -eq 0x52 -and $bytes[1] -eq 0x61 -and $bytes[2] -eq 0x72 -and $bytes[3] -eq 0x21) {
            return "rar"
        }
    } finally {
        $stream.Dispose()
    }

    return $null
}

function Test-ArchiveFile {
    param([string] $Path)

    return -not [string]::IsNullOrWhiteSpace((Get-ArchiveKind $Path))
}

function Convert-DriveHtmlUrl {
    param([string] $Href)

    $decoded = $Href.Replace("&amp;", "&").Replace("\u003d", "=").Replace("\u0026", "&")
    if ($decoded.StartsWith("/")) {
        return "https://drive.google.com$decoded"
    }

    return $decoded
}

function Download-GoogleDriveFile {
    param(
        [string] $FileId,
        [string] $OutputPath,
        [string] $WorkRoot
    )

    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $attempts = @(
        "https://drive.usercontent.google.com/download?id=$FileId&export=download&confirm=t",
        "https://drive.google.com/uc?export=download&id=$FileId&confirm=t"
    )

    foreach ($url in $attempts) {
        $temp = Join-Path $WorkRoot ([System.IO.Path]::GetRandomFileName())
        Invoke-WebRequest -Uri $url -OutFile $temp -WebSession $session -UseBasicParsing -MaximumRedirection 10
        if (Test-ArchiveFile $temp) {
            Move-Item -LiteralPath $temp -Destination $OutputPath -Force
            return
        }
        Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
    }

    $landingPath = Join-Path $WorkRoot "drive-download-page.html"
    Invoke-WebRequest -Uri "https://drive.google.com/uc?export=download&id=$FileId" -OutFile $landingPath -WebSession $session -UseBasicParsing -MaximumRedirection 10

    if (Test-ArchiveFile $landingPath) {
        Move-Item -LiteralPath $landingPath -Destination $OutputPath -Force
        return
    }

    $html = Get-Content -LiteralPath $landingPath -Raw
    $downloadUrl = $null
    if ($html -match 'href="([^"]*confirm=[^"]*)"') {
        $downloadUrl = Convert-DriveHtmlUrl $Matches[1]
    } elseif ($html -match '"downloadUrl":"([^"]+)"') {
        $downloadUrl = Convert-DriveHtmlUrl $Matches[1]
    }

    if ([string]::IsNullOrWhiteSpace($downloadUrl)) {
        throw "Google Drive no entrego un link descargable. Revisa que el archivo este compartido para cualquiera con el enlace."
    }

    $tempDownload = Join-Path $WorkRoot ([System.IO.Path]::GetRandomFileName())
    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempDownload -WebSession $session -UseBasicParsing -MaximumRedirection 10
    if (-not (Test-ArchiveFile $tempDownload)) {
        throw "La descarga de Drive no parece ser un .zip o .rar valido."
    }

    Move-Item -LiteralPath $tempDownload -Destination $OutputPath -Force
}

function Download-Archive {
    param(
        [object] $Archive,
        [string] $OutputPath,
        [string] $WorkRoot
    )

    $name = Get-Prop $Archive "name" "Archivo"
    $url = Get-Prop $Archive "download_url" ""
    if ([string]::IsNullOrWhiteSpace($url) -or $url.StartsWith("PEGA_AQUI")) {
        throw "Falta configurar el download_url de: $name"
    }

    Write-Log "Descargando: $name"
    $driveId = Get-GoogleDriveFileId $url
    if (-not [string]::IsNullOrWhiteSpace($driveId)) {
        Download-GoogleDriveFile -FileId $driveId -OutputPath $OutputPath -WorkRoot $WorkRoot
    } else {
        Invoke-WebRequest -Uri $url -OutFile $OutputPath -UseBasicParsing -MaximumRedirection 10
        if (-not (Test-ArchiveFile $OutputPath)) {
            throw "El archivo descargado para '$name' no parece ser .zip o .rar."
        }
    }
}

function Verify-HashIfConfigured {
    param(
        [object] $Archive,
        [string] $Path
    )

    $expected = (Get-Prop $Archive "sha256" "").Trim()
    if ([string]::IsNullOrWhiteSpace($expected)) {
        return
    }

    $actual = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected.ToLowerInvariant()) {
        throw "SHA256 incorrecto para '$((Get-Prop $Archive "name" "archivo"))'. Esperado: $expected. Actual: $actual"
    }
}

function Find-RarExtractor {
    $commands = @("7z.exe", "7za.exe", "WinRAR.exe", "rar.exe")
    foreach ($command in $commands) {
        $found = Get-Command $command -ErrorAction SilentlyContinue
        if ($null -ne $found) {
            if ($command -like "7z*") {
                return @{ Kind = "7z"; Path = $found.Source }
            }
            return @{ Kind = "rar"; Path = $found.Source }
        }
    }

    $candidates = @(
        "$env:ProgramFiles\7-Zip\7z.exe",
        "${env:ProgramFiles(x86)}\7-Zip\7z.exe",
        "$env:ProgramFiles\WinRAR\WinRAR.exe",
        "${env:ProgramFiles(x86)}\WinRAR\WinRAR.exe"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            if ($candidate -like "*7-Zip*") {
                return @{ Kind = "7z"; Path = $candidate }
            }
            return @{ Kind = "rar"; Path = $candidate }
        }
    }

    return $null
}

function Expand-ArchiveFile {
    param(
        [string] $ArchivePath,
        [string] $Destination
    )

    New-Item -ItemType Directory -Path $Destination -Force | Out-Null

    $kind = Get-ArchiveKind $ArchivePath
    if ($kind -eq "zip") {
        Expand-Archive -LiteralPath $ArchivePath -DestinationPath $Destination -Force
        return
    }

    if ($kind -eq "rar") {
        $extractor = Find-RarExtractor
        if ($null -ne $extractor) {
            if ($extractor.Kind -eq "7z") {
                & $extractor.Path x $ArchivePath "-o$Destination" -y | Out-Null
            } else {
                & $extractor.Path x -y $ArchivePath "$Destination\" | Out-Null
            }

            if ($LASTEXITCODE -ne 0) {
                throw "No se pudo extraer el RAR con $($extractor.Path). Codigo: $LASTEXITCODE"
            }
            return
        }

        $tar = Get-Command "tar.exe" -ErrorAction SilentlyContinue
        if ($null -ne $tar) {
            & $tar.Source -xf $ArchivePath -C $Destination 2>$null
            if ($LASTEXITCODE -eq 0) {
                return
            }
        }

        throw "Este Windows no puede extraer .rar. Instala 7-Zip/WinRAR o sube los archivos como .zip."
    }

    throw "Formato de archivo no reconocido. Solo se acepta .zip o .rar."
}

function Get-FolderMappings {
    param([object] $Archive)

    $folders = @(Get-Prop $Archive "folders" @())
    $mappings = New-Object System.Collections.Generic.List[object]

    foreach ($folder in $folders) {
        if ($folder -is [string]) {
            $mappings.Add([pscustomobject]@{
                Source = $folder
                Target = $folder
                Required = $false
            }) | Out-Null
        } else {
            $source = Get-Prop $folder "source" (Get-Prop $folder "name" "")
            $target = Get-Prop $folder "target" $source
            $required = [bool](Get-Prop $folder "required" $false)
            if (-not [string]::IsNullOrWhiteSpace($source) -and -not [string]::IsNullOrWhiteSpace($target)) {
                $mappings.Add([pscustomobject]@{
                    Source = $source
                    Target = $target
                    Required = $required
                }) | Out-Null
            }
        }
    }

    return @($mappings)
}

function Get-PayloadRoot {
    param(
        [string] $ExtractRoot,
        [object[]] $Mappings
    )

    foreach ($mapping in $Mappings) {
        if ($mapping.Source -eq ".") {
            return $ExtractRoot
        }

        if (Test-Path -LiteralPath (Join-Path $ExtractRoot $mapping.Source) -PathType Container) {
            return $ExtractRoot
        }
    }

    $children = @(Get-ChildItem -LiteralPath $ExtractRoot -Directory -ErrorAction SilentlyContinue)
    if ($children.Count -eq 1) {
        $child = $children[0].FullName
        foreach ($mapping in $Mappings) {
            if ($mapping.Source -eq "." -or (Test-Path -LiteralPath (Join-Path $child $mapping.Source) -PathType Container)) {
                return $child
            }
        }
    }

    return $ExtractRoot
}

function Assert-ChildPath {
    param(
        [string] $Child,
        [string] $Parent
    )

    $parentFull = [System.IO.Path]::GetFullPath($Parent).TrimEnd('\', '/')
    $childFull = [System.IO.Path]::GetFullPath($Child).TrimEnd('\', '/')
    $prefix = $parentFull + [System.IO.Path]::DirectorySeparatorChar

    if ($childFull -eq $parentFull -or -not $childFull.StartsWith($prefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Ruta peligrosa fuera de la carpeta del modpack: $childFull"
    }
}

function Backup-And-ReplaceFolder {
    param(
        [string] $SourcePath,
        [string] $TargetRoot,
        [string] $TargetName,
        [string] $BackupRoot,
        [bool] $BackupEnabled,
        [bool] $CopySourceContents
    )

    $targetPath = Join-Path $TargetRoot $TargetName
    Assert-ChildPath -Child $targetPath -Parent $TargetRoot

    if ($BackupEnabled -and (Test-Path -LiteralPath $targetPath)) {
        $backupPath = Join-Path $BackupRoot $TargetName
        New-Item -ItemType Directory -Path (Split-Path -Parent $backupPath) -Force | Out-Null
        Copy-Item -LiteralPath $targetPath -Destination $backupPath -Recurse -Force
        Write-Log "Backup: $TargetName"
    }

    if (Test-Path -LiteralPath $targetPath) {
        Remove-Item -LiteralPath $targetPath -Recurse -Force
    }

    if ($CopySourceContents) {
        New-Item -ItemType Directory -Path $targetPath -Force | Out-Null
        Get-ChildItem -LiteralPath $SourcePath -Force | ForEach-Object {
            Copy-Item -LiteralPath $_.FullName -Destination $targetPath -Recurse -Force
        }
    } else {
        Copy-Item -LiteralPath $SourcePath -Destination $targetPath -Recurse -Force
    }

    Write-Log "Instalado: $TargetName"
}

function Install-OneArchive {
    param(
        [object] $Archive,
        [string] $TargetRoot,
        [string] $WorkRoot,
        [string] $BackupRoot,
        [bool] $BackupEnabled,
        [int] $Index
    )

    $name = Get-Prop $Archive "name" "Archivo $Index"
    $archivePath = Join-Path $WorkRoot ("archive-{0}.download" -f $Index)
    $extractRoot = Join-Path $WorkRoot ("extract-{0}" -f $Index)
    $mappings = @(Get-FolderMappings $Archive)

    if ($mappings.Count -eq 0) {
        throw "El archivo '$name' no tiene carpetas configuradas."
    }

    Download-Archive -Archive $Archive -OutputPath $archivePath -WorkRoot $WorkRoot
    Verify-HashIfConfigured -Archive $Archive -Path $archivePath

    Write-Log "Extrayendo: $name"
    Expand-ArchiveFile -ArchivePath $archivePath -Destination $extractRoot

    $payloadRoot = Get-PayloadRoot -ExtractRoot $extractRoot -Mappings $mappings
    $installedCount = 0
    foreach ($mapping in $mappings) {
        $copySourceContents = $false
        if ($mapping.Source -eq ".") {
            $sourcePath = $payloadRoot
            $copySourceContents = $true
        } else {
            $sourcePath = Join-Path $payloadRoot $mapping.Source
        }

        if (-not (Test-Path -LiteralPath $sourcePath -PathType Container)) {
            if ($mapping.Required) {
                throw "El archivo '$name' no contiene la carpeta esperada '$($mapping.Source)'."
            }
            Write-Log "Saltado: '$name' no contiene '$($mapping.Source)'"
            continue
        }

        Backup-And-ReplaceFolder `
            -SourcePath $sourcePath `
            -TargetRoot $TargetRoot `
            -TargetName $mapping.Target `
            -BackupRoot $BackupRoot `
            -BackupEnabled $BackupEnabled `
            -CopySourceContents $copySourceContents
        $installedCount++
    }

    if ($installedCount -eq 0 -and [bool](Get-Prop $Archive "required" $true)) {
        throw "El archivo '$name' no tenia ninguna carpeta esperada para instalar."
    }
}

function Get-ArchiveQueue {
    param(
        [object] $Config,
        [object] $Variant
    )

    $queue = New-Object System.Collections.Generic.List[object]

    foreach ($archive in @(Get-Prop $Config "common_archives" @())) {
        $queue.Add($archive) | Out-Null
    }

    if ($null -ne $Variant) {
        foreach ($archive in @(Get-Prop $Variant "archives" @())) {
            $queue.Add($archive) | Out-Null
        }
    }

    # Legacy fallback for the first updater format.
    $legacyUrl = Get-Prop $Config "download_url" ""
    if ($queue.Count -eq 0 -and -not [string]::IsNullOrWhiteSpace($legacyUrl)) {
        $queue.Add([pscustomobject]@{
            name = "Pack completo"
            download_url = $legacyUrl
            sha256 = Get-Prop $Config "sha256" ""
            required = $true
            folders = @(Get-Prop $Config "replace_folders" @("mods", "config", "defaultconfigs", "datapacks"))
        }) | Out-Null
    }

    return @($queue)
}

function Read-VersionFile {
    param([string] $TargetRoot)

    $path = Join-Path $TargetRoot ".harmfy-version.json"
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        return $null
    }

    try {
        return Read-JsonFile $path
    } catch {
        return $null
    }
}

function Save-VersionFile {
    param(
        [string] $TargetRoot,
        [object] $Config,
        [object] $Variant
    )

    $path = Join-Path $TargetRoot ".harmfy-version.json"
    $value = [pscustomobject]@{
        pack_name = Get-Prop $Config "pack_name" "Harmfy"
        pack_version = Get-Prop $Config "pack_version" ""
        updater_version = Get-Prop $Config "updater_version" ""
        variant_id = if ($null -ne $Variant) { Get-Prop $Variant "id" "" } else { "" }
        variant_label = if ($null -ne $Variant) { Get-Prop $Variant "label" "" } else { "" }
        installed_at = (Get-Date).ToString("o")
    }

    Save-JsonFile -Path $path -Value $value
}

try {
    Write-Log "Iniciando Harmfy Updater"

    $config = Read-JsonFile $configPath
    $settings = if (Test-Path -LiteralPath $settingsPath -PathType Leaf) {
        Read-JsonFile $settingsPath
    } else {
        [pscustomobject]@{}
    }

    $targetRoot = Select-InstallFolder -Config $config -Settings $settings
    $variant = Select-Variant -Config $config -Settings $settings
    if ((@(Get-Prop $config "variants" @())).Count -gt 0 -and $null -eq $variant) {
        Write-Log "Actualizacion cancelada por el jugador."
        exit 0
    }

    $packVersion = Get-Prop $config "pack_version" ""
    $variantId = if ($null -ne $variant) { Get-Prop $variant "id" "" } else { "" }
    $installedVersion = Read-VersionFile $targetRoot
    $installedPackVersion = Get-Prop $installedVersion "pack_version" ""
    $installedVariantId = Get-Prop $installedVersion "variant_id" ""
    $skipIfSame = [bool](Get-Prop $config "skip_if_same_version" $true)

    if (-not $Force -and $skipIfSame -and $installedPackVersion -eq $packVersion -and $installedVariantId -eq $variantId) {
        $label = if ($null -ne $variant) { Get-Prop $variant "label" $variantId } else { "" }
        Show-Message "Ya tienes Harmfy $packVersion instalado.`nVariante: $label"
        exit 0
    }

    $archiveQueue = @(Get-ArchiveQueue -Config $config -Variant $variant)
    if ($archiveQueue.Count -eq 0) {
        throw "No hay archivos configurados para descargar."
    }

    $settings | Add-Member -NotePropertyName "game_folder" -NotePropertyValue $targetRoot -Force
    $settings | Add-Member -NotePropertyName "variant_id" -NotePropertyValue $variantId -Force
    Save-JsonFile -Path $settingsPath -Value $settings

    $workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("HarmfyUpdater-{0}" -f ([Guid]::NewGuid().ToString("N")))
    New-Item -ItemType Directory -Path $workRoot -Force | Out-Null

    $backupEnabled = [bool](Get-Prop $config "backup_before_update" $true)
    $backupRoot = Join-Path $targetRoot (".harmfy-backups\{0}" -f (Get-Date -Format "yyyyMMdd-HHmmss"))
    if ($backupEnabled) {
        New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
    }

    $index = 0
    foreach ($archive in $archiveQueue) {
        $index++
        Install-OneArchive `
            -Archive $archive `
            -TargetRoot $targetRoot `
            -WorkRoot $workRoot `
            -BackupRoot $backupRoot `
            -BackupEnabled $backupEnabled `
            -Index $index
    }

    Save-VersionFile -TargetRoot $targetRoot -Config $config -Variant $variant

    Remove-Item -LiteralPath $workRoot -Recurse -Force -ErrorAction SilentlyContinue

    $variantLabel = if ($null -ne $variant) { "`nVariante: $((Get-Prop $variant "label" $variantId))" } else { "" }
    Show-Message "Actualizacion completada.`nVersion: $packVersion$variantLabel`n`nCarpeta:`n$targetRoot"
    Write-Log "Actualizacion completada"
    exit 0
} catch {
    $message = $_.Exception.Message
    Write-Log "ERROR: $message"
    Show-Message "No se pudo actualizar Harmfy:`n`n$message`n`nRevisa updater.log junto al .bat." "Harmfy Updater" "Error"
    exit 1
}
