package com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.entity

import com.mcstarrysky.aiyatsbus.core.util.coerceBoolean
import com.mcstarrysky.aiyatsbus.module.kether.AiyatsbusGenericProperty
import com.mcstarrysky.aiyatsbus.module.kether.AiyatsbusProperty
import org.bukkit.entity.AnimalTamer
import org.bukkit.entity.Tameable
import taboolib.common.OpenResult

/**
 * Aiyatsbus
 * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.entity.PropertyTameable
 *
 * @author mical
 * @since 2024/5/28 20:34
 */
@AiyatsbusProperty(
    id = "tameable",
    bind = Tameable::class
)
class PropertyTameable : AiyatsbusGenericProperty<Tameable>("tameable") {

    override fun readProperty(instance: Tameable, key: String): OpenResult {
        val property: Any? = when (key) {
            "owner" -> instance.owner
            "isTamed", "is-tamed", "tamed" -> instance.isTamed
            // PaperMC - Tameable#getOwnerUniqueId
            "ownerUniqueId", "owner-uniqueId", "owner-unique-id", "owner-uuid" -> instance.ownerUniqueId.toString()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful(property)
    }

    override fun writeProperty(instance: Tameable, key: String, value: Any?): OpenResult {
        when (key) {
            "owner" -> instance.owner = value as? AnimalTamer ?: return OpenResult.successful()
            "isTamed", "is-tamed", "tamed" -> instance.isTamed = value?.coerceBoolean() ?: return OpenResult.successful()
            else -> return OpenResult.failed()
        }
        return OpenResult.successful()
    }
}