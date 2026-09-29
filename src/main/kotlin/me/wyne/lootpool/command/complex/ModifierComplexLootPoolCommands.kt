package me.wyne.lootpool.command.complex

import dev.jorel.commandapi.CommandAPIBukkit
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.Tooltip
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.arguments.MultiLiteralArgument
import dev.jorel.commandapi.arguments.SafeSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import me.wyne.lootpool.api.LootPoolProvider
import me.wyne.lootpool.api.complex.EnchantmentPoolProvider
import me.wyne.lootpool.api.complex.modifier.AttributeEntry
import me.wyne.lootpool.api.complex.modifier.ConditionalModifier
import me.wyne.lootpool.api.complex.modifier.DamageModifier
import me.wyne.lootpool.api.complex.modifier.EnchantModifier
import me.wyne.lootpool.api.complex.modifier.LootModifier
import me.wyne.lootpool.api.complex.modifier.LoreModifier
import me.wyne.lootpool.api.complex.modifier.NameModifier
import me.wyne.lootpool.api.complex.modifier.SetAttributesModifier
import me.wyne.lootpool.command.assertEnchantmentPoolExists
import me.wyne.lootpool.command.complexPoolKey
import me.wyne.lootpool.command.editorFor
import me.wyne.lootpool.command.elementPath
import me.wyne.lootpool.command.enchantmentPoolKey
import me.wyne.lootpool.command.failInvalidIndex
import me.wyne.lootpool.command.helpCommand
import me.wyne.lootpool.command.modified
import me.wyne.lootpool.command.ownerFor
import me.wyne.lootpool.core.ComplexOwner
import me.wyne.lootpool.core.ListHandle
import me.wyne.lootpool.core.describe
import me.wyne.lootpool.core.rewrap
import me.wyne.lootpool.core.unwrap
import me.wyne.wutils.i18n.kotlin.placeholderComponent
import me.wyne.wutils.i18n.kotlin.replace
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.command.CommandSender
import org.bukkit.inventory.EquipmentSlot

fun modifierComplexCommand(provider: LootPoolProvider, enchantmentProvider: EnchantmentPoolProvider): CommandAPICommand {
    val subcommands = listOf(
        addModifierCommand(provider, enchantmentProvider),
        removeModifierCommand(provider),
        moveModifierCommand(provider),
        conditionalModifierCommand(provider),
        setModifierCommand(provider, enchantmentProvider)
    )
    return CommandAPICommand("modifier")
        .withShortDescription("Manage item modifiers.")
        .apply { subcommands.forEach { withSubcommand(it) } }
        .withSubcommand(helpCommand("lootpool complex modifier", subcommands))
}

