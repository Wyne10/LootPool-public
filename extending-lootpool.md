---
description: >-
  Add your own pool type, item modifier, loot entry or condition — from another
  plugin, or inside the source tree.
---

# Extending LootPool

Every layer of LootPool is an interface, and nothing about the built-in implementations is privileged. A pool type, a loot entry, an item modifier and a condition are each one interface plus Bukkit serialization, so another plugin can add its own and have it load, save, roll and show up in `/lootpool info` exactly like a built-in one.

{% hint style="warning" %}
**Commands and GUI are not generated.** Registering a type makes it load, save and roll. It does **not** give you a command to create one or a screen to edit one — every feature is tailored by hand, because the arguments a type needs, the way it should be described in chat and the clicks that ought to edit it are all specific to that type. Plan on writing those yourself, or on authoring your type's files directly. See [Commands and GUI](extending-lootpool.md#commands-and-gui).
{% endhint %}

## What you can implement

| Interface            | You get                                                                   |
| -------------------- | ------------------------------------------------------------------------- |
| `LootPool`           | A new pool type, usable everywhere a pool key is accepted                 |
| `LootEntry`          | A new kind of entry inside a [complex pool](complex-loot-pools.md)'s roll |
| `LootModifier`       | A new item modifier                                                       |
| `LootCondition`      | A new condition                                                           |
| `ContextualLootPool` | _(optional, on a pool)_ rolling against a player and a location           |
| `CloneableLootPool`  | _(optional, on a pool)_ `/lootpool clone` support                         |
| `EditableLootPool`   | _(optional, on a pool)_ `pool merge`, `weight` and `amount` support       |

`LootPool` is the only demanding one; the other three are one method each:

```java
public interface LootModifier extends ConfigurationSerializable {
    @Nullable ItemStack apply(@NotNull ItemStack item, @NotNull LootRollContext context);
}

public interface LootCondition extends ConfigurationSerializable {
    boolean test(@NotNull LootRollContext context);
}
```

Returning `null` from `apply` discards the item, which is what `DiscardModifier` does and why the return is nullable. Returning it unchanged is always a valid no-op — a modifier that can't act on an item should do that rather than throw.

Conditions carry no state about _where_ they're attached: the same `LootCondition` gates a pool, a roll, an entry or — wrapped in `ConditionalModifier` — a single modifier. Write it once.

## Registering a type

A type becomes loadable the moment it is `ConfigurationSerializable` and registered with Bukkit:

```java
ConfigurationSerialization.registerClass(MyLootPool.class);
```

**Timing is the whole trick.** LootPool reads every pool file during its own startup, and a file naming a class Bukkit doesn't know about fails to deserialize. The failure is caught and logged, so the symptom is a pool that silently isn't there rather than a crash.

So register in your plugin's **`onLoad`**, and tell the server to load your plugin first:

```yaml
# plugin.yml
name: MyPlugin
loadbefore: [LootPool]
softdepend: [LootPool]
```

```java
@Override
public void onLoad() {
    ConfigurationSerialization.registerClass(MyLootPool.class);
}
```

`loadbefore` orders the _load_ phase, which is what you need — `depend` only orders _enable_, by which time LootPool has already read its files. Registering a serializable class doesn't touch LootPool's API, so it's safe to do even when LootPool isn't installed.

### Serialization rules

* Give the class `@SerializableAs("MyLootPool")`. Bukkit registers **both** the alias and the fully qualified name, so files keep loading if you later move the class between packages, and the YAML stays readable.
*   Nested `ConfigurationSerializable` values inside a `List` or `Map` sometimes come back as `Map<String, Object>` rather than as objects. Re-map defensively. The api module's `LootSerialization` does exactly this and is public:

    ```java
    List<LootModifier> modifiers = LootSerialization.deserializeList(args.get("modifiers"), LootModifier.class);
    List<Object> raw = LootSerialization.serializeList(modifiers);
    ```
* `Enchantment`, `Attribute`, `AttributeModifier.Operation`, `EquipmentSlot` and `Material` are not serializable. Store them by `getKey().toString()` or `name()`, and read them back through a guarded lookup that **drops the entry rather than failing the whole pool** — enchantments and materials come and go between Minecraft versions. `LootSerialization.deserializeEnum` does the guarded `valueOf`.
* Prefer nesting a structured value under one key (`data.put("entries", …)`) over flattening fields onto the top level. The flat style can't carry structured values.

## A worked example: CustomItems

