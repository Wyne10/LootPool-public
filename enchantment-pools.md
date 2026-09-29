---
description: >-
  Named, reusable sets of weighted enchantments with level ranges, drawn from by
  the enchant modifier of a complex loot pool.
---

# Enchantment pools

An **enchantment pool** is a named list of enchantments, each with a weight and an inclusive level range. It is a registry of its own, separate from loot pools, and exists for one purpose: to be drawn from by the `enchant` [item modifier](complex-loot-pools.md#item-modifiers) of a [complex pool](complex-loot-pools.md).

That indirection is the point. Define `sword_enchants` once, reference it from every sword entry in every dungeon, and a single edit changes what every dungeon sword can roll.

```
/lootpool enchant create sword_enchants
/lootpool enchant entry add sword_enchants sharpness 10 1 5
/lootpool enchant entry add sword_enchants looting 4 1 3
/lootpool enchant entry add sword_enchants fire_aspect 2 1 2
/lootpool enchant entry add sword_enchants mending 1
```

Then point a modifier at it:

```
/lootpool complex modifier add enchant dungeon_chest 0.1 sword_enchants 1 3 true
```

## Commands

| Command                                                                              | Permission        | What it does                                                                 |
| ------------------------------------------------------------------------------------ | ----------------- | ---------------------------------------------------------------------------- |
| `/lootpool enchant create <key>`                                                     | `lootpool.create` | Creates an empty enchantment pool                                            |
| `/lootpool enchant clone <key> <newKey>`                                             | `lootpool.create` | Copies one under a new key                                                   |
| `/lootpool enchant remove <key>`                                                     | `lootpool.remove` | Deletes the pool and its file                                                |
| `/lootpool enchant info <key>`                                                       | `lootpool.info`   | Lists the entries with their levels and chances                              |
| `/lootpool enchant edit <key>`                                                       | `lootpool.modify` | Opens [the editor](enchantment-pools.md#the-editor)                          |
| `/lootpool enchant entry add <key> <enchantment> [weight] [minLevel] [maxLevel]`     | `lootpool.modify` | Adds or replaces an entry                                                    |
| `/lootpool enchant entry remove <key> <entry>`                                       | `lootpool.modify` | Removes the entry at an index                                                |
| `/lootpool enchant entry set <key> <entry> weight\|min-level\|max-level <operation>` | `lootpool.modify` | Applies an [operation](commands-and-permissions.md#operations) to one number |

Keys follow the same rule as loot pool keys — lowercase letters, digits, `_`, `-` and `.` only — but live in their own namespace, so an enchantment pool and a loot pool may share a key without clashing.

`entry add` defaults the weight to 1, the minimum level to 1, and the maximum level to the minimum. The level arguments suggest the enchantment's own vanilla range, but **higher values are accepted**: loot is enchanted ignoring vanilla level limits, so `sharpness 1 10 10` is a legitimate entry. Adding an enchantment that is already in the pool replaces its entry rather than duplicating it.

## How a draw works

The `enchant` modifier asks for a count between its `minEnchants` and `maxEnchants`, and the pool draws that many entries **without replacement** — weighted, so a weight of 10 against a weight of 1 comes up ten times as often, and an entry with weight 0 never comes up at all. Each drawn entry then rolls its own level from its range.

With `onlyCompatible` set to `true`, an entry is skipped when its enchantment can't go on the item or conflicts with one already drawn, which is why a draw can return fewer enchantments than asked for. With `false`, anything in the pool lands on anything — Sharpness on a helmet included.

Books are handled the way vanilla does: a `BOOK` becomes an `ENCHANTED_BOOK`, and an enchanted book receives _stored_ enchantments rather than active ones, so every enchantment is compatible with it.

{% hint style="info" %}
A modifier pointing at an enchantment pool that no longer exists leaves the item unchanged, the same way a missing pool key is skipped elsewhere in the plugin. Removing an enchantment pool doesn't break the pools that reference it, but it doesn't warn you either.
{% endhint %}

## The editor

```
/lootpool enchant edit sword_enchants
```

The editor shows one book per enchantment registered on the server, paged 45 to a screen. A gray name on a plain book is an enchantment the pool doesn't hold; **right-click it to raise its weight above 0** and it turns into an enchanted book, named with its chance in aqua and glowing at its maximum level. Under the name sits the enchantment's namespaced key, so two enchantments with the same display name stay distinguishable, and below that its vanilla maximum level for reference.

| Input                      | Effect                  |
| -------------------------- | ----------------------- |
| Left / right click         | Weight down / up        |
| Shift + left / right click | Minimum level down / up |
| Hotbar key `1` / `2`       | Maximum level down / up |
| `Q`                        | Reset the entry         |

The bottom row pages through the list, cycles the step between 1, 10 and 100, and closes the editor. Left-clicking **Close** saves; right-clicking throws the session away. Only entries with a weight above 0 are written, so dropping a weight to 0 is the same as removing the entry.

## On disk

One file per pool in `plugins/LootPool/enchantment/<key>.yml`:

```yaml
sword_enchants:
  ==: EnchantmentPool
  key: sword_enchants
  entries:
  - ==: EnchantmentEntry
    enchantment: minecraft:sharpness
    weight: 10
    minLevel: 1
    maxLevel: 5
  - ==: EnchantmentEntry
    enchantment: minecraft:looting
    weight: 4
    minLevel: 1
    maxLevel: 3
```

Enchantments are stored by namespaced key, so an entry naming an enchantment the server no longer has is dropped when the file loads rather than failing the whole pool. `/lootpool reload` re-reads these files along with everything else.

## From your plugin

The registry is published the same two ways the loot pool registry is:

```java
EnchantmentPoolProvider provider = LootPoolApi.getEnchantmentProvider();
// or
EnchantmentPoolProvider provider = Bukkit.getServicesManager()
        .getRegistration(EnchantmentPoolProvider.class)
        .getProvider();

provider.writeEnchantmentPool(new EnchantmentPool("sword_enchants", List.of(
        new EnchantmentEntry(Enchantment.DAMAGE_ALL, 10, 1, 5),
        new EnchantmentEntry(Enchantment.LOOT_BONUS_MOBS, 4, 1, 3)
)));
```

`addEnchantmentPool` registers in memory only; `writeEnchantmentPool` also persists. See [Using it from your plugin](using-it-from-your-plugin.md).
