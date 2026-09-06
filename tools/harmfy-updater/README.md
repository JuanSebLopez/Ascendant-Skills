# Harmfy Updater

Actualizador portable para Windows. Los jugadores solo deben abrir `Actualizar Harmfy.bat`.

## Preparacion

1. Sube un `.zip` del modpack a Drive o a un enlace directo.
2. Edita `updater-config.json`.
3. Cambia:

```json
"pack_version": "0.6.5",
"download_url": "PEGA_AQUI_EL_LINK_DE_DRIVE_O_DIRECTO_DEL_ZIP",
"sha256": ""
```

`sha256` puede quedar vacio. Si lo llenas, el updater verifica que el zip descargado coincida.

El zip debe contener carpetas como:

```text
mods/
config/
defaultconfigs/
datapacks/
resourcepacks/
shaderpacks/
```

No metas `saves`, `options.txt`, `servers.dat` ni screenshots en el zip.

## Primer uso del jugador

El updater intenta detectar:

- Modrinth: `%APPDATA%\ModrinthApp\profiles\Harmfy`
- Minecraft/TLauncher: `%APPDATA%\.minecraft`
- SKLauncher: rutas comunes de instances
- Prism/PolyMC: rutas comunes de instances

Si no encuentra una carpeta clara, abre un selector. Esa ruta queda guardada en `local-settings.json`.

## Backups

Antes de reemplazar carpetas crea:

```text
.harmfy-backups/yyyyMMdd-HHmmss/
```

## Nota

El `.bat` usa `ExecutionPolicy Bypass` solo para esta ejecucion de PowerShell. No cambia la politica global de Windows.
