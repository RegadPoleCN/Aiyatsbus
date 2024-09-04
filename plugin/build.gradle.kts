@file:Suppress("PropertyName", "SpellCheckingInspection")

taboolib {
    description {
        name(rootProject.name)
        contributors {
            name("Mical")
            name("坏黑")
            name("白熊")
            name("xiaozhangup")
        }

        dependencies {
            name("PlaceholderAPI").optional(true)
            name("ItemsAdder").optional(true)
            name("BentoBox").optional(true)
            name("Residence").optional(true)
            name("QuickShop").optional(true)
            name("QuickShop-Hikari").optional(true)
            name("Citizens")
        }

        desc("Aiyatsbus is a powerful enchantment framework for Paper servers.")
        load("STARTUP")
        bukkitApi("1.16")
    }

    relocate("ink", "com.mcstarrysky.aiyatsbus.module.compat.library.um")
}

dependencies {
    taboo("ink.ptms:um:1.0.9")
}

tasks {
    jar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].output) }
    }
    sourcesJar {
        // 构件名
        archiveBaseName.set(rootProject.name)
        // 打包子项目源代码
        rootProject.subprojects.forEach { from(it.sourceSets["main"].allSource) }
    }
}