---
description: >-
  Compile against lootpool-api, look pools up by key, and roll them into
  inventories, onto the ground, or into your own code—or create pools yourself.
---

# Using it from your plugin

## Adding the dependency

The API artifact is published to Maven Central:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.wyne10:lootpool-api:2.9.1")
}
```

Keep it `compileOnly`. The API classes ship inside the LootPool plugin jar at their real package names, so shading your own copy leaves you with two unrelated `LootPool` interfaces and a `ClassCastException`. The API also compiles against the Paper API, which your plugin already has.

| Package                                  | Holds                                                                                                      |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `me.wyne.lootpool.api`                   | `LootPool`, `Loot`, one record per flat [pool type](pool-types.md), `LootPoolProvider`, `LootPoolApi`      |
| `me.wyne.lootpool.api.complex`           | `ComplexLootPool`, `LootRoll`, the entry types, `LootRollContext`, `ContextualLootPool`, `EnchantmentPool` |
| `me.wyne.lootpool.api.complex.modifier`  | `LootModifier` and the built-in modifiers                                                                  |
| `me.wyne.lootpool.api.complex.condition` | `LootCondition` and the built-in conditions                                                                |

Then declare the plugin dependency in `plugin.yml`:

```yaml
depend: [LootPool]
```

Use `softdepend` if your plugin can run without it, and check for `null` as shown under [When the provider isn't there](using-it-from-your-plugin.md#when-the-provider-isnt-there). Either way the declaration makes the server enable LootPool first, and the provider doesn't exist before that.

## Getting the provider

LootPool publishes its `LootPoolProvider` two ways during its own `onEnable`. Both return the same object:

```java
// Static access point.
LootPoolProvider provider = LootPoolApi.getProvider();

// Bukkit services manager, registered at ServicePriority.Normal.
LootPoolProvider provider = Bukkit.getServicesManager()
        .getRegistration(LootPoolProvider.class)
        .getProvider();
```

By the time your plugin enables, every pool on disk is loaded. The [enchantment pool](enchantment-pools.md) registry is published the same two ways, as `EnchantmentPoolProvider` and `LootPoolApi.getEnchantmentProvider()`.

## Rolling a pool

Look the pool up by its key and call one of its `populate` methods. Each one rolls through the pool's own logic, so a composite picks a child pool, a roll pool uses its range, and so on:

```java
LootPool pool = LootPoolApi.getProvider().getLootPool("dungeon_chest");
if (pool == null) return;

// Fill a chest's empty slots in order. Returns what didn't fit.
List<ItemStack> leftovers = pool.populate(chest.getInventory());

// Same, but into random empty slots.
pool.populateRandomly(chest.getInventory());

// Roll 5 items without touching any inventory.
List<ItemStack> items = pool.populate(5);

// Draw a single entry and turn it into an item stack.
ItemStack reward = pool.getRandom().create();
```

The overloads that take no count behave as described in [Pool types](pool-types.md). A basic pool makes one draw per empty slot, a roll pool uses its range, and a snapshot places its items in their saved slots. Pass a count to control it yourself: `populate(inventory, 3)`, `populateRandomly(inventory, 3)`.

`Loot` also offers `create(Loot.Amount.MIN)`, `create(Loot.Amount.MAX)` and `create(int)` when you want a fixed stack size instead of a rolled one.

`LootPool` extends Bukkit's `LootTable`, so `populateLoot` and `fillInventory` work too, for code that expects a loot table. The `Random` and `LootContext` arguments are ignored, except by a vanilla pool.

## Rolling a complex pool

A [complex pool](complex-loot-pools.md) has conditions that can ask about a player, a world, a biome, the time and the weather. None of that fits `LootPool`'s signatures, so it is offered through an extra interface instead of widening the one everybody already implements:

```java
public interface ContextualLootPool extends LootPool {
    List<ItemStack> populate(LootRollContext context);
    List<ItemStack> populate(int slots, LootRollContext context);
    // …plus the inventory overloads
}
```

`LootRollContext` carries a nullable player, a nullable location and a luck value:

```java
LootPool pool = LootPoolApi.getProvider().getLootPool("dungeon_chest");