private fun addModifierCommand(provider: LootPoolProvider, enchantmentProvider: EnchantmentPoolProvider): CommandAPICommand {
    val enchant = CommandAPICommand("enchant")
        .withShortDescription("Add a random-enchantment modifier.")
        .withFullDescription(
            """
                Apply a random selection of enchantments drawn from an enchantment pool, each at a
                level rolled from that entry's own range.
                This is what lets one item template produce a spread of differently enchanted
                items, instead of needing one entry per combination.
                "onlyCompatible" (on by default) skips enchantments the item cannot carry and ones
                that conflict with an already-drawn enchantment; books accept anything either way.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(enchantmentPoolKey("enchantPool") { enchantmentProvider.enchantmentPoolMap })
        .withOptionalArguments(IntegerArgument("minEnchants", 0), IntegerArgument("maxEnchants", 0), BooleanArgument("onlyCompatible"))
        .executes(CommandExecutor { sender, args ->
            val modifiers = modifiersOf(ownerFor(args, sender, provider), sender)
            modifiers.add(readEnchantModifier(args, sender, enchantmentProvider))
            modified(sender, args)
        })

    val damage = CommandAPICommand("damage")
        .withShortDescription("Add a durability-damage modifier.")
        .withFullDescription(
            """
                Wear the item down by a random fraction of its durability.
                0.0 leaves it pristine and 1.0 leaves it one hit from breaking.
                A no-op on items that have no durability.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(DoubleArgument("minFraction", 0.0, 1.0))
        .withOptionalArguments(DoubleArgument("maxFraction", 0.0, 1.0))
        .executes(CommandExecutor { sender, args ->
            val modifiers = modifiersOf(ownerFor(args, sender, provider), sender)
            modifiers.add(readDamageModifier(args))
            modified(sender, args)
        })

    val name = CommandAPICommand("name")
        .withShortDescription("Add a display-name modifier.")
        .withFullDescription(
            """
                Set the produced item's display name.
                The text uses the same markup as the language files, for example
                "[gold]Excalibur".
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(GreedyStringArgument("text"))
        .executes(CommandExecutor { sender, args ->
            val modifiers = modifiersOf(ownerFor(args, sender, provider), sender)
            modifiers.add(NameModifier(args.getByClass("text", String::class.java)!!))
            modified(sender, args)
        })

    val lore = CommandAPICommand("lore")
        .withShortDescription("Add a lore modifier.")
        .withFullDescription(
            """
                Add a lore modifier holding a single first line.
                "append" adds the lines below whatever lore the item already has, "replace"
                discards the existing lore.
                Add further lines with "/lootpool complex modifier lore add".
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(MultiLiteralArgument("mode", "append", "replace"))
        .withArguments(GreedyStringArgument("text"))
        .executes(CommandExecutor { sender, args ->
            val modifiers = modifiersOf(ownerFor(args, sender, provider), sender)
            val mode = if (args.getByClass("mode", String::class.java) == "replace") LoreModifier.Mode.REPLACE else LoreModifier.Mode.APPEND
            modifiers.add(LoreModifier(listOf(args.getByClass("text", String::class.java)!!), mode))
            modified(sender, args)
        })

    val attributes = CommandAPICommand("attributes")
        .withShortDescription("Add an empty attributes modifier.")
        .withFullDescription(
            """
                Add an attribute modifier with no entries yet.
                Add attribute entries with "/lootpool complex modifier attribute add"; each one
                rolls its own amount, so the same configuration produces a spread of stat values.
                "replace" clears the item's existing modifiers for an attribute before adding to
                it; without it the new modifiers stack on top.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withOptionalArguments(BooleanArgument("replace"))
        .executes(CommandExecutor { sender, args ->
            val modifiers = modifiersOf(ownerFor(args, sender, provider), sender)
            modifiers.add(SetAttributesModifier(emptyList(), args.getByClassOrDefault("replace", Boolean::class.java, false)))
            modified(sender, args)
        })

    return CommandAPICommand("add")
        .withShortDescription("Add an item modifier.")
        .withFullDescription("Attach an item modifier to a pool, a roll or an entry.")
        .withSubcommand(enchant)
        .withSubcommand(damage)
        .withSubcommand(name)
        .withSubcommand(lore)
        .withSubcommand(attributes)
}

private fun removeModifierCommand(provider: LootPoolProvider) = CommandAPICommand("remove")
    .withShortDescription("Remove an item modifier.")
    .withFullDescription(
        """
            Remove an item modifier from the list the path names, by its index.
            Indices shift once a modifier is removed; "/lootpool info" shows the current ones.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(modifierIndexArgument<LootModifier>("modifier", provider))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        val modifiers = modifiersOf(owner, sender)
        modifiers.removeAt(modifierIndex(modifiers, owner, args, sender))
        modified(sender, args)
    })

private fun moveModifierCommand(provider: LootPoolProvider) = CommandAPICommand("move")
    .withShortDescription("Reorder an item modifier.")
    .withFullDescription(
        """
            Move an item modifier up or down its list by the given offset.
            Modifiers are applied in order, so this decides which one sees the other's result.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(modifierIndexArgument<LootModifier>("modifier", provider))
    .withArguments(IntegerArgument("delta"))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        val modifiers = modifiersOf(owner, sender)
        modifiers.move(modifierIndex(modifiers, owner, args, sender), args.getByClass("delta", Int::class.java)!!)
        modified(sender, args)
    })

private fun conditionalModifierCommand(provider: LootPoolProvider) = CommandAPICommand("conditional")
    .withShortDescription("Make a modifier conditional, or not.")
    .withFullDescription(
        """
            Wrap an item modifier so that it only applies when its own conditions pass, or unwrap
            one that already is.
            Once wrapped, add conditions to it with "/lootpool complex condition add" using the
            modifier's own path, for example "0.2.m0".
            Unwrapping discards the conditions attached to it.
        """.trimIndent()
    )
    .withPermission("lootpool.modify")
    .withArguments(complexPoolKey("key", provider))
    .withArguments(elementPath("path", provider))
    .withArguments(modifierIndexArgument<LootModifier>("modifier", provider))
    .withArguments(BooleanArgument("conditional"))
    .executes(CommandExecutor { sender, args ->
        val owner = ownerFor(args, sender, provider)
        val modifiers = modifiersOf(owner, sender)
        val index = modifierIndex(modifiers, owner, args, sender)
        val modifier = modifiers[index]
        modifiers[index] = if (args.getByClass("conditional", Boolean::class.java)!!) {
            modifier as? ConditionalModifier ?: ConditionalModifier(modifier, emptyList())
        } else {
            if (modifier is ConditionalModifier) modifier.modifier() else modifier
        }
        modified(sender, args)
    })

private fun setModifierCommand(provider: LootPoolProvider, enchantmentProvider: EnchantmentPoolProvider): CommandAPICommand {
    val enchant = CommandAPICommand("enchant")
        .withShortDescription("Change an enchant modifier.")
        .withFullDescription(
            """
                Replace an existing enchant modifier's enchantment pool, enchantment count range and
                compatibility check, keeping its position in the list.
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(modifierIndexArgument<EnchantModifier>("modifier", provider))
        .withArguments(enchantmentPoolKey("enchantPool") { enchantmentProvider.enchantmentPoolMap })
        .withOptionalArguments(IntegerArgument("minEnchants", 0), IntegerArgument("maxEnchants", 0), BooleanArgument("onlyCompatible"))
        .executes(CommandExecutor { sender, args ->
            val owner = ownerFor(args, sender, provider)
            val modifiers = modifiersOf(owner, sender)
            val index = modifierIndex(modifiers, owner, args, sender)
            modifiers[index] = rewrap(modifiers[index], readEnchantModifier(args, sender, enchantmentProvider))
            modified(sender, args)
        })

    val damage = CommandAPICommand("damage")
        .withShortDescription("Change a damage modifier.")
        .withFullDescription("Replace an existing damage modifier's durability fraction range.")
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(modifierIndexArgument<DamageModifier>("modifier", provider))
        .withArguments(DoubleArgument("minFraction", 0.0, 1.0))
        .withOptionalArguments(DoubleArgument("maxFraction", 0.0, 1.0))
        .executes(CommandExecutor { sender, args ->
            val owner = ownerFor(args, sender, provider)
            val modifiers = modifiersOf(owner, sender)
            val index = modifierIndex(modifiers, owner, args, sender)
            modifiers[index] = rewrap(modifiers[index], readDamageModifier(args))
            modified(sender, args)
        })

    val name = CommandAPICommand("name")
        .withShortDescription("Change a display-name modifier.")
        .withFullDescription(
            """
                Replace the text an existing display-name modifier gives the produced item.
                The text uses the same markup as the language files, for example "[gold]Excalibur".
            """.trimIndent()
        )
        .withPermission("lootpool.modify")
        .withArguments(complexPoolKey("key", provider))
        .withArguments(elementPath("path", provider))
        .withArguments(modifierIndexArgument<NameModifier>("modifier", provider))
        .withArguments(GreedyStringArgument("text"))
        .executes(CommandExecutor { sender, args ->
            val owner = ownerFor(args, sender, provider)
            val modifiers = modifiersOf(owner, sender)
            val index = modifierIndex(modifiers, owner, args, sender)
            modifiers[index] = rewrap(modifiers[index], NameModifier(args.getByClass("text", String::class.java)!!))
            modified(sender, args)
        })

    fun loreModifierCommand(provider: LootPoolProvider): CommandAPICommand {
        val add = CommandAPICommand("add")
            .withShortDescription("Append a lore line.")
            .withFullDescription("Append a line to an existing lore modifier.")
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<LoreModifier>("modifier", provider))
            .withArguments(GreedyStringArgument("text"))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val lore = unwrap(modifiers[index]) as? LoreModifier ?: failWrongModifierType(sender, args)
                modifiers[index] = rewrap(
                    modifiers[index],
                    LoreModifier(lore.lines() + args.getByClass("text", String::class.java)!!, lore.mode())
                )
                modified(sender, args)
            })

        val remove = CommandAPICommand("remove")
            .withShortDescription("Remove a lore line.")
            .withFullDescription("Remove a line from an existing lore modifier by its index.")
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<LoreModifier>("modifier", provider))
            .withArguments(IntegerArgument("line", 0))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val lore = unwrap(modifiers[index]) as? LoreModifier ?: failWrongModifierType(sender, args)
                val line = args.getByClass("line", Int::class.java)!!
                if (line !in lore.lines().indices) failInvalidIndex(sender, owner.path, line)
                modifiers[index] = rewrap(
                    modifiers[index],
                    LoreModifier(lore.lines().toMutableList().also { it.removeAt(line) }, lore.mode())
                )
                modified(sender, args)
            })

        val mode = CommandAPICommand("mode")
            .withShortDescription("Change a lore modifier's mode.")
            .withFullDescription("Switch an existing lore modifier between appending to and replacing the item's lore.")
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<LoreModifier>("modifier", provider))
            .withArguments(MultiLiteralArgument("mode", "append", "replace"))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val lore = unwrap(modifiers[index]) as? LoreModifier ?: failWrongModifierType(sender, args)
                val newMode = if (args.getByClass("mode", String::class.java) == "replace") LoreModifier.Mode.REPLACE else LoreModifier.Mode.APPEND
                modifiers[index] = rewrap(modifiers[index], LoreModifier(lore.lines(), newMode))
                modified(sender, args)
            })

        return CommandAPICommand("lore")
            .withShortDescription("Edit a lore modifier's lines.")
            .withFullDescription("Add, remove or re-mode the lines of an existing lore modifier.")
            .withSubcommand(add)
            .withSubcommand(remove)
            .withSubcommand(mode)
    }

    fun attributeModifierCommand(provider: LootPoolProvider): CommandAPICommand {
        val add = CommandAPICommand("add")
            .withShortDescription("Add an attribute entry.")
            .withFullDescription(
                """
                    Add one attribute modification to an existing attributes modifier.
                    "operation" is how the amount combines with the attribute's base value:
                    "add_number" adds it outright, "add_scalar" adds a fraction of the base, and
                    "multiply_scalar_1" multiplies the total.
                    The amount is rolled per drop from the given inclusive range; negative amounts
                    weaken the attribute.
                    "slot" limits the modifier to one equipment slot, defaulting to all of them.
                """.trimIndent()
            )
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<SetAttributesModifier>("modifier", provider))
            .withArguments(attributeArgument("attribute"))
            .withArguments(MultiLiteralArgument("operation", *AttributeModifier.Operation.entries.map { it.name.lowercase() }.toTypedArray()))
            .withArguments(DoubleArgument("minAmount"))
            .withOptionalArguments(DoubleArgument("maxAmount"), slotArgument("slot"))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val attributes = unwrap(modifiers[index]) as? SetAttributesModifier ?: failWrongModifierType(sender, args)

                val attribute = Attribute.valueOf(args.getByClass("attribute", String::class.java)!!.uppercase())
                val operation = AttributeModifier.Operation.valueOf(args.getByClass("operation", String::class.java)!!.uppercase())
                val minAmount = args.getByClass("minAmount", Double::class.java)!!
                val maxAmount = args.getByClassOrDefault("maxAmount", Double::class.java, minAmount)
                val slot = args.getByClass("slot", String::class.java)
                    ?.takeIf { !it.equals("any", true) }
                    ?.let { EquipmentSlot.valueOf(it.uppercase()) }

                modifiers[index] = rewrap(
                    modifiers[index],
                    SetAttributesModifier(
                        attributes.entries() + AttributeEntry(attribute, operation, minAmount, maxAmount, slot),
                        attributes.replace()
                    )
                )
                modified(sender, args)
            })

        val remove = CommandAPICommand("remove")
            .withShortDescription("Remove an attribute entry.")
            .withFullDescription("Remove one attribute modification from an existing attributes modifier by its index.")
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<SetAttributesModifier>("modifier", provider))
            .withArguments(IntegerArgument("entry", 0))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val attributes = unwrap(modifiers[index]) as? SetAttributesModifier ?: failWrongModifierType(sender, args)
                val entry = args.getByClass("entry", Int::class.java)!!
                if (entry !in attributes.entries().indices) failInvalidIndex(sender, owner.path, entry)
                modifiers[index] = rewrap(
                    modifiers[index],
                    SetAttributesModifier(attributes.entries().toMutableList().also { it.removeAt(entry) }, attributes.replace())
                )
                modified(sender, args)
            })

        val replace = CommandAPICommand("replace")
            .withShortDescription("Toggle an attributes modifier's replace flag.")
            .withFullDescription(
                """
                    Choose whether an attributes modifier clears the item's existing modifiers for an
                    attribute before adding to it, or stacks on top of them.
                """.trimIndent()
            )
            .withPermission("lootpool.modify")
            .withArguments(complexPoolKey("key", provider))
            .withArguments(elementPath("path", provider))
            .withArguments(modifierIndexArgument<SetAttributesModifier>("modifier", provider))
            .withArguments(BooleanArgument("replace"))
            .executes(CommandExecutor { sender, args ->
                val owner = ownerFor(args, sender, provider)
                val modifiers = modifiersOf(owner, sender)
                val index = modifierIndex(modifiers, owner, args, sender)
                val attributes = unwrap(modifiers[index]) as? SetAttributesModifier ?: failWrongModifierType(sender, args)
                modifiers[index] = rewrap(
                    modifiers[index],
                    SetAttributesModifier(attributes.entries(), args.getByClass("replace", Boolean::class.java)!!)
                )
                modified(sender, args)
            })

        return CommandAPICommand("attribute")
            .withShortDescription("Edit an attributes modifier.")
            .withFullDescription("Add or remove the attribute modifications of an existing attributes modifier.")
            .withSubcommand(add)
            .withSubcommand(remove)
            .withSubcommand(replace)
    }

    return CommandAPICommand("set")
        .withShortDescription("Change an item modifier.")
        .withFullDescription(
            """
                Change the settings of a modifier already attached to a pool, a roll or an entry.
                The modifier keeps its place in the list, so the order it is applied in does not
                change, and a conditional modifier stays conditional.
                "enchant", "damage" and "name" replace a modifier's settings outright, while "lore"
                and "attribute" are groups of their own, because those modifiers hold a list you
                edit an entry at a time.
            """.trimIndent()
        )
        .withSubcommand(enchant)
        .withSubcommand(damage)
        .withSubcommand(name)
        .withSubcommand(loreModifierCommand(provider))
        .withSubcommand(attributeModifierCommand(provider))
}

