---
description: >-
  Every /lootpool subcommand: building and editing pools, getting loot out of
  them, and the permission behind each one.
---

# Commands and permissions

Everything lives under `/lootpool`. Run `/lootpool help` in-game for a clickable list, `/lootpool <group> help` for one group — `pool`, `item`, `complex`, `enchant`, and the groups inside `complex` — and `/lootpool help <command>` for the full description of one command.

Pool keys tab-complete wherever a command expects one, and most arguments under `complex` suggest with a tooltip describing what each one points at — see [Read the tooltips](commands-and-permissions.md#complex-loot-pools).

## Building and editing pools

| Command                                                        | Permission        | What it does                                                                                                        |
| -------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| `/lootpool pool create <key>`                                  | `lootpool.create` | Opens [the editor](the-loot-editor.md) for a new basic pool                                                         |
| `/lootpool pool modify <key>`                                  | `lootpool.modify` | Opens the editor on an existing basic pool                                                                          |
| `/lootpool pool merge <destination> <source>`                  | `lootpool.modify` | Adds the entries of `source` (any type) to the basic pool `destination`                                             |
| `/lootpool pool weight <key> <operation>`                      | `lootpool.modify` | Applies an [operation](commands-and-permissions.md#operations) to every weight in a basic pool                      |
| `/lootpool pool amount <key> min\|max <operation>`             | `lootpool.modify` | Applies an operation to every minimum or every maximum amount, kept between 1 and the item's maximum stack size     |
| `/lootpool pool flatten <key> <pool:operation>…`               | `lootpool.create` | Builds a new basic pool from the entries of several pools, applying a weight operation to each pool's entries first |
| `/lootpool item create <key> [minAmount] [maxAmount] [weight]` | `lootpool.create` | Creates a [keyed loot](pool-types.md#keyed-loot) from the item in your main hand                                    |
| `/lootpool item modify <key> [minAmount] [maxAmount] [weight]` | `lootpool.modify` | Replaces a keyed loot's item with the one in your hand. Values you leave out keep their old setting.                |
| `/lootpool include <key> <pool> [pool…]`                       | `lootpool.create` | Creates a [multi pool](pool-types.md#multi)                                                                         |
| `/lootpool compose <key> <pool:weight>…`                       | `lootpool.create` | Creates a [composite pool](pool-types.md#composite), e.g. `common:80 rare:20`                                       |
| `/lootpool roll <key> <pool> [minRolls] [maxRolls]`            | `lootpool.create` | Creates a [roll pool](pool-types.md#roll) around an existing pool                                                   |
| `/lootpool snapshot <key> [x y z]`                             | `lootpool.create` | Creates a [snapshot](pool-types.md#snapshot) of the container at `x y z`, or of your own inventory                  |
| `/lootpool register <key> <loot table>`                        | `lootpool.create` | Creates a [vanilla pool](pool-types.md#vanilla), e.g. `minecraft:chests/simple_dungeon`                             |
| `/lootpool clone <key> <newKey>`                               | `lootpool.create` | Copies a pool of any type under a new key, writing it straight away                                                 |
| `/lootpool remove <key>`                                       | `lootpool.remove` | Deletes a pool of any type, and its file                                                                            |
| `/lootpool info <key> [amount] [sort] [depth]`                 | `lootpool.info`   | Lists a pool's entries in chat with their amounts, weights and chances                                              |
| `/lootpool reload`                                             | `lootpool.reload` | Re-reads the config, language files and every pool from disk                                                        |

The `pool` commands and `merge` work only on basic pools, but `merge` and `flatten` accept a source of any type. Only its entries are taken; a composite's weighted pick, a roll pool's range or a complex pool's structure doesn't carry over, and a vanilla pool contributes one sample roll. The source pools themselves stay as they were.

`info` shows the first 15 entries unless you pass `amount`, and sorts them by `weight` unless you pass `name`, `min-amount` or `max-amount`. For a multi or composite pool it lists the pools it's built from, each with its share of the rolls. For a [complex pool](complex-loot-pools.md) it prints the tree instead, `depth` levels deep (1 to 3, 3 by default), with every line clickable to fill in a command addressed at that node.

If you try to create a pool that already exists and you hold `lootpool.modify`, the error comes with a clickable **Modify** link.

### Operations

`weight`, `amount`, `flatten` and `enchant entry set` take an operation: an optional operator followed by a number.

| Operation | Result                              |
| --------- | ----------------------------------- |
| `+5`      | Add 5                               |
| `-1`      | Subtract 1                          |
| `*1000`   | Multiply by 1000                    |
| `/3`      | Divide by 3                         |
| `**2`     | Raise to the power of 2             |
| `%4`      | Take the remainder of dividing by 4 |
| `<10`     | Clamp down to at most 10            |
| `>1`      | Clamp up to at least 1              |
| `10`      | Set to 10                           |

`<` and `>` **clamp, rather than compare**: `<10` leaves 4 as 4 and turns 25 into 10, and `>1` raises anything below 1 up to it. A bare number with no operator sets the value outright.

`flatten` takes one operation per pool: `/lootpool pool flatten ores iron_ores:*3 gold_ores:+0 diamond_ores:1`.

## Complex loot pools

The `complex` group builds and edits [complex pools](complex-loot-pools.md). Everything here is also reachable from `/lootpool complex edit`, the GUI — the commands add nothing the editor lacks, and the editor adds nothing the commands lack, except that text fields are command-only.

{% hint style="success" %}
**Read the tab-completion tooltips.** Rolls, entries, modifiers and conditions are addressed by number, which would be miserable to work with blind — so every one of those arguments suggests only the elements that actually exist, each annotated with a description of what it points at. A roll shows as `Roll 0 [3..5] — 2 entry(s), 1 modifier(s), 0 condition(s)`, an entry as `Item DIAMOND_SWORD x1-1 (weight 10)`, a modifier as `Enchant from 'sword_enchants' x1-3`, a condition as `Chance 80%`. The `path` argument describes every node it offers, the enchantment level arguments carry the enchantment's own vanilla maximum, and `/lootpool help <command>` annotates each subcommand with its short description.

The lists are **filtered by type** as well, so `modifier set lore` offers only the lore modifiers and `entry set amount` only the item entries. An index you expected but don't see is the wrong type for that subcommand rather than missing.

Hovering the suggestion list is almost always faster than running `/lootpool info` first.
{% endhint %}

| Command                                                                          | Permission        | What it does                                                                         |
| -------------------------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------ |
| `/lootpool complex create <key>`                                                 | `lootpool.create` | Creates an empty complex pool                                                        |
| `/lootpool complex convert <source> <key> [minRolls] [maxRolls]`                 | `lootpool.create` | Builds one from an existing pool: every entry becomes an item entry in a single roll |
| `/lootpool complex edit <key>`                                                   | `lootpool.modify` | Opens the editor                                                                     |
| `/lootpool complex roll add <key> [minRolls] [maxRolls]`                         | `lootpool.modify` | Appends a roll                                                                       |
| `/lootpool complex roll set <key> <roll> <minRolls> [maxRolls]`                  | `lootpool.modify` | Changes a roll's roll count                                                          |
| `/lootpool complex roll remove\|move <key> <roll> [delta]`                       | `lootpool.modify` | Removes or reorders a roll                                                           |
| `/lootpool complex entry add item <key> <roll> [weight] [minAmount] [maxAmount]` | `lootpool.modify` | Adds the item in your hand as an entry                                               |
| `/lootpool complex entry add pool <key> <roll> <pool> [weight]`                  | `lootpool.modify` | Adds a reference to another pool                                                     |
| `/lootpool complex entry add empty <key> <roll> [weight]`                        | `lootpool.modify` | Adds a "nothing" entry                                                               |
| `/lootpool complex entry remove\|move <key> <roll> <entry> [delta]`              | `lootpool.modify` | Removes or reorders an entry                                                         |
| `/lootpool complex entry set item\|pool\|weight\|amount …`                       | `lootpool.modify` | Changes one property of an entry, keeping its modifiers and conditions               |
| `/lootpool complex modifier add <type> <key> <path> …`                           | `lootpool.modify` | Attaches an item modifier                                                            |
| `/lootpool complex modifier remove\|move <key> <path> <modifier> [delta]`        | `lootpool.modify` | Removes or reorders a modifier                                                       |
| `/lootpool complex modifier conditional <key> <path> <modifier> <true\|false>`   | `lootpool.modify` | Wraps or unwraps a modifier so it applies only when its own conditions pass          |
| `/lootpool complex modifier set <type> …`                                        | `lootpool.modify` | Changes a modifier's settings in place                                               |
| `/lootpool complex condition add <type> <key> <path> …`                          | `lootpool.modify` | Attaches a condition                                                                 |
| `/lootpool complex condition remove\|move <key> <path> <condition> [delta]`      | `lootpool.modify` | Removes or reorders a condition                                                      |
| `/lootpool complex condition invert <key> <path> <condition> <true\|false>`      | `lootpool.modify` | Negates a condition, or un-negates it                                                |
| `/lootpool complex condition set <type> …`                                       | `lootpool.modify` | Changes a condition's settings in place                                              |

`<type>` is a literal: `enchant`, `damage`, `attributes`, `name`, `lore`, `amount`, `limit`, `type`, `smelt` or `discard` for modifiers, and `chance`, `permission`, `world`, `biome`, `time`, `weather`, `hasloot`, `placeholder`, `allof` or `anyof` for conditions. Each one takes its own arguments, so tab-completion and `/lootpool help` describe them individually.

`<path>` addresses whatever owns the list being edited: `pool`, a roll like `0`, an entry like `0.2`, or deeper — see [Addressing by path](complex-loot-pools.md#addressing-by-path).

`modifier set lore` and `modifier set attribute` are groups of their own, because those modifiers hold a list you edit one element at a time:

```
/lootpool complex modifier set lore add|remove|mode <key> <path> <modifier> …
/lootpool complex modifier set attribute add|remove|replace <key> <path> <modifier> …
```

`smelt` and `discard` have nothing to set — remove and re-add them instead.

## Enchantment pools

The `enchant` group manages [enchantment pools](enchantment-pools.md), a registry of its own:

```
/lootpool enchant create|clone|remove|info|edit <key> …
/lootpool enchant entry add <key> <enchantment> [weight] [minLevel] [maxLevel]
/lootpool enchant entry remove <key> <entry>
/lootpool enchant entry set <key> <entry> weight|min-level|max-level <operation>
```

`create` and `clone` need `lootpool.create`, `remove` needs `lootpool.remove`, `info` needs `lootpool.info`, and `edit` and everything under `entry` need `lootpool.modify`.

## Getting loot out of a pool

These commands come in two families, and the difference matters for anything more than a basic pool.

### Drawing entries directly

`give`, `drop` and `insert` take the pool's entry list and make the draws themselves. That lets them offer `amount` and `unique`, but it also means they **ignore how the pool is built**. A composite pool counts as all its child pools' entries together with no weighted pick. A roll pool's range, a snapshot's slots and a vanilla table's own logic don't apply. Unless you pass `slots`, they make as many draws as the pool has entries.

| Command                                                             | Permission         | Puts the loot…                                                      |
| ------------------------------------------------------------------- | ------------------ | ------------------------------------------------------------------- |
| `/lootpool give <key> <player> [amount] [unique] [slots]`           | `lootpool.give`    | In a player's inventory. What doesn't fit is dropped at their feet. |
| `/lootpool drop <key> <x y z> [amount] [unique] [slots]`            | `lootpool.drop`    | On the ground at a location                                         |
| `/lootpool insert <key> <x y z> [amount] [random] [unique] [slots]` | `lootpool.insert`  | In the container at a block location. What doesn't fit is lost.     |
| `/lootpool project <key> <player> [amount]`                         | `lootpool.project` | In a player's inventory, **one of every entry**, ignoring weights   |

* `amount` sets each item's stack size: `min-amount`, `max-amount`, `random-amount` (the default), or a number.
* `unique` set to `true` stops an entry from coming up twice. Once every entry has been drawn, the remaining draws produce nothing.
* `random` set to `true` scatters the items across random empty slots instead of filling slots in order.

{% hint style="info" %}
A [complex pool](complex-loot-pools.md) is the exception. `give`, `drop` and `insert` run its real pipeline rather than drawing from its flattened list, because the flattened list has none of its rolls, modifiers or conditions in it. `amount` and `unique` have no effect on one — the pool decides its own stack sizes and its own item count. `project` still flattens, because it is a preview by definition.
{% endhint %}

### Letting the pool roll itself

`fill`, `populate`, `spawn` and `preview` hand the work to the pool. A composite picks one child pool, a roll pool uses its range, a snapshot puts items in their saved slots, a vanilla pool runs the vanilla table, and a complex pool runs its rolls, modifiers and conditions.

| Command                                     | Permission          | What it does                                                                                                                                        |
| ------------------------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/lootpool fill <key> <x y z> [random]`     | `lootpool.fill`     | Fills the container at a block location. What doesn't fit is lost.                                                                                  |
| `/lootpool populate <key> <player> [slots]` | `lootpool.populate` | Fills a player's inventory. What doesn't fit is dropped at their feet.                                                                              |
| `/lootpool spawn <key> <x y z> [slots]`     | `lootpool.spawn`    | Drops the loot on the ground. A roll or complex pool uses its own count when `slots` is left out; **any other pool drops nothing without `slots`**. |
| `/lootpool preview <key> [rows]`            | `lootpool.info`     | Opens a chest of 1 to 6 rows (3 by default) filled from the pool                                                                                    |

Without a slot count, a basic, multi or composite pool makes **one draw per empty slot** of the target, so `fill` fills the whole chest and `populate` fills the player's whole inventory, armor and off-hand slots included. For a sensible amount of loot, point these commands at a [roll pool](pool-types.md#roll) or pass `slots`.

{% hint style="danger" %}
The preview is an ordinary chest, so the items in it are real and can be taken out. Give `lootpool.info` only to staff.
{% endhint %}

### What each command knows about the roll

A complex pool's conditions can ask about the player, the world, the biome, the time and the weather. Each command supplies only what it actually has, and a condition that can't be answered **fails**.

| Command                           | Knows                            |
| --------------------------------- | -------------------------------- |
| `give`, `populate`                | The player, and where they stand |
| `drop`, `insert`, `spawn`, `fill` | The location only                |
| `preview`, `project`              | Neither                          |

So `/lootpool fill` at a chest can gate on world, biome, time and weather, but a `permission` condition there never passes. `/lootpool preview` knows nothing at all, which makes it a poor way to test a gated pool — every condition but `chance` fails in it. A pool rolled from another plugin knows whatever that plugin passes; see [Using it from your plugin](using-it-from-your-plugin.md#rolling-a-complex-pool).

### From the console and command blocks

Everything in [Getting loot out of a pool](commands-and-permissions.md#getting-loot-out-of-a-pool) except `preview`, plus `info`, `clone`, `remove`, `include`, `compose`, `roll`, `register`, `reload`, and every `complex` and `enchant` command except the two `edit` commands, works from the console and command blocks. That makes these commands handy for rewards, quests and scheduled refills, and lets a setup script build a whole loot table from nothing. The rest need a player: every `pool` and `item` command, `snapshot`, `preview`, `complex entry add item`, `complex entry set item`, and the editors.

The "Dropped N items" confirmation goes to players only, so console and command-block runs are silent.

## Permissions

Every permission defaults to operators.

| Permission          | Grants                                                                                                                                                                              |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `lootpool.create`   | `pool create`, `pool flatten`, `item create`, `include`, `compose`, `roll`, `snapshot`, `register`, `clone`, `complex create`, `complex convert`, `enchant create`, `enchant clone` |
| `lootpool.modify`   | `pool modify`, `pool merge`, `pool weight`, `pool amount`, `item modify`, everything under `complex` that edits a pool, `enchant edit`, `enchant entry …`                           |
| `lootpool.remove`   | `remove`, `enchant remove`                                                                                                                                                          |
| `lootpool.info`     | `info`, `preview`, `enchant info`                                                                                                                                                   |
| `lootpool.give`     | `give`                                                                                                                                                                              |
| `lootpool.drop`     | `drop`                                                                                                                                                                              |
| `lootpool.insert`   | `insert`                                                                                                                                                                            |
| `lootpool.populate` | `populate`                                                                                                                                                                          |
| `lootpool.spawn`    | `spawn`                                                                                                                                                                             |
| `lootpool.fill`     | `fill`                                                                                                                                                                              |
| `lootpool.project`  | `project`                                                                                                                                                                           |
| `lootpool.reload`   | `reload`                                                                                                                                                                            |
| `lootpool.*`        | All of the above                                                                                                                                                                    |

## When CommandAPI is missing

The command is registered through [CommandAPI](https://commandapi.jorel.dev), a soft dependency. Without it LootPool logs `CommandAPI not found, commands are not registered` at startup and runs without `/lootpool`. Pools on disk still load and still work through AbstractMenus and the API.
