package org.bigcraft.lootpool.core

import com.google.inject.Inject
import com.google.inject.Singleton
import org.bigcraft.lootpool.api.CommonLootProvider
import org.bigcraft.lootpool.api.Loot

@Singleton
class CommonLootManager @Inject constructor(private val lootManager: LootManager, private val lootPoolManager: LootPoolManager) : CommonLootProvider {
    override fun getLootKeys(): Set<String> =
        lootManager.mapKeys + lootPoolManager.mapKeys

    override fun getLootList(key: String): List<Loot>? =
        lootPoolManager.getLootList(key) ?: lootManager.getLootList(key)
}