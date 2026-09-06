# Harmfy Updater

Actualizador portable para Windows. Los jugadores solo deben abrir `Actualizar Harmfy.bat`.

## Que descarga

Este updater descarga solamente:

- `config.rar`
- una variante de mods elegida por el jugador:
  - `mods (con tarjeta grafica).rar`
  - `mods (no tarjeta grafica).rar`

No descarga `shaderpacks.rar`, resource packs ni otros archivos visuales aunque esten en la carpeta de Drive.

## Preparacion

1. Sube o reemplaza en Drive estos archivos: `config.rar`, `mods (con tarjeta grafica).rar` y `mods (no tarjeta grafica).rar`.
2. Abre `updater-config.json`.
3. Cambia los tres `download_url` por links directos o links compartidos de cada archivo de Google Drive.
4. Sube `pack_version` cada vez que cambies algo del pack.

`sha256` puede quedar vacio. Si lo llenas, el updater verifica que el archivo descargado coincida.

Los `.rar` deben contener estas carpetas:

```text
config.rar
  config/
  defaultconfigs/
  datapacks/

mods (con tarjeta grafica).rar
  mods/

mods (no tarjeta grafica).rar
  mods/
```

Si tus jugadores no tienen 7-Zip o WinRAR, es mejor subir esos archivos como `.zip`. El updater soporta `.zip` sin instalar nada adicional.

## Primer uso del jugador

El updater intenta detectar:

- Modrinth: `%APPDATA%\ModrinthApp\profiles\Harmfy`
- Minecraft/TLauncher: `%APPDATA%\.minecraft`
- SKLauncher: rutas comunes de instances
- Prism/PolyMC: rutas comunes de instances

Si no encuentra una carpeta clara, abre un selector. Esa ruta queda guardada en `local-settings.json`.

## Variantes

Por defecto el updater pregunta en cada ejecucion si quiere usar la variante con tarjeta grafica o sin tarjeta grafica. Si quieres que recuerde la ultima variante sin preguntar, cambia esto:

```json
"ask_variant_each_update": false
```

## Backups

Antes de reemplazar carpetas crea:

```text
.harmfy-backups/yyyyMMdd-HHmmss/
```

## Nota

El `.bat` usa `ExecutionPolicy Bypass` solo para esta ejecucion de PowerShell. No cambia la politica global de Windows.
