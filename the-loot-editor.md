---
description: >-
  The chest GUI behind every basic pool: adding items, setting weights and
  amounts with clicks, the control row, and when changes are saved.
---

# The loot editor

The editor is a six-row chest titled with the pool's key. `/lootpool pool create` opens it empty, and `modify`, `merge`, `weight`, `amount` and `flatten` open it with the pool's entries already in it. See [Commands and permissions](commands-and-permissions.md#building-and-editing-pools).

The **top five rows hold the entries**, 45 to a page. The bottom row is a fixed control row and never holds loot.

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
| `Q` (drop)                  | Remove the entry                                                                              |
| Click while holding an item | Replace the entry's item and keep its weight and amounts. The old item goes onto your cursor. |

Each entry's name ends in its current chance in aqua, for example `(12.50)`, and the stack you see is its maximum amount. The lines added to its lore are the `gui-loot` message from the [language file](configuration.md#language-files).

Amounts stay between 1 and the item's maximum stack size, and wrap around: lowering past 1 jumps to the maximum, raising past the maximum jumps to 1. The minimum can't go above the maximum. Raise the minimum past it and the maximum moves up too, and the reverse. Weight never goes below 0, and an entry at weight 0 never drops.

## The control row

| Slot                | Button                                                                      |
| ------------------- | --------------------------------------------------------------------------- |
| First two           | **Previous** and **Next page**, shown only when there is more than one page |
| Middle              | **Close**                                                                   |
| Second from the end | **Step**                                                                    |
| Last                | **Nothing**                                                                 |

**Step** is how much one click changes a number by. Clicking it cycles 1 → 10 → 100 → 1, and it applies to every control above, in this editor and in every other one.

Clicking **outside** the window still changes pages, as it always has: left click for the previous page, right click for the next.

### The Nothing slot

The gray glass pane in the last control slot is **Nothing**: its weight is the chance that a draw produces nothing at all. It starts at 0 and takes the same clicks as any other entry — left and right click for its weight, shift-click and hotbar keys for amounts that don't matter. It can't be removed or replaced.

When you save with Nothing at weight 0, the pool has no empty entry.

## Saving

Changes are written when you **close the editor**. Closing writes the pool to its file and replaces the loaded copy, so the next roll already uses it.

| How you close                        | What happens                            |
| ------------------------------------ | --------------------------------------- |
| Left-click **Close**, or press `Esc` | Saves, if there is anything to save     |
| Right-click **Close**                | Discards everything and says so in chat |

"Anything to save" means an edit you made, or a pool one of the transforming commands handed over unsaved. Opening an existing pool and closing it untouched writes nothing and says nothing; an empty `create` closed untouched creates nothing. A pool you emptied on purpose **is** saved as empty.

The commands that transform a pool — `merge`, `weight`, `amount` and `flatten` — change nothing by themselves. They open the editor with the result already applied, and it's saved when you close it. Right-clicking **Close** is how you back out of one.

{% hint style="info" %}
`/lootpool clone` is the exception: it copies a pool of any type under a new key and writes it straight away, without opening an editor.
{% endhint %}

## The other editors

[Complex pools](complex-loot-pools.md#editing-in-a-gui) and [enchantment pools](enchantment-pools.md#the-editor) have editors of their own. They share this one's control row, its step button, its outside-click paging and its close behaviour, and add a **Back** button for the screens you can drill into.
