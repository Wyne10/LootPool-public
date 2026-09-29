---
description: >-
  The chest GUI behind every basic pool: adding items, setting weights and
  amounts with clicks, the Nothing slot, and when changes are saved.
---

# The loot editor

The editor is a six-row chest titled with the pool's key. `/lootpool pool create` opens it empty, and `modify`, `clone`, `merge`, `weight`, `amount` and `flatten` open it with the pool's entries already in it. See [Commands and permissions](commands-and-permissions.md#building-and-editing-pools).

## Adding items

* Click an empty slot while holding an item, or
* Shift-click an item in your own inventory.

The editor stores a copy of the item with everything on it: name, lore, enchantments, NBT. The item you're holding stays with you. The new entry starts at weight 1, and its minimum and maximum amount both start at the stack size you added.

## Controls

Hover over an entry and:

| Input                       | Effect                                                                                        |
| --------------------------- | --------------------------------------------------------------------------------------------- |
| Left / right click          | Weight down / up                                                                              |
| Shift + left / right click  | Minimum amount down / up                                                                      |
| Hotbar key `1` / `2`        | Maximum amount down / up                                                                      |
| `F` (swap hands)            | Switch the step between 1 and 10, for every control above                                     |
| `Q` (drop)                  | Remove the entry                                                                              |
| Click while holding an item | Replace the entry's item and keep its weight and amounts. The old item goes onto your cursor. |

The editor holds 54 entries per page. Click **outside** the window to change pages: left click for the previous page, right click for the next.

Each entry's name ends in its current chance in aqua, for example `(12.50)`, and the stack you see is its maximum amount. The lines added to its lore are the `gui-loot` message from the [language file](configuration.md#language-files).

Amounts stay between 1 and the item's maximum stack size, and wrap around: lowering past 1 jumps to the maximum, raising past the maximum jumps to 1. The minimum can't go above the maximum. Raise the minimum past it and the maximum moves up too, and the reverse. Weight never goes below 0, and an entry at weight 0 never drops.

## The Nothing slot

The first slot of the first page always holds a gray glass pane called **Nothing**. Its weight is the chance that a draw produces nothing at all. It starts at 0. Change it with left and right click like any other entry, and press `Q` on it to reset its weight to 0. Its amounts don't matter, and it can't be removed or replaced.

When you save with Nothing at weight 0, the pool has no empty entry.

## Saving

Changes are saved when you **close the editor**, not before. Closing writes the pool to its file and replaces the loaded copy, so the next roll already uses it.

There is no cancel. Closing always saves, with one exception: if the editor holds nothing besides the Nothing slot, it closes without saving. Closing an empty `create` discards it. Closing a `modify` after removing every item leaves the old pool as it was.

The commands that transform a pool (`merge`, `weight`, `amount`, `flatten`, and `clone`) change nothing by themselves. They open the editor with the result already applied, and it's saved when you close it.
