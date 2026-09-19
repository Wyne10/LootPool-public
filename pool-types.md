---
description: >-
  Seven kinds of pool, from a plain weighted list to pools of pools and wrapped
  vanilla tables: how each one rolls, and the command that creates it.
---

# Pool types

## How a roll works

Every pool comes down to a list of **loot entries**. An entry is an item (with its full name, lore, enchantments and NBT), a **weight**, and an inclusive **amount range**.

Picking an entry is a weighted draw. An entry's chance is its weight divided by the total weight of the pool: with weights 6, 3 and 1, the three entries come up 60%, 30% and 10% of the time. An entry with weight 0 never comes up. Once an entry is picked, its stack size is rolled evenly between its minimum and maximum amount.

A pool can also hold an **empty entry**: an air item with a weight of its own. Drawing it produces nothing, which is how you say "30% of the time this slot stays empty". The editor shows it as the **Nothing** item.

Throughout the plugin, **slots** means _number of draws_. Rolling a pool into 5 slots makes 5 independent draws, so the same entry can come up more than once.

## Choosing a type

| Type                                   | Holds                                   | Pick it when you want…                                          | Created with            |
| -------------------------------------- | --------------------------------------- | --------------------------------------------------------------- | ----------------------- |
| [Basic](pool-types.md#basic)           | A list of entries                       | An ordinary weighted loot table                                 | `/lootpool pool create` |
| [Keyed loot](pool-types.md#keyed-loot) | One entry                               | A single item you can reuse by name                             | `/lootpool item create` |
| [Multi](pool-types.md#multi)           | Keys of other pools                     | Several pools merged into one table                             | `/lootpool include`     |
| [Composite](pool-types.md#composite)   | Keys of other pools, each with a weight | Whole pools competing: "80% the common table, 20% the rare one" | `/lootpool compose`     |
| [Roll](pool-types.md#roll)             | The key of one pool, plus a roll range  | A random number of items each time                              | `/lootpool roll`        |
| [Snapshot](pool-types.md#snapshot)     | Entries pinned to slot numbers          | An exact container layout, replayed                             | `/lootpool snapshot`    |
| [Vanilla](pool-types.md#vanilla)       | A vanilla loot table path               | A Minecraft table such as `chests/simple_dungeon` as a pool     | `/lootpool register`    |

All pools share one registry, so a key is unique across every type: you can't have a basic pool and a keyed loot both called `diamond`.

{% hint style="info" %}
Keys may contain only lowercase letters, digits, `_`, `-` and `.`, because each one becomes part of a Minecraft `NamespacedKey` (`lootpool:<key>`). A command given any other key, such as `Dungeon`, refuses it before creating anything.
{% endhint %}

## Basic

A plain list of entries that you build in [the loot editor](the-loot-editor.md). It's the only type that `/lootpool pool modify`, `clone`, `merge`, `weight` and `amount` work on.

```
/lootpool pool create common_ores
```

Rolling a basic pool into an inventory without a slot count makes **one draw per empty slot**, so it fills the whole inventory. Give a slot count, or wrap the pool in a [Roll](pool-types.md#roll) pool, when you want fewer items.

## Keyed loot

A pool of exactly one entry, taken from the item in your main hand:

```
/lootpool item create emerald_stack 16 32
```

The arguments after the key are the minimum amount, the maximum amount (both 1–64), and the weight. The minimum defaults to the size of the stack you're holding. The maximum defaults to that stack size or the minimum, whichever is greater. The weight defaults to 1. Holding 8 emeralds, `/lootpool item create emeralds` gives exactly 8, and `/lootpool item create emeralds 4` gives 4 to 8.

The weight doesn't matter when you roll the keyed loot itself, since there's nothing to compete with. It matters once the entry lands in a larger table through [Multi](pool-types.md#multi), `pool merge` or `pool flatten`.

## Multi

Merges the entries of the pools it names into one flat list, then rolls that list:

```
/lootpool include ores iron_ores gold_ores diamond_ores
```

Entries from all three pools compete directly, so a pool with more entries or heavier weights takes a proportionally bigger share.

## Composite

Names other pools, each with a weight of its own. Every use first picks **one** of those pools by weight, then lets that pool do the whole roll:

```
/lootpool compose chest_loot common:80 rare:20
```

Eighty percent of the chests filled from `chest_loot` get only common loot, and 20% get only rare loot. Contrast that with [Multi](pool-types.md#multi), where a single chest can hold a mix of both.

{% hint style="info" %}
Multi and composite pools look the pools they name up again on every roll, so editing `rare` changes `chest_loot` immediately. A name that no longer exists is skipped without an error. Removing a pool doesn't break the pools built on it, but it doesn't warn you either.
{% endhint %}

## Roll

Wraps another pool and adds a roll count. Whenever the roll pool is used without an explicit slot count, it makes a random number of draws between `minRolls` and `maxRolls`:

```
/lootpool roll dungeon_chest dungeon_loot 3 6
```

Filling a chest from `dungeon_chest` puts 3 to 6 items from `dungeon_loot` into it, where filling from `dungeon_loot` directly would fill every empty slot. Leave out `maxRolls` to always make exactly `minRolls` draws, or leave out both for a single draw.

A roll pool wraps any type. Over a [Composite](pool-types.md#composite) pool it picks one child pool and makes all 3 to 6 draws from that one. When a command does pass a slot count, the roll pool hands it straight to the wrapped pool and ignores its own range.

## Snapshot

Captures an inventory, either your own or a container you point at, and remembers every item in its exact slot and amount:

```
/lootpool snapshot starter_kit
/lootpool snapshot vault_layout 120 64 -35
```

Filling a container or a player inventory from a snapshot puts each item back into its original slot. An item whose slot is taken, or doesn't exist in the target, is left over (`populate` drops leftovers at the player's feet). Snapshotting your own inventory captures armor and off-hand too, and populating a player from it puts them back.

Random placement (`fill … true`) ignores the saved slots. It draws from the entries like a basic pool, where every entry has weight 1, and scatters the results across empty slots.

## Vanilla

Wraps a vanilla loot table by its path:

```
/lootpool register dungeon minecraft:chests/simple_dungeon
```

Each use runs the vanilla table as if it were generated at the spawn point of the server's first world, with no looter, killer or luck. The vanilla table decides how many items it drops, so slot counts are ignored, and random placement falls back to vanilla's own placement.

Because the table re-rolls every time, the pool has no fixed list of entries. `/lootpool info`, the AbstractMenus catalog and every command that reads the entry list see a fresh sample roll instead.

## Where pools are stored

Each pool lives in `plugins/LootPool/lootpool/<key>.yml`, written the moment it's created or saved, as a Bukkit-serialized object:

```yaml
common_ores:
  ==: me.wyne.lootpool.api.BasicLootPool
  key: common_ores
  '0':
    ==: me.wyne.lootpool.api.Loot
    item:
      ==: org.bukkit.inventory.ItemStack
      v: 2586
      type: IRON_INGOT
    weight: 6
    minAmount: 1
    maxAmount: 4
  '1':
    ==: me.wyne.lootpool.api.Loot
    item:
      ==: org.bukkit.inventory.ItemStack
      v: 2586
      type: AIR
    weight: 2
    minAmount: 1
    maxAmount: 1
```

The second entry is an empty entry: air with weight 2.

The plugin writes these files, but you can copy them between servers, or edit one and run `/lootpool reload`. If you do, keep the file name, the top-level key and the `key:` field the same. `/lootpool remove <key>` deletes the file along with the pool.
