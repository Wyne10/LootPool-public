---
description: >-
  Show a random roll from a pool on any menu item, or list a pool's entries in a
  generated menu with the LOOTPOOL catalog.
---

# AbstractMenus integration

When [AbstractMenus](https://abstractmenus.github.io/docs/en/) is installed, LootPool registers two things with it: the `lootPool` item property and the `LOOTPOOL` catalog. Without AbstractMenus it logs `AbstractMenus not found, catalog and property are not registered` and carries on.

## The `lootPool` item property

Turns a menu item into a fresh roll from a pool (material, meta and amount) each time the item is drawn:

```hocon
items: [
  {
    slot: 13
    material: CHEST
    lootPool: "daily_reward"
  }
]
```

The declared `material` is only a fallback. It shows as-is if the pool doesn't exist.

The item is for display only. It isn't handed out, and running `/lootpool give daily_reward …` from a click action makes a new roll that won't match what was shown.

## The `LOOTPOOL` catalog

A [generated menu](https://abstractmenus.github.io/docs/en/advanced/generation/) with the `LOOTPOOL` catalog gets one element per entry of the pool. That makes it a ready-made "possible drops" screen:

```hocon
title: "Dungeon loot"
size: 6
catalog {
  type: LOOTPOOL
  lootPool: "dungeon_loot"
}
matrix {
  cells: [
    "xxxxxxxxx",
    "xxxxxxxxx",
    "xxxxxxxxx",
    "xxxxxxxxx",
    "xxxxxxxxx",
    "_________"
  ]
  templates {
    "x" {
      material: "%ctg_material%"
      name: "%ctg_name-legacy%"
      lore: [
        "&7Weight: &e%ctg_weight%",
        "&7Amount: &e%ctg_minAmount%-%ctg_maxAmount%"
      ]
    }
  }
}
```

Entries appear in the pool's own order. A multi or composite pool shows every entry of every pool it's built from. A vanilla pool shows a fresh sample roll each time the menu opens.

### Catalog placeholders

Inside a template, each placeholder is written `%ctg_<name>%`:

| Placeholder    | Value                                                                                        |
| -------------- | -------------------------------------------------------------------------------------------- |
| `material`     | The item's material, e.g. `DIAMOND_SWORD`                                                    |
| `name`         | The item's display name, or its translated vanilla name, in the plugin's `serializer` format |
| `lore_<n>`     | Line `n` of the item's lore, counting from 1; empty if there is no such line                 |
| `weight`       | The entry's weight                                                                           |
| `minAmount`    | The entry's minimum amount                                                                   |
| `maxAmount`    | The entry's maximum amount                                                                   |
| `randomAmount` | A random amount between the two, re-rolled each time it's shown                              |
| `serialized`   | The whole item as a Base64-encoded Bukkit object stream                                      |

`name` and `lore` come in other formats too. Add a suffix to the name: `name-legacy` for `&` color codes, `name-parsed` for `§` codes, `name-mm` for MiniMessage, `name-plain` for plain text, `name-gson` for JSON. Lore takes the same suffixes before the line number: `lore-legacy_2`. AbstractMenus reads `&` codes, so `-legacy` is usually the one you want.
