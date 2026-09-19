---
description: >-
  Language, message format and log level in config.yml, the language files
  behind every message, and the files the plugin writes.
---

# Configuration

The pools themselves aren't configured here. They live in their own files, managed through commands (see [Pool types](pool-types.md#where-pools-are-stored)). `plugins/LootPool/config.yml` holds only the plugin's own settings:

```yaml
regenerate: true
lang: 'en.yml'
usePlayerLanguage: true
# LEGACY/ENHANCED_LEGACY
serializer: ENHANCED_LEGACY
# OFF/FATAL/ERROR/WARN/INFO/DEBUG/TRACE/ALL
logLevel: INFO
```

| Key                 | What it does                                                                                                                                                  |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `regenerate`        | When `true`, the next startup merges any keys that are new in this version into your config, backs the old one up, and removes this flag. It ships as `true`. |
| `lang`              | The default language file in `lang/`. `en.yml` and `ru.yml` ship with the plugin.                                                                             |
| `usePlayerLanguage` | When `true`, each player gets messages in their client's language if a matching file exists, and in `lang` otherwise.                                         |
| `serializer`        | How the language files are parsed. The bundled files are written for `ENHANCED_LEGACY`; see [Language files](configuration.md#language-files).                |
| `logLevel`          | Threshold for the plugin's own logging. `DEBUG` logs each pool as it loads, which helps find a pool file that fails to load.                                  |

After updating the plugin, set `regenerate: true` and restart to pick up settings the new version added.

## Language files

Every message and every piece of editor text lives in `plugins/LootPool/lang/`. Edit `en.yml` or `ru.yml` there, or add a file named after another client language (`de_de.yml`, say) for `usePlayerLanguage` to pick up. Every file in the folder is loaded. Keys missing from the bundled `en.yml` and `ru.yml` are filled in from the plugin's own copies.

With `serializer: ENHANCED_LEGACY` messages use [EnhancedLegacyText](https://github.com/Vankka/EnhancedLegacyText) tags: `[red]`, `[italic:off]`, `[hover:show_text:…]`, `[click:run_command:…]`. `<name>` marks a value filled in by the plugin.

| Key                                                         | Used for                                                                                                            |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `gui-loot`                                                  | Lore lines added to each entry in [the editor](the-loot-editor.md), with `<weight>`, `<min-amount>`, `<max-amount>` |
| `gui-nothing-item`                                          | Name of the Nothing slot                                                                                            |
| `info-lootpool`, `info-lootpool-loot`, `info-lootpool-more` | The `/lootpool info` listing: header, one line per entry, and the "and N more" line                                 |
| `info-lootpool-already-exists`, `info-loot-already-exists`  | The "already exists" error with a **Modify** link, shown to players with `lootpool.modify`                          |
| `error-*`                                                   | Command errors: pool not found, already exists, invalid key, empty hand, not a container, bad `pool:value` entry    |
| `success-*`                                                 | Confirmations for reload, create, remove, and loot handed out                                                       |

## Applying changes

`/lootpool reload` re-reads `config.yml`, the log level, the language files, and every pool file. A restart does the same.

Pools are re-read from disk, so a pool file you edited by hand is picked up. Pools another plugin added only in memory are dropped; see [Using it from your plugin](using-it-from-your-plugin.md#creating-pools).

## Files the plugin writes

| Path                          | Contents                                                                                      |
| ----------------------------- | --------------------------------------------------------------------------------------------- |
| `plugins/LootPool/config.yml` | Your config                                                                                   |
| `plugins/LootPool/lang/`      | Language files, yours to edit                                                                 |
| `plugins/LootPool/lootpool/`  | One YAML file per pool, named after its key                                                   |
| `plugins/LootPool/defaults/`  | The bundled config and language files, rewritten on every start for merging. Don't edit them. |
| `plugins/LootPool/backups/`   | A copy of `config.yml` from before each `regenerate` merge                                    |
