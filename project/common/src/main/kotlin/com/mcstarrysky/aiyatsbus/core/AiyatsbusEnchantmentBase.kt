@file:Suppress("LeakingThis")

package com.mcstarrysky.aiyatsbus.core

import com.mcstarrysky.aiyatsbus.core.data.*
import com.mcstarrysky.aiyatsbus.core.data.registry.Target
import com.mcstarrysky.aiyatsbus.core.data.registry.Rarity
import com.mcstarrysky.aiyatsbus.core.data.trigger.Trigger
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import taboolib.common.util.unsafeLazy
import taboolib.module.configuration.Configuration
import java.io.File

/**
 * 2024/8/21 此类变为抽象类, 使第三方通过代码自定义附魔更容易
 * 需要实现这个类, 此类不再提供自带的触发器, 方便其他插件如果有需要调用自己提供的触发器
 *
 * @author mical
 * @since 2024/2/17 14:39
 */
abstract class AiyatsbusEnchantmentBase(
    final override val id: String,
    final override val file: File,
    final override val config: Configuration
) : AiyatsbusEnchantment {

    override val enchantmentKey: NamespacedKey = NamespacedKey.minecraft(id)

    override val basicData: BasicData = BasicData(config.getConfigurationSection("basic")!!)

    override val alternativeData: AlternativeData = AlternativeData(config.getConfigurationSection("alternative"))

    override val dependencies: Dependencies = Dependencies(config.getConfigurationSection("dependencies"))

    override lateinit var enchantment: Enchantment

    override val rarity: Rarity
        get() = aiyatsbusRarity(config["rarity"].toString()) ?: aiyatsbusRarity(AiyatsbusSettings.defaultRarity) ?: error("Enchantment $id has an unknown rarity")

    override val variables: Variables = Variables(config.getConfigurationSection("variables"))

    override val targets: List<Target>
        get() = config.getStringList("targets").mapNotNull(::aiyatsbusTarget)

    override val displayer: Displayer = Displayer(config.getConfigurationSection("display")!!, this)

    override val limitations: Limitations = Limitations(this, config.getStringList("limitations"))
}