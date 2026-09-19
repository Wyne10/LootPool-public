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
    compileOnly("io.github.wyne10:lootpool-api:2.8.4")
}
```

Keep it `compileOnly`. The API classes ship inside the LootPool plugin jar at their real package names, so shading your own copy leaves you with two unrelated `LootPool` interfaces and a `ClassCastException`. The API also compiles against the Paper API, which your plugin already has.

Everything lives in `me.wyne.lootpool.api`: the `LootPool` interface, one record per [pool type](pool-types.md) (`BasicLootPool`, `KeyedLoot`, `MultiLootPool`, `CompositeLootPool`, `RollLootPool`, `SnapshotLootPool`, `VanillaLootPool`), the `Loot` entry record, the `LootPoolProvider` registry, and the `LootPoolApi` access point.

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

By the time your plugin enables, every pool on disk is loaded.

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

## Reading a pool

```java
for (Loot loot : pool.getLootList()) {
    ItemStack item = loot.item();       // the template, never modified by rolls
    int weight = loot.weight();
    int min = loot.minAmount(), max = loot.maxAmount();
}
```

`getLootList()` returns an immutable copy. For multi and composite pools it's every entry of every pool they're built from, and for a vanilla pool it's a fresh sample roll on every call.

`getMapOf(Class)` gives you every pool of one type. `getMapOf(KeyedLoot.class)` returns all keyed loot, for example.

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

| Method                | Effect                                                                                                                           |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `writeLootPool(pool)` | Registers the pool and saves it to `lootpool/<key>.yml`. It survives reloads and restarts.                                       |
| `addLootPool(pool)`   | Registers the pool in memory only. The next `/lootpool reload` or restart drops it; useful for pools you rebuild on every start. |
| `removeLootPool(key)` | Unregisters the pool and deletes its file                                                                                        |
| `reload()`            | Same as `/lootpool reload`                                                                                                       |

Both add methods replace any pool registered under the same key.

Keys follow the same rule as in-game: lowercase letters, digits, `_`, `-` and `.` only (see [Pool types](pool-types.md#choosing-a-type)). The commands check this, but the API doesn't. A pool with any other key has a `null` `getKey()`, and `addLootPool` and `writeLootPool` throw a `NullPointerException` on it. `NamespacedKey.fromString("lootpool:" + key) != null` tells you whether a key is valid.

## When the provider isn't there

`LootPoolApi.getProvider()` returns `null`, and so does the services-manager lookup, until LootPool has enabled, or if it isn't installed. `getLootPool(key)` returns `null` for a key that doesn't exist.

Don't keep `LootPool` objects around between uses. A reload replaces every loaded pool, and an edit replaces the pool it touched, so a stored reference goes stale. Store the key and look the pool up each time. The lookup is a map read.

The registry and inventories aren't thread-safe. Call the API from the server thread.
