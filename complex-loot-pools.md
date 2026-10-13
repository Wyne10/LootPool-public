---
description: >-
  Rolls, item modifiers and conditions, in the shape of a vanilla loot table:
  how a complex pool is built, addressed by path, and edited.
---

# Complex loot pools

Every other pool type comes down to one weighted list. That makes composition impossible to state exactly — you can nudge probabilities with weights, but you can't say _"3 to 5 commons, 1 tool, and exactly 1 quest item"_ — and it makes variation expensive, because every enchantment, damage value and attribute has to be baked into the item of a separate entry.

A **complex pool** borrows the shape of a [vanilla loot table](https://minecraft.wiki/w/Loot_table) to fix both:

* **Rolls** — an ordered list of groups, each with its own roll count and its own weighted entries. Composition is stated, not approximated.
* **Item modifiers** — transformations applied to an item _after_ it has been rolled. One sword template plus an [enchantment pool](enchantment-pools.md) becomes a spread of differently enchanted swords.
* **Conditions** — predicates that gate the whole pool, one roll, one entry, or one modifier.

```
/lootpool complex create dungeon_chest
```

## The shape

```
ComplexLootPool
├── conditions[]          gate the whole pool
├── modifiers[]           applied to every item the pool produces, last
└── rolls[]
    ├── minRolls / maxRolls
    ├── conditions[]      gate this roll
    ├── modifiers[]       applied to every item this roll produces
    └── entries[]
        ├── weight
        ├── conditions[]  gate this entry as a candidate
        ├── modifiers[]   applied first, to this entry's item only
        └── an item, a reference to another pool, or nothing
```

## How a roll runs

1. If any of the **pool's** conditions fails, the pool produces nothing.
2. Each **roll** is evaluated in order. A roll whose conditions fail is skipped.
3. The roll draws a count between `minRolls` and `maxRolls`, inclusive. Order doesn't matter and negative numbers are clamped to 0, exactly as in a [roll pool](pool-types.md#roll).
4. For each draw, the entries whose conditions pass are collected and one is picked by weight. An entry with weight 0 never comes up.
5. The picked entry produces an item, then **entry modifiers, roll modifiers and pool modifiers are applied in that order**. Any one of them may discard the item.

The pool produces however many items the rolls yield. Nothing is padded: a command that asks for 27 slots and gets 4 items puts 4 items in. Asking for fewer than the rolls produce truncates.

{% hint style="info" %}
`/lootpool info` on a complex pool shows its tree, and every line is clickable — clicking fills in a command already addressed at that node. It's the fastest way to find the [path](complex-loot-pools.md#addressing-by-path) you need. `/lootpool info <key> [amount] [sort] [depth]` takes a depth of 1 to 3, 3 by default.
{% endhint %}

## Entries

| Entry | Holds                              | Added with                          |
| ----- | ---------------------------------- | ----------------------------------- |
| Item  | An item, a weight, an amount range | `/lootpool complex entry add item`  |
| Pool  | The key of another pool, a weight  | `/lootpool complex entry add pool`  |
| Empty | A weight only                      | `/lootpool complex entry add empty` |

An **item entry** is the same `Loot` record the [loot editor](the-loot-editor.md) builds, so it keeps the item's full name, lore, enchantments and NBT, and rolls its stack size between its own minimum and maximum.

A **pool entry** hands the draw to another pool of any type, resolved by key on every roll. That's what makes _"one draw from `tools`, five draws from `resources`"_ two rolls of one entry each, and what lets you keep item categories in ordinary basic pools and compose them here. A key that no longer resolves is skipped, like everywhere else in the plugin.

An **empty entry** is the "nothing dropped" slot — the complex pool's version of the editor's **Nothing** item. Note that it is _not_ the same as discarding: an empty entry consumes the draw and produces nothing, while a [discard modifier](complex-loot-pools.md#item-modifiers) removes an item another entry already produced.

{% hint style="warning" %}
Empty entries can't hold modifiers — there is no item to modify. They can hold conditions.
{% endhint %}

## Item modifiers

A modifier takes the item that was rolled and returns a changed one, or nothing at all. Attach one to an entry, a roll, or the pool itself; they apply innermost first.

| Modifier     | Does                                                                | Vanilla equivalent |
| ------------ | ------------------------------------------------------------------- | ------------------ |
| `enchant`    | Draws enchantments from an [enchantment pool](enchantment-pools.md) | `enchant_randomly` |
| `damage`     | Wears the item down by a random fraction of its durability          | `set_damage`       |
| `attributes` | Adds attribute modifiers, each with its own rolled amount           | `set_attributes`   |
| `name`       | Sets the display name                                               | `set_name`         |
| `lore`       | Appends to, or replaces, the lore                                   | `set_lore`         |
| `amount`     | Sets the stack size to a fresh roll between a minimum and a maximum | `set_count`        |
| `limit`      | Clamps the stack size into a range without re-rolling it            | `limit_count`      |
| `type`       | Replaces the material, keeping the stack size                       | `set_item`         |
| `smelt`      | Smelts the item as a furnace would                                  | `furnace_smelt`    |
| `discard`    | Drops the item from the loot entirely                               | `discard`          |

```
/lootpool complex modifier add enchant dungeon_chest 0.1 sword_enchants 1 3 true
/lootpool complex modifier add damage dungeon_chest 0 0.1 0.6
/lootpool complex modifier add name dungeon_chest 0.1 [gold]Dungeon Blade
```

* `enchant` takes an enchantment pool key, a minimum and maximum number of enchantments, and `onlyCompatible`. With `onlyCompatible: true` an enchantment is skipped when it can't go on the item or conflicts with one already drawn; with `false` anything in the pool can land on anything. Levels ignore vanilla limits either way. A `BOOK` is turned into an `ENCHANTED_BOOK`, and books get stored enchantments rather than applied ones.
* `damage` takes a fraction of maximum durability: `0` leaves the item pristine, `1` leaves it one hit from breaking. It does nothing to an item that has no durability.
* `type` and `smelt` change the material but keep the stack size, so `smelt` is how one mob-drop entry yields cooked food or smelted ore without a second entry for it. Meta the new material can't carry is dropped, so put `type` before the modifiers that decorate the item.
* `discard` produces nothing. The draw still happened, so this is not the same as an entry that yields air, and it short-circuits: modifiers after it never run.
* `name` and `lore` text uses the same markup as the [language files](configuration.md#language-files), so `[gold]`, `[italic:off]` and the rest all work.

### Making a modifier conditional

Any modifier can be wrapped so that it only applies when its own conditions pass:

```
/lootpool complex modifier conditional dungeon_chest 0.1 0 true
/lootpool complex condition add chance dungeon_chest 0.1.m0 0.25
```

The wrapper keeps the modifier's place in the list, so the order it applies in doesn't change. Pass `false` to unwrap it again, which discards the conditions it was carrying.

## Conditions

| Condition     | Passes when…                                              | Needs          |
| ------------- | --------------------------------------------------------- | -------------- |
| `chance`      | A roll of the dice comes in under a fixed probability     | nothing        |
| `permission`  | The player holds a permission node                        | a player       |
| `world`       | The loot is generated in one of the named worlds          | a location     |
| `biome`       | The loot is generated in one of the named biomes          | a location     |
| `time`        | The world's time of day is inside an inclusive tick range | a location     |
| `weather`     | The world's weather matches                               | a location     |
| `hasloot`     | The player already carries loot from another pool         | a player       |
| `placeholder` | A PlaceholderAPI expression compares as stated            | PlaceholderAPI |
| `allof`       | Every child condition passes                              | —              |
| `anyof`       | At least one child condition passes                       | —              |

```
/lootpool complex condition add chance dungeon_chest pool 0.8
/lootpool complex condition add time dungeon_chest 0 13000 23000
/lootpool complex condition add biome dungeon_chest 1 desert badlands
```

* `weather` takes `raining` and `thundering`, and either may be left out to mean "don't care". A condition with both left out passes in any weather.
* `hasloot` names a pool and a mode: `any` passes once the player has any one of that pool's items, `all` only when they have every distinct item it can produce. `matchMeta` decides whether an item has to match exactly — name, lore, enchantments and NBT — or just by material.
* `placeholder` compares two PlaceholderAPI expressions with `==`, `!=`, `contains`, `>`, `>=`, `<` or `<=`. Both sides are expanded, so a literal value works as the right-hand side.
* `allof` and `anyof` are created empty and then filled through their own path. An empty `allof` passes; an empty `anyof` fails.

Any condition can be negated in place, which wraps it rather than replacing it:

```
/lootpool complex condition invert dungeon_chest pool 0 true
```

{% hint style="warning" %}
A condition that needs something the roll doesn't carry **fails closed**. `/lootpool fill` at a location has no player, so a `permission` condition there never passes; a pool rolled by another plugin through the context-free API has neither, so `world`, `biome`, `time` and `weather` don't pass either. Only `chance`, `allof` and `anyof` behave the same on every code path. See [Letting the pool roll itself](commands-and-permissions.md#letting-the-pool-roll-itself) for which commands supply what.
{% endhint %}

## Addressing by path

Rolls, entries, modifiers and conditions have no keys — they're addressed by position. Every `modifier` and `condition` command takes one dot-separated **path** naming the thing that _owns_ the list, followed by an index into that list.

| Path        | Owner                                                         |
| ----------- | ------------------------------------------------------------- |
| `pool`      | The pool's own modifiers and conditions                       |
| `0`         | Roll 0                                                        |
| `0.2`       | Entry 2 of roll 0                                             |
| `0.c1`      | The children of condition 1 of roll 0 — an `allof` or `anyof` |
| `0.2.m0`    | The conditions of modifier 0 of entry 2 of roll 0             |
| `0.2.m0.c1` | …and so on, arbitrarily deep                                  |

A bare number is a roll index at the first level and an entry index at the second. `c<i>` descends into a condition group, `m<i>` into a conditional modifier. Paths tab-complete with a description of each node, so you rarely type one from memory.

{% hint style="info" %}
Indexes shift when you remove something. `condition remove <key> <path> 1` renumbers everything after it, so re-read `/lootpool info` between edits rather than firing a prepared batch of commands.
{% endhint %}

## Editing in a GUI

```
/lootpool complex edit dungeon_chest
```

The first screen lists the pool's rolls; the bottom row pages through them, opens the pool's own modifiers and conditions, changes the step that clicks nudge numbers by, and closes the editor. Press `F` on a roll to open its entries, and `F` again on an entry to reach its own modifiers and conditions — every list in the tree is the same screen, so the navigation is uniform all the way down. The **Back** button returns one level; closing from any depth writes the whole pool once. Right-clicking the **Close** button throws the session away instead.

Text fields — a display name, a permission node, a lore line — are read-only in the GUI. Clicking one sends you a chat message with the command already filled in; press `T` and it's waiting on your prompt. Everything else is click-driven: left and right click nudge numbers, `F` opens, `Q` removes, shift-click reorders.

## On disk

```yaml
dungeon_chest:
  ==: ComplexLootPool
  key: dungeon_chest
  rolls:
  - ==: LootRoll
    minRolls: 3
    maxRolls: 5
    entries:
    - ==: PoolEntry
      pool: resources-common
      weight: 1
      conditions: []
      modifiers: []
    modifiers: []
    conditions: []
  - ==: LootRoll
    minRolls: 1
    maxRolls: 1
    entries:
    - ==: ItemEntry
      loot:
        ==: Loot
        item:
          ==: org.bukkit.inventory.ItemStack
          v: 2586
          type: DIAMOND_SWORD
        weight: 10
        minAmount: 1
        maxAmount: 1
      conditions: []
      modifiers:
      - ==: EnchantModifier
        pool: sword_enchants
        minEnchants: 1
        maxEnchants: 3
        onlyCompatible: true
    modifiers:
    - ==: DamageModifier
      minFraction: 0.1
      maxFraction: 0.6
    conditions: []
  modifiers: []
  conditions:
  - ==: RandomChanceCondition
    chance: 0.8
```

## What a preview can't show

`/lootpool info`, the [AbstractMenus catalog](abstractmenus-integration.md) and the API's `getLootList()` all flatten a complex pool into a plain list of every entry across every roll, with pool entries resolved up to four levels deep. That list is for inspection only: **roll counts, conditions and modifiers are absent from it**, so a preview of a pool that enchants its swords shows unenchanted swords.

Every command that hands out loot runs the real pipeline instead — including `give`, `drop` and `insert`, which draw from the flat list for other pool types. Their `amount` and `unique` options have no effect on a complex pool, because the pool decides its own stack sizes and its own item count. `project` is the exception: it is a preview by definition, so it gives one of every entry in the flattened list.
