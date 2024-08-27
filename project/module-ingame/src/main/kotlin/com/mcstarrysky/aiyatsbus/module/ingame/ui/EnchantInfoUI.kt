@file:Suppress("DEPRECATION")

package com.mcstarrysky.aiyatsbus.module.ingame.ui

import com.mcstarrysky.aiyatsbus.core.*
import com.mcstarrysky.aiyatsbus.core.data.CheckType
import com.mcstarrysky.aiyatsbus.core.data.LimitType
import com.mcstarrysky.aiyatsbus.core.data.MenuMode
import com.mcstarrysky.aiyatsbus.core.util.*
import com.mcstarrysky.aiyatsbus.module.ingame.mechanics.VillagerSupport
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.*
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.config.MenuConfiguration
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.feature.util.MenuFunctionBuilder
import com.mcstarrysky.aiyatsbus.core.util.variable
import com.mcstarrysky.aiyatsbus.core.util.variables
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import taboolib.module.chat.component
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.PageableChest
import taboolib.platform.util.giveItem
import taboolib.platform.util.modifyLore
import taboolib.platform.util.modifyMeta
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.UIType
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.record
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.chat.Source
import kotlin.collections.set

@MenuComponent("EnchantInfo")
object EnchantInfoUI {

    @Config("core/ui/enchant_info.yml", autoReload = true)
    private lateinit var source: Configuration
    private lateinit var config: MenuConfiguration