if (pool instanceof ContextualLootPool contextual) {
    // Everything the conditions can ask about.
    List<ItemStack> items = contextual.populate(LootRollContext.of(player));

    // Loot that belongs to a place rather than a person.
    contextual.populate(chest.getInventory(), LootRollContext.of(chest.getLocation()));
}
```

`LootRollContext.of(player)` also carries where they're standing. `of(location)` carries no player, and `LootRollContext.EMPTY` carries neither.

{% hint style="warning" %}
**A condition that can't be answered fails.** Calling the plain `populate(5)` on a complex pool rolls it with `LootRollContext.EMPTY`, so every `permission`, `world`, `biome`, `time`, `weather` and `hasloot` condition in it fails and the pool likely produces nothing. Pass a context whenever you have one.
{% endhint %}

If you'd rather not branch on the type, the plugin's own commands go through one helper you can copy:

```java
List<ItemStack> roll(LootPool pool, Integer slots, LootRollContext context) {
    if (pool instanceof ContextualLootPool contextual)
        return slots == null ? contextual.populate(context) : contextual.populate(slots, context);
    if (pool instanceof RollLootPool roll)
        return roll.populate(slots == null ? roll.rollSlots() : slots);
    return slots == null ? List.of() : pool.populate(slots);
}
```

## Reading a pool

```java
for (Loot loot : pool.getLootList()) {
    ItemStack item = loot.item();       // the template, never modified by rolls
    int weight = loot.weight();
    int min = loot.minAmount(), max = loot.maxAmount();
}
```

`getLootList()` returns an immutable copy. For multi and composite pools it's every entry of every pool they're built from, for a vanilla pool it's a fresh sample roll on every call, and for a complex pool it's every entry of every roll with pool references resolved four levels deep — **with no roll counts, conditions or modifiers applied**. Treat it as a preview, not as a description of what the pool does.

`getMapOf(Class)` gives you every pool of one type. `getMapOf(KeyedLoot.class)` returns all keyed loot, and `getMapOf(ComplexLootPool.class)` all complex pools.

## Creating pools

Pools are immutable records. Build one and hand it to the provider:

```java
LootPoolProvider provider = LootPoolApi.getProvider();

BasicLootPool gems = new BasicLootPool("gems", List.of(
        new Loot(new ItemStack(Material.EMERALD), 6, 1, 3),
        new Loot(new ItemStack(Material.DIAMOND), 3, 1, 1),
        new Loot(new ItemStack(Material.AIR), 1, 1, 1)   // 10% chance of nothing
));
provider.writeLootPool(gems);

provider.writeLootPool(new RollLootPool("gem_chest", "gems", 2, 4));
provider.writeLootPool(new CompositeLootPool("reward", Map.of("gems", 1, "ores", 3)));
```

A complex pool is built the same way, out of nested records:

```java
provider.writeLootPool(new ComplexLootPool("dungeon_chest",
        List.of(
                new LootRoll(3, 5, List.of(new PoolEntry("resources-common", 1))),
                new LootRoll(1, 1, List.of(new ItemEntry(
                        new Loot(new ItemStack(Material.DIAMOND_SWORD), 10, 1, 1),
                        List.of(),
                        List.of(new EnchantModifier("sword_enchants", 1, 3, true))
                )))
        ),
        List.of(),                                        // pool-wide modifiers
        List.of(new RandomChanceCondition(0.8))           // pool-wide conditions
));
```

| Method                | Effect                                                                                                                           |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `writeLootPool(pool)` | Registers the pool and saves it to `lootpool/<key>.yml`. It survives reloads and restarts.                                       |
| `addLootPool(pool)`   | Registers the pool in memory only. The next `/lootpool reload` or restart drops it; useful for pools you rebuild on every start. |
| `removeLootPool(key)` | Unregisters the pool and deletes its file                                                                                        |
| `reload()`            | Same as `/lootpool reload`                                                                                                       |

Both add methods replace any pool registered under the same key.

Keys follow the same rule as in-game: lowercase letters, digits, `_`, `-` and `.` only (see [Pool types](pool-types.md#choosing-a-type)). The commands check this, but the API doesn't. A pool with any other key has a `null` `getKey()`, and `addLootPool` and `writeLootPool` throw a `NullPointerException` on it. `NamespacedKey.fromString("lootpool:" + key) != null` tells you whether a key is valid.

## Copying a pool

Every built-in type implements `CloneableLootPool`, which copies it under a new key, defensively copying whatever it holds:

```java
CloneableLootPool copy = ((CloneableLootPool) pool).withKey("dungeon_chest_hard");
provider.writeLootPool(copy);
```

Pools the original _references_ by key are shared, not copied — cloning a composite gives you a second composite pointing at the same children.

## When the provider isn't there

`LootPoolApi.getProvider()` returns `null`, and so does the services-manager lookup, until LootPool has enabled, or if it isn't installed. `getLootPool(key)` returns `null` for a key that doesn't exist.

Don't keep `LootPool` objects around between uses. A reload replaces every loaded pool, and an edit replaces the pool it touched, so a stored reference goes stale. Store the key and look the pool up each time. The lookup is a map read.

The registry and inventories aren't thread-safe. Call the API from the server thread.

## Going further

To add a pool type, an item modifier or a condition of your own, see [Extending LootPool](extending-lootpool.md).
