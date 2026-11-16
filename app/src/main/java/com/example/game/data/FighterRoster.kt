package com.example.game.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Type d'effet d'attaque utilisé par le moteur de combat 2D et le rendu Canvas.
 */
enum class MoveEffectType {
    MELEE,
    BAYONETTA_FIST,       // Glitcherman : portail + poing géant holographique
    ULTI_GLITCH_BARRAGE,  // Glitcherman : double poing géant + surcharge numérique
    VINE_WHIP,            // Jardin : fouet de lianes épineuses
    ULTI_VINE_PUNCH,      // Jardin : vignes au sol -> capture -> poing colossal 75% PV
    CHOPSTICK_FLURRY,     // Maître Chen : tourbillon rapide aux baguettes
    ULTI_CHOPSTICKS,      // Maître Chen : déluge multi-coups de baguettes
    SWEEP_KICK,           // Daron Brésilien : balayette basse au sol
    ULTI_TROPICAL_SLAM    // Daron Brésilien : charge blindée + méga-balayette
}

enum class MoveSlot(val label: String) {
    CLASSIC("Coup Classique"),
    BASE_ABILITY("Capacité de Base"),
    ULTI("Ultime")
}

/**
 * Définition complète d'une attaque (facile à ajuster pour l'équilibrage).
 */
data class MoveSpec(
    val slot: MoveSlot,
    val name: String,
    val description: String,
    val damage: Int,
    val cooldownMs: Long,
    val range: Float,          // Portée normalisée dans l'arène (0.0 à 1.0)
    val startupMs: Long,       // Temps avant l'impact
    val activeMs: Long,        // Durée de l'animation active
    val knockback: Float,      // Recul infligé à l'adversaire (0.0 à 0.30)
    val ultiGainOnHit: Int,    // Charge d'ulti gagnée si le coup touche
    val ultiCost: Int = 0,     // 100 pour l'ulti
    val effectType: MoveEffectType
)

/**
 * Statistiques de base du combattant.
 * Tous les personnages sont équilibrés autour d'un budget total de 300 points.
 */
data class FighterStats(
    val maxHp: Int,
    val moveSpeed: Float,
    val jumpPower: Float,
    val defensePercent: Int,
    val heightScale: Float = 1.0f,
    val widthScale: Float = 1.0f,
    // Notes d'affichage (sur 100) pour barres visuelles équilibrées (somme = 300)
    val ratingHp: Int,
    val ratingAttack: Int,
    val ratingSpeed: Int,
    val ratingDefense: Int,
    val ratingRange: Int
)

enum class ComboInputStep(val label: String, val badge: String) {
    CLASSIC("Classique", "C"),
    BASE_ABILITY("Capacité", "CAP"),
    ULTI("Ultime", "ULTI"),
    JUMP_CLASSIC("Saut + Classique", "↑+C")
}

data class ComboChallenge(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<ComboInputStep>
)

enum class FighterVisualStyle {
    GLITCHERMAN,
    JARDIN,
    CHINOIS_BAGUETTES,
    DARON_BRESILIEN
}

/**
 * Modèle centralisé d'un combattant.
 * Pour modifier les stats ou dégâts d'un personnage, éditez simplement son entrée dans [FighterRoster].
 */
data class FighterDefinition(
    val id: String,
    val name: String,
    val title: String,
    val archetype: String,
    val lore: String,
    val primaryColor: Color,
    val accentColor: Color,
    @param:DrawableRes val portraitRes: Int,
    val visualStyle: FighterVisualStyle,
    val stats: FighterStats,
    val classicMove: MoveSpec,
    val baseAbility: MoveSpec,
    val ultimateMove: MoveSpec,
    val comboChallenges: List<ComboChallenge>
)

data class ArenaStage(
    val id: String,
    val name: String,
    val subtitle: String,
    @param:DrawableRes val backgroundRes: Int,
    val floorColor: Color,
    val accentGlow: Color
)

/**
 * Registre officiel et équilibré des 4 combattants et des arènes de Clash Arena.
 * Somme des ratings (PV + ATK + VIT + DEF + PORTÉE) = 300 points pour chaque personnage.
 */
object FighterRoster {

    val stages: List<ArenaStage> = listOf(
        ArenaStage(
            id = "cyber_street",
            name = "Rue Néo-Dojo",
            subtitle = "Asphalte humide & néons d'arcade",
            backgroundRes = R.drawable.img_hero_arena,
            floorColor = Color(0xFF12182B),
            accentGlow = Color(0xFF00E5FF)
        ),
        ArenaStage(
            id = "sunset_dojo",
            name = "Temple du Crépuscule",
            subtitle = "Toit sacré & cerisiers en fleurs",
            backgroundRes = R.drawable.img_arena_dojo,
            floorColor = Color(0xFF28171C),
            accentGlow = Color(0xFFFF6D00)
        )
    )

