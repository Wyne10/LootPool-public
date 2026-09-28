package me.wyne.lootpool.command

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.StringTooltip
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.EnchantmentArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.MultiLiteralArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.lootpool.api.complex.EnchantmentEntry
import me.wyne.lootpool.api.complex.EnchantmentPool
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.wutils.common.operation.Operations
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.reduce
import me.wyne.wutils.i18n.kotlin.replace
import me.wyne.wutils.i18n.kotlin.replaceComponent
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.enchantments.Enchantment

class CreateEnchantmentPoolCommand(provider: EnchantmentPoolProvider) : SubCommand("create") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Create an enchantment pool.")
        .withFullDescription(
            """
                Create a new, empty enchantment pool under a unique key.
                An enchantment pool is a weighted list of enchantments, each with its own level
                range, that an enchant modifier draws from when rolling loot.
                Add entries to it with "/lootpool enchant entry add".
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(StringArgument("key"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolNotExists(key, sender, provider.enchantmentPoolMap)
            provider.writeEnchantmentPool(EnchantmentPool(key, emptyList()))
            sender.placeholderComponent("success-enchantment-pool-create", "key" replace key).sendMessage(sender)
        })
}

class RemoveEnchantmentPoolCommand(provider: EnchantmentPoolProvider) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Remove an enchantment pool.")
        .withFullDescription(
            """
                Remove an enchantment pool and delete its file.
                Enchant modifiers still referencing it stop applying enchantments, but the loot
                pools using them keep working otherwise.
            """.trimIndent()
        )
        .withPermission("lootpool.remove")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            provider.removeEnchantmentPool(key)
            sender.placeholderComponent("success-enchantment-pool-remove", "key" replace key).sendMessage(sender)
        })
}

class CloneEnchantmentPoolCommand(provider: EnchantmentPoolProvider) : SubCommand("clone") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Copy an enchantment pool.")
        .withFullDescription(
            """
                Copy an existing enchantment pool's entries into a new pool under a new key.
                The original is left untouched.
            """.trimIndent()
        )
        .withPermission("lootpool.create")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .withArguments(StringArgument("newKey"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            val newKey = args.getByClass("newKey", String::class.java)!!
            assertEnchantmentPoolNotExists(newKey, sender, provider.enchantmentPoolMap)
            provider.writeEnchantmentPool(EnchantmentPool(newKey, provider.getEnchantmentPool(key)!!.entries()))
            sender.placeholderComponent("success-enchantment-pool-create", "key" replace newKey).sendMessage(sender)
        })
}

class InfoEnchantmentPoolCommand(provider: EnchantmentPoolProvider) : SubCommand("info") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Display enchantment pool contents.")
        .withFullDescription(
            """
                Display an enchantment pool's entries in chat.
                Each entry shows its enchantment, level range, weight and the chance of being drawn
                relative to the other entries.
            """.trimIndent()
        )
        .withPermission("lootpool.info")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            displayEnchantmentPoolInfo(sender, provider.getEnchantmentPool(key)!!)
        })
}

class AddEnchantmentEntryCommand(provider: EnchantmentPoolProvider) : SubCommand("add") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Add an enchantment to a pool.")
        .withFullDescription(
            """
                Add a weighted enchantment entry to an enchantment pool.
                Optionally provide a weight and an inclusive level range; omit the maximum level to
                use a fixed level, or omit both to use level 1.
                The level arguments suggest the chosen enchantment's own vanilla range, but higher
                levels are accepted - loot is enchanted ignoring vanilla level limits.
                Adding an enchantment that is already in the pool replaces its entry.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .withArguments(EnchantmentArgument("enchantment"))
        .withOptionalArguments(
            IntegerArgument("weight", 0),
            enchantmentLevel("minLevel", null),
            enchantmentLevel("maxLevel", "minLevel")
        )
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            val enchantment = args.getByClass("enchantment", Enchantment::class.java)!!
            val weight = args.getByClassOrDefault("weight", Int::class.java, 1)
            val minLevel = args.getByClassOrDefault("minLevel", Int::class.java, 1)
            val maxLevel = args.getByClassOrDefault("maxLevel", Int::class.java, minLevel)

            val pool = provider.getEnchantmentPool(key)!!
            val entries = pool.entries().filter { it.enchantment() != enchantment } +
                EnchantmentEntry(enchantment, weight, minLevel.coerceAtMost(maxLevel), maxLevel.coerceAtLeast(minLevel))
            provider.writeEnchantmentPool(EnchantmentPool(key, entries))
            sender.placeholderComponent("success-enchantment-pool-modify", "key" replace key).sendMessage(sender)
        })
}

