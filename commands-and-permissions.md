---
description: >-
  Every /lootpool subcommand: building and editing pools, getting loot out of
  them, and the permission behind each one.
---

# Commands and permissions

Everything lives under `/lootpool`. Run `/lootpool help`, `/lootpool pool help` or `/lootpool item help` in-game for a clickable list, and `/lootpool help <command>` for the full description of one command.

Pool keys tab-complete wherever a command expects one.

## Building and editing pools

| Command                                                        | Permission        | What it does                                                                                                        |
| -------------------------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------------------------------- |
| `/lootpool pool create <key>`                                  | `lootpool.create` | Opens [the editor](the-loot-editor.md) for a new basic pool                                                         |
| `/lootpool pool modify <key>`                                  | `lootpool.modify` | Opens the editor on an existing basic pool                                                                          |
| `/lootpool pool clone <key> <newKey>`                          | `lootpool.create` | Opens the editor with a copy of a basic pool under a new key                                                        |
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
| `/lootpool remove <key>`                                       | `lootpool.remove` | Deletes a pool of any type, and its file                                                                            |
| `/lootpool info <key> [amount] [sort]`                         | `lootpool.info`   | Lists a pool's entries in chat with their amounts, weights and chances                                              |
| `/lootpool reload`                                             | `lootpool.reload` | Re-reads the config, language files and every pool from disk                                                        |

The `pool` commands and `merge` work only on basic pools, but `merge` and `flatten` accept a source of any type. Only its entries are taken; a composite's weighted pick or a roll pool's range doesn't carry over, and a vanilla pool contributes one sample roll. The source pools themselves stay as they were.

`info` shows the first 15 entries unless you pass `amount`, and sorts them by `weight` unless you pass `name`, `min-amount` or `max-amount`. For a multi or composite pool it lists the pools it's built from, each with its share of the rolls.

If you try to create a pool that already exists and you hold `lootpool.modify`, the error comes with a clickable **Modify** link.

### Operations

`weight`, `amount` and `flatten` take an operation: an optional operator followed by a whole number.

| Operation | Result                   |
| --------- | ------------------------ |
| `+5`      | Add 5                    |
| `-1`      | Subtract 1               |
| `*1000`   | Multiply by 1000         |
| `/3`      | Divide by 3              |
| `**2`     | Raise to the power of 2  |
| `=10`     | Set to 10                |
| `10`      | Set to 10, same as `=10` |

`flatten` takes one operation per pool: `/lootpool pool flatten ores iron_ores:*3 gold_ores:+0 diamond_ores:=1`.

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

### Letting the pool roll itself

`fill`, `populate`, `spawn` and `preview` hand the work to the pool. A composite picks one child pool, a roll pool uses its range, a snapshot puts items in their saved slots, and a vanilla pool runs the vanilla table.

| Command                                     | Permission          | What it does                                                                                                                         |
| ------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `/lootpool fill <key> <x y z> [random]`     | `lootpool.fill`     | Fills the container at a block location. What doesn't fit is lost.                                                                   |
| `/lootpool populate <key> <player> [slots]` | `lootpool.populate` | Fills a player's inventory. What doesn't fit is dropped at their feet.                                                               |
| `/lootpool spawn <key> <x y z> [slots]`     | `lootpool.spawn`    | Drops the loot on the ground. A roll pool uses its range when `slots` is left out; **any other pool drops nothing without `slots`**. |
| `/lootpool preview <key> [rows]`            | `lootpool.info`     | Opens a chest of 1 to 6 rows (3 by default) filled from the pool                                                                     |

Without a slot count, a basic, multi or composite pool makes **one draw per empty slot** of the target, so `fill` fills the whole chest and `populate` fills the player's whole inventory, armor and off-hand slots included. For a sensible amount of loot, point these commands at a [roll pool](pool-types.md#roll) or pass `slots`.

{% hint style="danger" %}
The preview is an ordinary chest, so the items in it are real and can be taken out. Give `lootpool.info` only to staff.
{% endhint %}

### From the console and command blocks

Everything in [Getting loot out of a pool](commands-and-permissions.md#getting-loot-out-of-a-pool) except `preview`, plus `info`, `remove`, `include`, `compose`, `roll`, `register` and `reload`, works from the console and command blocks. That makes these commands handy for rewards, quests and scheduled refills, and lets a setup script register vanilla tables. The rest need a player: every `pool` and `item` command, plus `snapshot` and `preview`.

The "Dropped N items" confirmation goes to players only, so console and command-block runs are silent.

## Permissions

Every permission defaults to operators.

| Permission          | Grants                                                                                                           |
| ------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `lootpool.create`   | `pool create`, `pool clone`, `pool flatten`, `item create`, `include`, `compose`, `roll`, `snapshot`, `register` |
| `lootpool.modify`   | `pool modify`, `pool merge`, `pool weight`, `pool amount`, `item modify`                                         |
| `lootpool.remove`   | `remove`                                                                                                         |
| `lootpool.info`     | `info`, `preview`                                                                                                |
| `lootpool.give`     | `give`                                                                                                           |
| `lootpool.drop`     | `drop`                                                                                                           |
| `lootpool.insert`   | `insert`                                                                                                         |
| `lootpool.populate` | `populate`                                                                                                       |
| `lootpool.spawn`    | `spawn`                                                                                                          |
| `lootpool.fill`     | `fill`                                                                                                           |
| `lootpool.project`  | `project`                                                                                                        |
| `lootpool.reload`   | `reload`                                                                                                         |
| `lootpool.*`        | All of the above                                                                                                 |

## When CommandAPI is missing

The command is registered through [CommandAPI](https://commandapi.jorel.dev), a soft dependency. Without it LootPool logs `CommandAPI not found, commands are not registered` at startup and runs without `/lootpool`. Pools on disk still load and still work through AbstractMenus and the API.