private fun attributeArgument(nodeName: String): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.strings(*Attribute.entries.map { it.name.lowercase() }.toTypedArray()))

private fun slotArgument(nodeName: String): Argument<String> =
    StringArgument(nodeName)
        .replaceSuggestions(ArgumentSuggestions.strings(*(listOf("any") + EquipmentSlot.entries.map { it.name.lowercase() }).toTypedArray()))

private fun failWrongModifierType(sender: CommandSender, args: CommandArguments): Nothing =
    throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent(
            "error-wrong-modifier-type",
            "path" replace args.getByClassOrDefault("path", String::class.java, ""),
            "modifier" replace args.getByClassOrDefault("modifier", Int::class.java, 0)
        ).get()
    )

private fun readEnchantModifier(args: CommandArguments, sender: CommandSender, enchantmentProvider: EnchantmentPoolProvider): EnchantModifier {
    val enchantPool = args.getByClass("enchantPool", String::class.java)!!
    assertEnchantmentPoolExists(enchantPool, sender, enchantmentProvider.enchantmentPoolMap)
    val minEnchants = args.getByClassOrDefault("minEnchants", Int::class.java, 1)
    val maxEnchants = args.getByClassOrDefault("maxEnchants", Int::class.java, minEnchants)
    return EnchantModifier(
        enchantPool,
        minEnchants,
        maxEnchants.coerceAtLeast(minEnchants),
        args.getByClassOrDefault("onlyCompatible", Boolean::class.java, true)
    )
}

