package org.bigcraft.lootpool.menu

import com.google.inject.Singleton
import ru.abstractmenus.api.Types

@Singleton
class AbstractMenusRegistry {

    init {
        Types.registerItemProperty("keyedLoot", KeyedLootProperty::class.java, KeyedLootProperty.Factory)
        Types.registerItemProperty("lootPool", LootPoolProperty::class.java, LootPoolProperty.Factory)
        Types.registerCatalog("LOOTPOOL", LootPoolCatalog::class.java, LootPoolCatalog.Factory)
    }

}