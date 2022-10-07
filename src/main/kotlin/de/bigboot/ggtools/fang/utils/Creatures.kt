package de.bigboot.ggtools.fang.utils

data class Creature(val id: String, val name: String, val baby: String, val adult: String, val family: String)

object Creatures {
     val CERB = Creature("cerb", "Cerberus", "CerberusBaby", "CerberusAdult","cerb")
     val CERB_SHADOW = Creature("cerb_shadow", "Shadow Cerberus", "CerberusBaby_Shadow", "CerberusShadow","cerb")
     val CERB_STONE = Creature("cerb_stone", "Stone Cerberus", "CerberusBaby_Tough", "CerberusTough","cerb")
     val CYCLOPS = Creature("cyclops", "Mountain Cyclops", "CyclopsBaby", "CyclopsAdult","cyclops")
     val CYCLOPS_YETI = Creature("cyclops_yeti", "Yeti Cyclops", "CyclopsBaby_Frost", "CyclopsFrost","cyclops")
     val CYCLOPS_RIFTBORN = Creature("cyclops_riftborn", "Riftborn Cyclops", "CyclopsBaby_Magic", "CyclopsMagic","cyclops")
     val BLOOMER = Creature("bloomer", "Summer Bloomer", "EntBaby", "EntAdult","bloomer")
     val BLOOMER_WINTER = Creature("bloomer_winter", "Winter Bloomer", "EntBaby_Winter", "EntWinter","bloomer")
     val BLOOMER_SPRING = Creature("bloomer_spring", "Spring Bloomer", "EntBaby_Spring", "EntSpring","bloomer")
     val BLOOMER_AUTUMN = Creature("bloomer_autumn", "Autumn Bloomer", "EntBaby_Fall", "EntFall","bloomer")
     val DRAGON = Creature("dragon", "Fire Dragon", "DragonBaby", "DragonAdult","dragon")
     val DRAGON_STORM = Creature("dragon_storm", "Storm Dragon", "DragonBaby_Storm", "DragonStorm","dragon")
     val OBELISK = Creature("obelisk", "Ancient Obelisk", "ObeliskBaby", "ObeliskAdult","obelisk")
     val INFERNAL = Creature("infernal", "Crimson Infernal", "DemonBaby", "DemonAdult","infernal")
     val INFERNAL_VOID = Creature("infernal_void", "Void Infernal", "DemonBaby", "DemonVoid","infernal")
     val INFERNAL_SAVAGE = Creature("infernal_savage", "Savage Infernal", "DemonBaby", "DemonOni","infernal")

    val ALL = listOf(CERB, CERB_SHADOW, CERB_STONE, CYCLOPS, CYCLOPS_YETI, CYCLOPS_RIFTBORN, BLOOMER, BLOOMER_WINTER, BLOOMER_SPRING, BLOOMER_AUTUMN, DRAGON, DRAGON_STORM, OBELISK, INFERNAL, INFERNAL_VOID, INFERNAL_SAVAGE)
}