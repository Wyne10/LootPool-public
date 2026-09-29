---
description: >-
  Worked recipes: named items, item categories, kits, wrapped datapack tables,
  scattered chest fills, and loot that varies by rank, biome or time.
---

# Common setups

Each of these is a complete, working arrangement. They're meant to be read for the pattern rather than copied literally — most of them are two or three commands and a reason.

## Share one named item everywhere

Give an item you built by hand a name, so every table can reference it and one edit updates all of them.

```
# holding the item
/lootpool item create boss_key 1 1 1
```

`boss_key` is now a pool like any other. Reference it from a [multi](pool-types.md#multi) pool, drop it into a [complex](complex-loot-pools.md) roll as a pool entry, drag it into the [editor](the-loot-editor.md), or hand it out directly:

```
/lootpool give boss_key @p
/lootpool complex entry add pool dungeon_chest 2 boss_key 1
```

`item modify` replaces the item while keeping the key, so re-holding a retouched version and running it updates every table that references the key at once. Baking the same item into five basic pools instead means editing it five times.

## Item categories, composed

The most useful shape in the plugin: keep flat [basic pools](pool-types.md#basic) as **categories**, and let a [complex pool](complex-loot-pools.md) decide how many of each a chest gets.

```
/lootpool pool create resources_common
/lootpool pool create tools_common
/lootpool pool create armor_rare
```

Fill each in the editor. Then state the composition:

```
/lootpool complex create dungeon_chest
/lootpool complex roll add dungeon_chest 4 7
/lootpool complex entry add pool dungeon_chest 0 resources_common
/lootpool complex roll add dungeon_chest 1 2
/lootpool complex entry add pool dungeon_chest 1 tools_common
/lootpool complex roll add dungeon_chest 0 1
/lootpool complex entry add pool dungeon_chest 2 armor_rare
```

Every `dungeon_chest` now holds 4–7 resources, 1–2 tools and sometimes a piece of rare armour. The categories stay editable on their own, in the GUI, and every chest built on them changes at once.

Rolls with several entries let a roll pick between categories:

```
/lootpool complex entry add pool dungeon_chest 2 armor_rare 1
/lootpool complex entry add pool dungeon_chest 2 armor_common 9
```

Now the third roll is 90% common armour, 10% rare.

## Enchanted gear without an entry per combination

Put one plain sword in a pool and let an [enchantment pool](enchantment-pools.md) do the variation.

```
/lootpool enchant create sword_enchants
/lootpool enchant entry add sword_enchants sharpness 10 1 5
/lootpool enchant entry add sword_enchants looting 4 1 3
/lootpool enchant entry add sword_enchants fire_aspect 2 1 2

/lootpool complex modifier add enchant dungeon_chest 1 sword_enchants 1 3 true
```

Attached to roll 1, that enchants everything the tools roll produces with one to three compatible enchantments. Add a `damage` modifier next to it and the tools come out worn as well:

```
/lootpool complex modifier add damage dungeon_chest 1 0.2 0.7
```

Two lines replace what would otherwise be dozens of hand-built entries, and the spread is different in every chest.

## A starter kit

[Snapshot](pool-types.md#snapshot) pools remember exact slots, which is what makes them kits rather than loot.

```
# set your inventory up exactly as the kit should arrive, then
/lootpool snapshot starter_kit
/lootpool populate starter_kit @p
```

Armor and off-hand are captured too, and populating a player puts everything back where it was. Anything that doesn't fit is dropped at their feet. For a shop or a rank reward, hand the same snapshot out from a command block or a rewards plugin.

## Wrap a datapack or vanilla table

```
/lootpool register simple_dungeon minecraft:chests/simple_dungeon
/lootpool register relic mypack:chests/relic_stash
```

A [vanilla pool](pool-types.md#vanilla) runs the real table every time, so it keeps whatever the datapack does. From there it's an ordinary pool key, which means you can build on top of it:

```
/lootpool complex create dungeon_plus
/lootpool complex roll add dungeon_plus 1 1
/lootpool complex entry add pool dungeon_plus 0 simple_dungeon
/lootpool complex roll add dungeon_plus 0 1
/lootpool complex entry add pool dungeon_plus 1 my_server_relics
```

`dungeon_plus` gives vanilla's dungeon loot plus, half the time, one of your own relics — without editing a datapack.

{% hint style="warning" %}
Don't use `merge` or `flatten` on a vanilla pool unless you mean to freeze it. Those commands read the entry list, and a vanilla pool's entry list is one sample roll, so you'd bake a single random result into a basic pool. Referencing it keeps it live.
{% endhint %}

## Fill a chest so it looks scattered

`/lootpool fill` places items into empty slots in order, so a chest comes out packed from the top-left. Passing `random` fixes that, but you can't always pass it — an AbstractMenus button, a rewards plugin or a command block may not offer the option.

Add an empty entry instead. The **Nothing** slot in the [editor](the-loot-editor.md#the-nothing-slot) raises the chance that a draw produces nothing, and a draw that produces nothing leaves its slot empty:

```
# in the editor, right-click Nothing until its weight is around two-thirds of the total
```

Filling a 27-slot chest now makes 27 draws and lands roughly nine items, spread out. The higher the Nothing weight, the emptier and more natural the chest. It also gives you a knob the [roll pool](pool-types.md#roll) doesn't: density rather than count.

## Tiered chests

Weight whole tables against each other with a [composite](pool-types.md#composite) pool, then put a count on top:

```
/lootpool compose loot_tier common:80 rare:18 legendary:2
/lootpool roll chest loot_tier 3 6
/lootpool fill chest 120 64 -35
```

A chest is 80% all-common, 18% all-rare, 2% all-legendary — never a mix, which is what makes finding a legendary chest feel like an event. Swap `compose` for `include` if you'd rather every chest hold a mix.

## Loot that fits where it drops

[Conditions](complex-loot-pools.md#conditions) let one pool key serve every region.

```
/lootpool complex create region_loot
/lootpool complex roll add region_loot 1 1
/lootpool complex entry add pool region_loot 0 desert_relics
/lootpool complex condition add biome region_loot 0 desert badlands

/lootpool complex roll add region_loot 1 1
/lootpool complex entry add pool region_loot 1 tundra_relics
/lootpool complex condition add biome region_loot 1 snowy_tundra ice_spikes
```

Both rolls are evaluated, and only the one whose biome matches produces anything, so `/lootpool fill region_loot …` gives desert loot in a desert and tundra loot in a tundra. The same trick with `time` and `weather` gives you night-only or storm-only drops.

{% hint style="info" %}
Conditions can only check what the command handed them. `fill`, `drop`, `insert` and `spawn` know a location; `give` and `populate` know a player. See [what each command knows](commands-and-permissions.md#what-each-command-knows-about-the-roll).
{% endhint %}

## A bonus roll for a rank

```
# crate already has roll 0, the one everybody gets
/lootpool complex roll add crate 1 1
/lootpool complex entry add pool crate 1 vip_bonus
/lootpool complex condition add permission crate 1 myserver.vip
```

Rolled through `/lootpool give crate <player>`, the second roll only fires for players holding the node. A `placeholder` condition does the same against any PlaceholderAPI expression, which is how you'd gate on a level, a quest flag or a playtime.

## Stop handing out a duplicate

```
/lootpool complex condition add hasloot crate 0 collectibles any true
/lootpool complex condition invert crate 0 0 true
```

Roll 0 now only fires for a player carrying **none** of `collectibles`'s items — a first-time-only drop. `all` instead of `any` reads the other way: gate a reward behind having collected the whole set. `matchMeta` set to `true` compares items exactly, including name, lore and enchantments, so a renamed copy doesn't count; `false` compares by material only.

## Mob drops that come out cooked

```
/lootpool complex modifier add smelt cow_drops pool
```

Attached to the pool itself, `smelt` runs every item it produces through the furnace recipes, so one entry for raw beef also covers steak — and the same pool covers ores and their ingots. Combine it with a condition to make it a fire-aspect table:

```
/lootpool complex modifier conditional cow_drops pool 0 true
/lootpool complex condition add chance cow_drops pool.m0 0.25
```

## A preview players can look at

Register the pool with AbstractMenus and let players browse it instead of asking staff:

```hocon
title: "Dungeon loot"
size: 6
catalog { type: LOOTPOOL, lootPool: "dungeon_chest" }
```

See [AbstractMenus integration](abstractmenus-integration.md). Remember that a catalog shows the pool's **flattened** entry list, so a complex pool's roll counts and modifiers aren't visible in it — a sword that only ever drops enchanted is shown unenchanted. `/lootpool preview` has the same limit and hands out real items besides, which is why `lootpool.info` belongs to staff only.

## Refill a chest on a schedule

Everything in [Getting loot out of a pool](commands-and-permissions.md#getting-loot-out-of-a-pool) runs from the console and from command blocks, so a scheduler plugin needs no integration:

```
/lootpool fill arena_crate 120 64 -35 true
```

Clear the container first if you want a fresh chest — `fill` only uses slots that are already empty.
