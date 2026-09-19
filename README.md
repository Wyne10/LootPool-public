---
description: >-
  Weighted loot tables you build in-game from real items, then give, drop,
  spawn, or fill containers with from commands, menus, or your own plugin.
---

# LootPool

LootPool is a Paper plugin for building loot tables in-game. You put real items into a chest GUI, set a weight and an amount range for each one, and LootPool saves the result as a named **loot pool**. From then on any command, AbstractMenus menu, or other plugin can roll that pool: give the loot to a player, drop it on the ground, or fill a chest with it.

A pool isn't limited to a flat list of items. It can merge other pools, pick between them by weight, wrap a vanilla loot table, replay an exact chest layout, or roll a random number of items each time. See [Pool types](pool-types.md).

## What it gives you

* **An in-game editor.** `/lootpool pool create <key>` opens a chest GUI: drop items in, then click to set each one's weight and minimum and maximum amount. No item stacks to write by hand. See [The loot editor](the-loot-editor.md).
* **Seven pool types that build on each other.** Plain weighted lists, single named items, pools of pools (merged or weighted against each other), roll-count wrappers, container snapshots, and vanilla loot tables. See [Pool types](pool-types.md).
* **A command for every way loot leaves a pool.** Into a player's inventory, onto the ground, into a container, or into a preview window, plus bulk edits of weights and amounts. See [Commands and permissions](commands-and-permissions.md).
* **AbstractMenus integration.** A `lootPool` item property and a `LOOTPOOL` catalog for generated menus. See AbstractMenus integration.
* **A small API.** Other plugins compile against `io.github.wyne10:lootpool-api` from Maven Central, look pools up by key, and roll them or create new ones. See Using it from your plugin.

## Requirements

|          |                                                                                      |
| -------- | ------------------------------------------------------------------------------------ |
| Server   | Paper 1.16.5 or later. Plain Spigot won't do: the plugin uses Paper's Adventure API. |
| Java     | 16 or later                                                                          |
| Optional | [CommandAPI](https://commandapi.jorel.dev), for the `/lootpool` command              |
| Optional | [AbstractMenus](https://abstractmenus.github.io/docs/en/), for the menu integration  |

Both are soft dependencies, but CommandAPI is the only way to manage pools in-game. Without it LootPool still loads the pools on disk and serves them to AbstractMenus and other plugins, but there is no `/lootpool` command at all.

## How it fits together

1. LootPool enables, reads its config and language files, and publishes its `LootPoolProvider` through the API and the Bukkit services manager.
2. It loads every pool in `plugins/LootPool/lootpool/` into memory, one pool per file, keyed by file name.
3. You create and edit pools with commands and the editor. Each change is written to that pool's file straight away.
4. Commands, menus and other plugins roll pools by key. Pools that point at other pools look them up again on every roll, so an edit to a child pool shows up in every pool built on it at once.

To compile the plugin yourself, see Building from source.