    fun reload() {
        source.reload()
        config = MenuConfiguration(source)
    }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        source.onReload {
            config = MenuConfiguration(source)
        }
    }

    fun open(player: Player, params: Map<String, Any?>) {
        open(
            player, params["enchant"] as AiyatsbusEnchantment,
            params["level"] as Int,
            params["checked"] as ItemStack,
            params["category"].toString()
        )
    }

    fun open(
        player: Player,
        enchant: AiyatsbusEnchantment,
        level: Int = enchant.basicData.maxLevel,
        checked: ItemStack = ItemStack(Material.AIR),
        category: String = "conflicts"
    ) {
        if (player.menuMode == MenuMode.CHEAT) {
            player.giveItem(enchant.book(level))
            return
        }

        player.record(UIType.ENCHANT_INFO, "enchant" to enchant, "level" to level, "checked" to checked, "category" to category)
        player.openMenu<PageableChest<String>>(
            config.title()
                .replace("[enchant_display_roman]", enchant.displayName(level)
                    .component().buildColored().toLegacyText())
        ) {
//            virtualize()

            val (shape, templates) = config
            rows(shape.rows)
            val slots = shape["EnchantInfo:element"].toList()
            slots(slots)
            elements {
                when (category) {
                    "conflicts" -> enchant.limitations.limitations.mapNotNull { (type, identifier) ->
                        when (type) {
                            LimitType.CONFLICT_ENCHANT -> "enchant:$identifier"
                            LimitType.CONFLICT_GROUP -> "group:$identifier"
                            else -> null
                        }
                    }

                    "dependencies" -> enchant.limitations.limitations.mapNotNull { (type, identifier) ->
                        when (type) {
                            LimitType.DEPENDENCE_ENCHANT -> "enchant:$identifier"
                            LimitType.DEPENDENCE_GROUP -> "group:$identifier"
                            else -> null
                        }
                    }

                    "related" -> aiyatsbusGroups.values.filter { enchant.enchantment.isInGroup(it) }.map { "group:${it.name}" }
                    else -> listOf()
                }
            }

            load(
                shape, templates, player,
                "EnchantInfo:available", "EnchantInfo:other", "EnchantInfo:limitations",
                "EnchantInfo:basic", "EnchantInfo:level", "EnchantInfo:related",
                "EnchantInfo:dependencies", "EnchantInfo:conflicts", "EnchantInfo:element",
                "EnchantInfo:favorite", "EnchantInfo:minus", "EnchantInfo:plus", "Previous", "Next"
            )
            pages(shape, templates)

            val template = templates.require("EnchantInfo:element")
            onGenerate(async = true) { _, element, index, slot -> template(slot, index) { this["element"] = element;this["category"] = category } }
            onClick { event, element -> templates[event.rawSlot]?.handle(this, event, "element" to element) }

            val params = arrayOf(
                "player" to player,
                "enchant" to enchant,
                "level" to level,
                "checked" to checked,
                "category" to category
            )
            listOf("conflicts", "dependencies", "related").forEach {
                setSlots(shape, templates, "EnchantInfo:$it", listOf(), *params.clone().also { p -> p[4] = "category" to it })
            }
            setSlots(shape, templates, "EnchantInfo:available", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:basic", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:limitations", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:other", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:favorite", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:level", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:minus", listOf(), *params)
            setSlots(shape, templates, "EnchantInfo:plus", listOf(), *params)

            onClick { event ->
                event.isCancelled = true
                if (event.rawSlot !in shape) {
                    val item = event.currentItem ?: return@onClick
                    open(player, enchant, level, item, category)
                }
            }
        }
    }

    @MenuComponent
    private val conflicts = MenuFunctionBuilder { onClick { (_, _, _, event, args) -> open(event.clicker, args) } }

    @MenuComponent
    private val dependencies = MenuFunctionBuilder { onClick { (_, _, _, event, args) -> open(event.clicker, args) } }

    @MenuComponent
    private val related = MenuFunctionBuilder { onClick { (_, _, _, event, args) -> open(event.clicker, args) } }

    @MenuComponent
    private val level = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val player = args["player"] as Player
            val level = args["level"] as Int
            val enchant = args["enchant"] as AiyatsbusEnchantment
            icon.amount = level
            icon.variables {
                when (it) {
                    "params" -> enchant.variables.leveled.map { (variable) ->
                        player.asLang("ui-enchant-info-variables", variable to "variable", enchant.variables.leveled(variable, level, false) to "value")
                    }

                    "roman" -> listOf(level.roman())
                    else -> listOf()
                }
            }
        }
        onClick { (_, _, _, event, args) ->
            val enchant = args["enchant"] as AiyatsbusEnchantment
            open(event.clicker, args.toMutableMap().also { it["level"] = enchant.basicData.maxLevel })
        }
    }

    @MenuComponent
    private val minus = MenuFunctionBuilder {
        onClick { (_, _, _, event, args) ->
            val level = args["level"] as Int
            open(event.clicker, args.toMutableMap().also { it["level"] = (level - 1).coerceAtLeast(1) })
        }
    }

    @MenuComponent
    private val plus = MenuFunctionBuilder {
        onClick { (_, _, _, event, args) ->
            val level = args["level"] as Int
            val enchant = args["enchant"] as AiyatsbusEnchantment
            open(event.clicker, args.toMutableMap().also { it["level"] = (level + 1).coerceAtMost(enchant.basicData.maxLevel) })
        }
    }


    @MenuComponent
    private val basic = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val player = args["player"] as Player
            val holders = enchant.displayer.holders(enchant.basicData.maxLevel, player, enchant.book())
            icon.variables { variable -> listOf(holders[variable] ?: "") }
                .modifyMeta<ItemMeta> {
                    lore = lore.toBuiltComponent().map(Source::toLegacyText)
                }
                .skull(enchant.rarity.skull)
        }
    }

    @MenuComponent
    private val limitations = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val player = args["player"] as Player
            val limits = enchant.limitations.limitations
            val conflicts = limits.filter { it.first.toString().contains("CONFLICT") }.joinToString("; ") { it.second }
            val dependencies = limits.filter { it.first.toString().contains("DEPENDENCE") }.joinToString("; ") { it.second }
            val perms = limits.filter { it.first == LimitType.PERMISSION }.map { it.second }
            val permission = player.asLang("ui-enchant-info-limitations-permission-${perms.none { !player.hasPermission(it) }}")
            val activeSlots = enchant.targets.map { it.activeSlots }.flatten().toSet().joinToString("; ") {
                player.asLang("limitations-${it.name.lowercase()}")
            }
            icon.variables {
                listOf(
                    when (it) {
                        "targets" -> enchant.targets.joinToString("; ") { target -> target.name }
                        "conflicts" -> conflicts
                        "dependencies" -> dependencies
                        "permission" -> permission
                        "disable_worlds" -> enchant.basicData.disableWorlds.joinToString("; ")
                        "active_slots" -> activeSlots
                        else -> ""
                    }.ifBlank { "删除本行" }
                )
            }.modifyLore {
                removeIf { it.contains("删除本行") }
            }
        }
    }

    @MenuComponent
    private val other = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val player = args["player"] as Player
            var attainWays = ""
            if (enchant.alternativeData.isDiscoverable &&
                enchant.alternativeData.weight > 0 &&
                enchant.rarity.weight > 0
            ) attainWays += player.asLang("ui-enchant-info-other-attain-ways-discoverable-enchantable")
            if (enchant.alternativeData.isTradeable &&
                enchant.enchantment.isInGroup(VillagerSupport.tradeGroup)
            ) {
                if (attainWays.isNotBlank()) attainWays += " "
                attainWays += player.asLang("ui-enchant-info-other-attain-ways-tradeable")
            }
            icon.variables {
                listOf(
                    when (it) {
                        "attain_ways" -> attainWays
                        "grindstoneable" -> player.asLang("ui-enchant-info-other-grindstoneable-${enchant.alternativeData.grindstoneable}")
                        "treasure" -> player.asLang("ui-enchant-info-other-treasure-${enchant.alternativeData.isTreasure}")
                        "curse" -> player.asLang("ui-enchant-info-other-curse-${enchant.alternativeData.isCursed}")
                        else -> ""
                    }
                )
            }
        }
    }

    @MenuComponent
    private val available = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val player = args["player"] as Player
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val checked = args["checked"] as ItemStack
            if (checked.isNull) {
                return@onBuild icon.variables {
                    listOf(
                        when (it) {
                            "state" -> player.asLang("ui-enchant-info-available-state-unknown")
                            "reasons" -> player.asLang("ui-enchant-info-available-reasons-unknown")
                            else -> ""
                        }
                    )
                }
            }
            val level = checked.etLevel(enchant)
            val result = enchant.limitations.checkAvailable(CheckType.ANVIL, checked, player)
            val state = if (level > 0)
                player.asLang("ui-enchant-info-available-installed", player.asLang("ui-enchant-info-available-installed-can-upgrade-${level < enchant.basicData.maxLevel}") to "upgrade")
            else player.asLang("ui-enchant-info-available-can-install-${result.isSuccess}")
            val reasons = result.reason.ifEmpty { player.asLang("ui-enchant-info-available-reasons-empty") }

            icon.variables {
                listOf(
                    when (it) {
                        "state" -> state
                        "reasons" -> reasons
                        else -> ""
                    }
                )
            }
        }
    }

    @Suppress("unchecked_cast")
    @MenuComponent
    private val element = MenuFunctionBuilder {
        onBuild { (_, extra, _, _, icon, args) ->
            val element = args["element"].toString()
            val player = args["player"] as Player
            val parts = element.split(":")
            val type = parts[0].lowercase()
            val lore = extra[type + "_lore"] as List<String>
            val name = extra[type + "_name"] as String

            icon.modifyMeta<ItemMeta> {
                setDisplayName(name)
                this.lore = lore
            }.let { item ->
                when (type) {
                    "group" -> {
                        val group = aiyatsbusGroup(parts[1])!!
                        item.skull(group.skull).variables {
                                listOf(
                                    when (it) {
                                        "group" -> group.name
                                        "amount" -> group.enchantments.size.toString()
                                        "max_coexist" -> if (args["category"] == "conflicts") "${group.maxCoexist - 1}" else "删除本行"
                                        else -> ""
                                    }
                                )
                            }.modifyLore { removeIf { it.contains("删除本行") } }
                    }
                    "enchant" -> {
                        val enchant = aiyatsbusEt(parts[1])!!
                        val holders = enchant.displayer.holders(enchant.basicData.maxLevel, player, enchant.book())
                        item.skull(enchant.rarity.skull).variables { variable -> listOf(holders[variable] ?: "") }
                    }
                    else -> item
                }
            }
        }

        onClick { (_, _, _, event, args) ->
            val player = event.clicker
            val element = args["element"].toString()
            val parts = element.split(":")
            val filter = Aiyatsbus.api().getEnchantmentFilter()
            when (parts[0]) {
                "group" -> {
                    filter.clearFilters(player)
                    filter.addFilter(player, FilterType.GROUP, parts[1], FilterStatement.ON)
                    EnchantSearchUI.open(player)
                }

                "enchant" -> open(player, aiyatsbusEt(parts[1])!!)
            }
        }
    }

    @MenuComponent
    private val favorite = MenuFunctionBuilder {
        onBuild { (_, _, _, _, icon, args) ->
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val player = args["player"] as Player
            icon.variable("state", listOf(player.asLang("ui-enchant-info-favorite-${!player.favorites.contains(enchant.basicData.id)}")))
        }


        onClick { (_, _, _, event, args) ->
            val player = event.clicker
            val enchant = args["enchant"] as AiyatsbusEnchantment
            val id = enchant.basicData.id
            if (!player.favorites.contains(id)) player.favorites += id
            else player.favorites -= id

            open(player, args)
        }
    }
}