private fun readDamageModifier(args: CommandArguments): DamageModifier {
    val minFraction = args.getByClass("minFraction", Double::class.java)!!
    val maxFraction = args.getByClassOrDefault("maxFraction", Double::class.java, minFraction)
    return DamageModifier(minOf(minFraction, maxFraction), maxOf(minFraction, maxFraction))
}

private inline fun <reified T : LootModifier> modifierIndexArgument(nodeName: String, provider: LootPoolProvider): Argument<Int> =
    IntegerArgument(nodeName, 0)
        .replaceSafeSuggestions(SafeSuggestions.tooltipCollection { info ->
            try {
                val owner = ownerFor(info.previousArgs, info.sender, provider)
                owner.modifiers?.items
                    ?.mapIndexedNotNull { index, modifier -> if (modifier is T) Tooltip.ofString(index, describe(modifier)) else null }
            } catch (_: WrapperCommandSyntaxException) {
                emptyList()
            }
        })

private fun modifierIndex(modifiers: ListHandle<LootModifier>, owner: ComplexOwner, args: CommandArguments, sender: CommandSender): Int {
    val index = args.getByClass("modifier", Int::class.java)!!
    if (index in modifiers.indices) return index
    failInvalidIndex(sender, owner.path, index)
}

private fun modifiersOf(owner: ComplexOwner, sender: CommandSender): ListHandle<LootModifier> =
    owner.modifiers ?: throw CommandAPIBukkit.failWithAdventureComponent(
        sender.placeholderComponent("error-path-has-no-modifiers", "path" replace owner.path).get()
    )