class RemoveEnchantmentEntryCommand(provider: EnchantmentPoolProvider) : SubCommand("remove") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Remove an enchantment from a pool.")
        .withFullDescription(
            """
                Remove an enchantment entry from an enchantment pool by its index.
                Indices are shown by "/lootpool enchant info" and shift once an entry is removed.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .withArguments(IntegerArgument("entry", 0))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            val index = args.getByClass("entry", Int::class.java)!!
            val pool = provider.getEnchantmentPool(key)!!
            assertEnchantmentEntryExists(key, index, sender, pool)
            provider.writeEnchantmentPool(
                EnchantmentPool(key, pool.entries().toMutableList().also { it.removeAt(index) })
            )
            sender.placeholderComponent("success-enchantment-pool-modify", "key" replace key).sendMessage(sender)
        })
}

class SetEnchantmentEntryCommand(provider: EnchantmentPoolProvider) : SubCommand("set") {
    override val command: CommandAPICommand = super.command
        .withShortDescription("Change an enchantment entry's numbers.")
        .withFullDescription(
            """
                Apply an operation to an enchantment entry's weight, minimum level or maximum level.
                Supported operations are "+5", "-1", "*2", "/3", "**2" and "=10";
                if no operation symbol is provided, the "set" operation is performed.
                Indices are shown by "/lootpool enchant info".
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(enchantmentPoolKey("key") { provider.enchantmentPoolMap })
        .withArguments(IntegerArgument("entry", 0))
        .withArguments(MultiLiteralArgument("target", "weight", "min-level", "max-level"))
        .withArguments(GreedyStringArgument("operation"))
        .executes(CommandExecutor { sender, args ->
            val key = args.getByClass("key", String::class.java)!!
            assertEnchantmentPoolExists(key, sender, provider.enchantmentPoolMap)
            val index = args.getByClass("entry", Int::class.java)!!
            val pool = provider.getEnchantmentPool(key)!!
            assertEnchantmentEntryExists(key, index, sender, pool)

            val target = args.getByClass("target", String::class.java)!!
            val operation = Operations.getIntOperation(args.getByClass("operation", String::class.java)!!)
            val entry = pool.entries()[index]
            val updated = when (target) {
                "weight" -> EnchantmentEntry(entry.enchantment(), operation.evaluate(entry.weight()).coerceAtLeast(0), entry.minLevel(), entry.maxLevel())
                "min-level" -> {
                    val minLevel = operation.evaluate(entry.minLevel()).coerceAtLeast(1)
                    EnchantmentEntry(entry.enchantment(), entry.weight(), minLevel, entry.maxLevel().coerceAtLeast(minLevel))
                }
                else -> {
                    val maxLevel = operation.evaluate(entry.maxLevel()).coerceAtLeast(1)
                    EnchantmentEntry(entry.enchantment(), entry.weight(), entry.minLevel().coerceAtMost(maxLevel), maxLevel)
                }
            }

            provider.writeEnchantmentPool(
                EnchantmentPool(key, pool.entries().toMutableList().also { it[index] = updated })
            )
            sender.placeholderComponent("success-enchantment-pool-modify", "key" replace key).sendMessage(sender)
        })
}

/**
 * A level argument that suggests the range of the enchantment already chosen on the command line.
 *
 * Suggestions start at [lowerBoundNode]'s value when one is given - so "maxLevel" never suggests a
 * level below the "minLevel" already typed - and run to the enchantment's vanilla maximum, which is
 * marked with a tooltip. They are only suggestions: higher levels parse fine, because loot is
 * enchanted with vanilla level limits ignored.
 */
private fun enchantmentLevel(nodeName: String, lowerBoundNode: String?) =
    IntegerArgument(nodeName, 1)
        .replaceSuggestions(ArgumentSuggestions.stringsWithTooltips { info ->
            val enchantment = info.previousArgs().getByClass("enchantment", Enchantment::class.java)
                ?: return@stringsWithTooltips emptyArray()
            val vanillaMax = enchantment.maxLevel
            val start = (lowerBoundNode?.let { info.previousArgs().getByClass(it, Int::class.java) }
                ?: enchantment.startLevel).coerceAtLeast(1)
            (start..maxOf(start, vanillaMax))
                .map { level ->
                    if (level == vanillaMax)
                        StringTooltip.ofString(level.toString(), "Vanilla maximum for ${enchantment.key.key}")
                    else
                        StringTooltip.none(level.toString())
                }
                .toTypedArray()
        })

fun displayEnchantmentPoolInfo(sender: CommandSender, pool: EnchantmentPool) {
    val totalWeight = pool.entries().sumOf { it.weight().toDouble() }
    val entryList = pool.entries()
        .mapIndexed { index, entry ->
            val percentage = if (totalWeight == 0.0) 0.0 else (entry.weight() / totalWeight) * 100
            sender.placeholderComponent(
                "info-enchantment-pool-entry",
                "enchantment" replace "$index. ${entry.enchantment().key.key}",
                "weight" replace entry.weight(),
                "min-level" replace entry.minLevel(),
                "max-level" replace entry.maxLevel(),
                "percentage" replace String.format("%.2f", percentage)
            )
        }.reduce() ?: Component.empty()
    sender.placeholderComponent("info-enchantment-pool", "key" replace pool.key())
        .replace("entry-list" replaceComponent entryList)
        .sendMessage(sender)
}

fun enchantmentPoolKey(nodeName: String, enchantmentPoolMap: () -> Map<String, EnchantmentPool>): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.stringCollection { _ -> enchantmentPoolMap().keys })

fun assertEnchantmentPoolExists(key: String, sender: CommandSender, enchantmentPoolMap: Map<String, EnchantmentPool>) {
    if (enchantmentPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-enchantment-pool-not-found", "key" replace key).get()
    )
}

fun assertEnchantmentPoolNotExists(key: String, sender: CommandSender, enchantmentPoolMap: Map<String, EnchantmentPool>) {
    assertValidKey(key, sender)
    if (!enchantmentPoolMap.containsKey(key)) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-enchantment-pool-already-exists", "key" replace key).get()
    )
}

private fun assertEnchantmentEntryExists(key: String, index: Int, sender: CommandSender, pool: EnchantmentPool) {
    if (index in pool.entries().indices) return
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-enchantment-not-found", "key" replace key, "entry" replace index).get()
    )
}

fun entryEnchantmentCommand(provider: EnchantmentPoolProvider): CommandAPICommand {
    val subcommands = listOf(
        AddEnchantmentEntryCommand(provider)(),
        RemoveEnchantmentEntryCommand(provider)(),
        SetEnchantmentEntryCommand(provider)()
    )
    return CommandAPICommand("entry")
        .withShortDescription("Manage an enchantment pool's entries.")
        .apply { subcommands.forEach { withSubcommand(it) } }
        .withSubcommand(helpCommand("lootpool enchant entry", subcommands))
}