    val fighters: List<FighterDefinition> = listOf(
        // 1. GLITCHERMAN -> Budget = 52 + 68 + 62 + 43 + 75 = 300
        FighterDefinition(
            id = "glitcherman",
            name = "Glitcherman",
            title = "Génie Cybernétique",
            archetype = "Techno / Portée",
            lore = "Prodige de la haute technologie. Ouvre des portails holographiques pour invoquer des poings géants façon Bayonetta.",
            primaryColor = Color(0xFF00E5FF),
            accentColor = Color(0xFFFF2A85),
            portraitRes = R.drawable.img_portrait_glitcherman,
            visualStyle = FighterVisualStyle.GLITCHERMAN,
            stats = FighterStats(
                maxHp = 980,
                moveSpeed = 0.50f,
                jumpPower = 1.44f,
                defensePercent = 7,
                heightScale = 1.0f,
                widthScale = 1.0f,
                ratingHp = 52,
                ratingAttack = 68,
                ratingSpeed = 62,
                ratingDefense = 43,
                ratingRange = 75
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Frappe Pixel",
                description = "Coup rapide amplifié par un gant de données numériques.",
                damage = 64,
                cooldownMs = 400L,
                range = 0.18f,
                startupMs = 80L,
                activeMs = 210L,
                knockback = 0.04f,
                ultiGainOnHit = 16,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Méga-Poing Holo",
                description = "Invoque à travers un portail un immense coup de poing holographique à longue portée.",
                damage = 128,
                cooldownMs = 2500L,
                range = 0.42f,
                startupMs = 150L,
                activeMs = 380L,
                knockback = 0.12f,
                ultiGainOnHit = 22,
                effectType = MoveEffectType.BAYONETTA_FIST
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Surcharge Système",
                description = "Déploie deux poings cybernétiques colossaux qui écrasent l'adversaire dans un déluge glitch.",
                damage = 285,
                cooldownMs = 1000L,
                range = 0.50f,
                startupMs = 150L,
                activeMs = 600L,
                knockback = 0.19f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_GLITCH_BARRAGE
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "glitch_c1",
                    title = "Poing de Bayonetta",
                    subtitle = "Frappe Pixel enchaînée avec Méga-Poing Holo",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "glitch_c2",
                    title = "Crash Critique",
                    subtitle = "Saut offensif, Classique, puis Surcharge Système",
                    steps = listOf(ComboInputStep.JUMP_CLASSIC, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 2. JARDIN -> Budget = 75 + 90 + 30 + 65 + 40 = 300
        FighterDefinition(
            id = "jardin",
            name = "Jardin",
            title = "Colosse Végétal",
            archetype = "Puissance Lourde",
            lore = "Géant sylvestre massif à la force brute colossale, compensée par des temps de recharge plus lents.",
            primaryColor = Color(0xFF00E676),
            accentColor = Color(0xFFFFC400),
            portraitRes = R.drawable.img_portrait_jardin,
            visualStyle = FighterVisualStyle.JARDIN,
            stats = FighterStats(
                maxHp = 1180,
                moveSpeed = 0.36f,
                jumpPower = 1.26f,
                defensePercent = 17,
                heightScale = 1.20f,
                widthScale = 1.14f,
                ratingHp = 75,
                ratingAttack = 90,
                ratingSpeed = 30,
                ratingDefense = 65,
                ratingRange = 40
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Poing Racinaire",
                description = "Coup lourd dévastateur à récupération plus lente.",
                damage = 96,
                cooldownMs = 700L,
                range = 0.20f,
                startupMs = 140L,
                activeMs = 280L,
                knockback = 0.08f,
                ultiGainOnHit = 20,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Fouet de Ronces",
                description = "Projette de grosses lianes épineuses devant lui (cooldown lent, gros dégâts).",
                damage = 148,
                cooldownMs = 3400L,
                range = 0.31f,
                startupMs = 190L,
                activeMs = 380L,
                knockback = 0.13f,
                ultiGainOnHit = 25,
                effectType = MoveEffectType.VINE_WHIP
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Vignes & Poing Colossal",
                description = "Fait pousser des vignes au sol devant lui : si elles touchent, il immobilise l'ennemi et charge un poing retirant 3/4 de la vie !",
                damage = 750, // Calculé dynamiquement à 75% des PV max de l'adversaire
                cooldownMs = 1000L,
                range = 0.28f,
                startupMs = 140L,
                activeMs = 1200L,
                knockback = 0.25f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_VINE_PUNCH
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "jardin_c1",
                    title = "Écrasement Sylvestre",
                    subtitle = "Poing Racinaire suivi de Fouet de Ronces",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "jardin_c2",
                    title = "Piège des Vignes",
                    subtitle = "Poing Racinaire puis Vignes & Poing Colossal",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 3. LE CHINOIS (MAÎTRE CHEN) -> Budget = 50 + 60 + 85 + 45 + 60 = 300
        FighterDefinition(
            id = "le_chinois",
            name = "Maître Chen",
            title = "Le Chinois aux Baguettes",
            archetype = "Vitesse / Baguettes",
            lore = "Virtuose du kung-fu maniant ses baguettes de jade avec une précision fulgurante et un jeu de jambes aérien.",
            primaryColor = Color(0xFFFF3D00),
            accentColor = Color(0xFFFFD600),
            portraitRes = R.drawable.img_portrait_le_chinois,
            visualStyle = FighterVisualStyle.CHINOIS_BAGUETTES,
            stats = FighterStats(
                maxHp = 960,
                moveSpeed = 0.61f,
                jumpPower = 1.52f,
                defensePercent = 6,
                heightScale = 0.96f,
                widthScale = 0.95f,
                ratingHp = 50,
                ratingAttack = 60,
                ratingSpeed = 85,
                ratingDefense = 45,
                ratingRange = 60
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Double Baguettes",
                description = "Estocades rapides à deux baguettes avec récupération quasi instantanée.",
                damage = 56,
                cooldownMs = 290L,
                range = 0.20f,
                startupMs = 55L,
                activeMs = 175L,
                knockback = 0.03f,
                ultiGainOnHit = 15,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Tourbillon de Baguettes",
                description = "Ruée éclair frappant avec une rafale dorée de baguettes tournoyantes.",
                damage = 114,
                cooldownMs = 2100L,
                range = 0.34f,
                startupMs = 95L,
                activeMs = 300L,
                knockback = 0.07f,
                ultiGainOnHit = 22,
                effectType = MoveEffectType.CHOPSTICK_FLURRY
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Déluge Impérial",
                description = "Enchaînement supersonique de baguettes d'or traversant toute l'arène.",
                damage = 278,
                cooldownMs = 1000L,
                range = 0.44f,
                startupMs = 100L,
                activeMs = 560L,
                knockback = 0.16f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_CHOPSTICKS
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "chen_c1",
                    title = "Danse des Baguettes",
                    subtitle = "Double Baguettes puis Tourbillon de Baguettes",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "chen_c2",
                    title = "Combo Impérial",
                    subtitle = "Tourbillon, Double Baguettes, puis Déluge Impérial",
                    steps = listOf(ComboInputStep.BASE_ABILITY, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 4. DARON BRÉSILIEN -> Budget = 95 + 65 + 35 + 75 + 30 = 300
        FighterDefinition(
            id = "daron_bresilien",
            name = "Daron Brésilien",
            title = "Tank en Veste d'Été",
            archetype = "Tank / Balayette",
            lore = "Colosse chaleureux en chemise d'été tropicale, doté d'une énorme résistance et d'une balayette redoutable.",
            primaryColor = Color(0xFFFFD600),
            accentColor = Color(0xFF00C853),
            portraitRes = R.drawable.img_portrait_daron_bresilien,
            visualStyle = FighterVisualStyle.DARON_BRESILIEN,
            stats = FighterStats(
                maxHp = 1320,
                moveSpeed = 0.38f,
                jumpPower = 1.24f,
                defensePercent = 22,
                heightScale = 1.05f,
                widthScale = 1.32f,
                ratingHp = 95,
                ratingAttack = 65,
                ratingSpeed = 35,
                ratingDefense = 75,
                ratingRange = 30
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Torgnole Tropicale",
                description = "Grosse claque de tank au corps-à-corps qui repousse l'ennemi.",
                damage = 84,
                cooldownMs = 540L,
                range = 0.18f,
                startupMs = 120L,
                activeMs = 260L,
                knockback = 0.08f,
                ultiGainOnHit = 18,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Balayette Carioca",
                description = "Glisse au ras du sol pour faucher les jambes de l'adversaire et le déstabiliser.",
                damage = 126,
                cooldownMs = 2400L,
                range = 0.34f,
                startupMs = 110L,
                activeMs = 340L,
                knockback = 0.14f,
                ultiGainOnHit = 24,
                effectType = MoveEffectType.SWEEP_KICK
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Samba Bulldozer",
                description = "Charge blindée en veste d'été suivie d'une méga-balayette sismique.",
                damage = 310,
                cooldownMs = 1000L,
                range = 0.40f,
                startupMs = 140L,
                activeMs = 600L,
                knockback = 0.22f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_TROPICAL_SLAM
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "daron_c1",
                    title = "Spécial Balayette",
                    subtitle = "Torgnole Tropicale suivie de Balayette Carioca",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "daron_c2",
                    title = "Festival de Rio",
                    subtitle = "Balayette Carioca suivie de Samba Bulldozer",
                    steps = listOf(ComboInputStep.BASE_ABILITY, ComboInputStep.ULTI)
                )
            )
        )
    )

    fun getFighter(id: String): FighterDefinition =
        fighters.firstOrNull { it.id == id } ?: fighters.first()

    fun getStage(id: String): ArenaStage =
        stages.firstOrNull { it.id == id } ?: stages.first()
}