[CustomItems](https://app.gitbook.com/o/XsU6IzUhP3OqlEVR7PIo/s/uOPXNnW6lipqHorzIhRN/) adds a pool type in about a hundred lines. It is a single entry, like `KeyedLoot`, except that it resolves its item through the CustomItems registry on every access instead of storing it:

```java
public record CustomKeyedLoot(@NotNull String key, @NotNull Loot loot)
        implements ConfigurationSerializable, LootPool {

    public CustomKeyedLoot(@NotNull Map<String, Object> args) {
        this((String) args.get("key"), (Loot) args.get("loot"));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("loot", loot);
        return data;
    }

    /** The stored loot, with its item swapped for the custom item's current definition. */
    public @NotNull Loot getLoot() {
        CustomItemApi item = CustomItemsApi.getProvider().getItem(key);
        if (item == null) return loot;
        return new Loot(item.createItemStack(null), loot.weight(), loot.minAmount(), loot.maxAmount());
    }

    @Override public @NotNull List<Loot> getLootList()               { return List.of(getLoot()); }
    @Override public @NotNull Loot getRandom()                        { return getLoot(); }
    @Override public @NotNull List<ItemStack> populate(int slots)     { return LootPool.populate(getLootList(), slots); }
    @Override public @NotNull NamespacedKey getKey()                  { return NamespacedKey.fromString("lootpool:" + key); }
    // …the two inventory populate overloads, both one line through LootPool's static helpers
}
```

Three things make it work:

1. **`LootPool`'s static helpers do the real work.** `LootPool.populate(lootList, slots)`, `LootPool.populate(lootList, inventory, slots)` and `LootPool.populateRandomly(…)` implement the weighted draw and the placement, so an implementation only has to produce a `List<Loot>`.
2. **The pool is late-binding.** Because `getLootList()` resolves the custom item every time, an edit to the item definition shows up in every pool built on it without rewriting a single file. The built-in `MultiLootPool` and `CompositeLootPool` do the same thing with pool keys.
3. **It registers in `onLoad` under `loadbefore: [LootPool]`**, as above. It predates `@SerializableAs`, so its files name the class in full — worth adding on a new type, but not worth changing on an old one.

Creating one is then a command of CustomItems' own:

```java
LootPoolApi.getProvider()
        .writeLootPool(new CustomKeyedLoot(key, new Loot(item.createItemStack(null), weight, min, max)));
```

…which is the whole point of the split: LootPool owns storage, rolling and every consumer command; the extending plugin owns only the one command that knows what its type needs.

## Commands and GUI

Nothing about a new type is discovered automatically:

| You want                            | You write                                                                                      |
| ----------------------------------- | ---------------------------------------------------------------------------------------------- |
| A command to create your type       | Your own command, registering it with `writeLootPool`. CustomItems adds one under its own root |
| Your type in `/lootpool info`       | Nothing — the generic listing reads `getLootList()`, so a sensible flattening is enough        |
| A GUI to edit your type             | Your own inventory screens                                                                     |
| Your modifier in the complex editor | A patch to LootPool itself; see [below](extending-lootpool.md#working-in-the-source-tree)      |

The consumer side needs nothing. `/lootpool give`, `drop`, `fill`, `populate`, `spawn`, `preview`, `clone`, `remove`, `info` and the AbstractMenus integration all work against the `LootPool` interface, so your type is usable from all of them the moment it loads. That is usually enough: author the file, or write one creation command, and let the built-ins do everything else.

A modifier or condition added from outside is a smaller case still. It will load, save, apply and be rolled correctly, and `/lootpool info` prints it — but no `/lootpool complex modifier add` subcommand and no picker entry will exist for it, so the pool holding it has to be authored in YAML or built through the API.

## Working in the source tree

Adding a built-in is deliberately mechanical. For a **modifier**:

| Step | Where                                                                                                                                |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------ |
| 1    | The record itself in `api/src/main/java/me/wyne/lootpool/api/complex/modifier/`, with full JavaDoc                                   |
| 2    | `ConfigurationSerialization.registerClass(…)` in `core/LootPoolManager.kt`                                                           |
| 3    | A branch in `describe(modifier)` in `core/ComplexPath.kt`, shared by chat and GUI so they can't disagree                             |
| 4    | A subcommand under `add` and, if it has settings worth editing, under `set`, in `command/complex/ModifierComplexLootPoolCommands.kt` |
| 5    | A button in `ModifierTypeScreen` and a branch in `modifierDetailScreen`, in `gui/complex/ModifierGui.kt`                             |
| 6    | The `gui-modifier-type-*` and `gui-field-*` keys in **both** `lang/en.yml` and `lang/ru.yml`                                         |

A **condition** is the same list against `condition/`, `ConditionComplexLootPoolCommands.kt` and `ConditionGui.kt`. A **pool type** needs the record, the `registerClass` call, a creation command, and — if it holds anything worth copying — a `CloneableLootPool.withKey` implementation that defensively copies its collections.

Constraints worth knowing before you start:

* The `api` module is **pure Java on Java 16**, with one `compileOnly` dependency on the Paper API. Records and `instanceof` patterns are available; sealed interfaces and pattern-matching `switch` are not. Anything needing Kotlin, PlaceholderAPI or the plugin's own utilities lives plugin-side — `PlaceholderCondition` is the worked example of that, in `src/main/kotlin/me/wyne/lootpool/condition/`.
* Target is Paper `1.16.5`. No 1.17+ API.
* Register a plugin-side serializer **unconditionally**, even when its soft dependency is missing. If registration were conditional, a server that lost PlaceholderAPI would fail to deserialize every pool containing a `PlaceholderCondition` — and that failure drops the whole pool, not just the condition. Make only the behaviour conditional, with a documented default.
* Keep `en.yml` and `ru.yml` key for key identical.

See [Building from source](building-from-source.md) for the build itself.

## Other extension points

`LootPoolApi` carries two hooks besides the registries:

```java
// How NameModifier and LoreModifier turn stored strings into components.
LootPoolApi.setTextRenderer(text -> myFormat.deserialize(text));

// The enchantment pool registry backing EnchantModifier.
LootPoolApi.setEnchantmentProvider(myProvider);
```

The plugin installs both at startup — the text renderer is wired to the same markup the [language files](configuration.md#language-files) use. Replacing them is for embedding the API outside the plugin rather than for extending a running server; the default text renderer, used when nothing has been installed, is legacy `&`-code colour.
