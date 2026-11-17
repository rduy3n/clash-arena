package com.example.game.data

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Type d'effet d'attaque utilisé par le moteur de combat 2D et le rendu Canvas.
 */
enum class MoveEffectType {
    MELEE,
    EMP_PULSE,              // Glitcherman : onde électromagnétique repoussante à 360°
    ULTI_ORBITAL_STRIKE,    // Glitcherman : ciblage satellite + rayon laser orbital céleste
    GLITCH_BLINK_STRIKE,    // Glitcherman Combo ← → + CAP : téléportation cyber + frappe éclair
    GLITCH_SHARD_BARRAGE,   // Glitcherman Combo → ← + C : double salve de cristaux laser
    VINE_WHIP,              // Jardin : fouet de lianes épineuses
    ULTI_VINE_PUNCH,        // Jardin : vignes au sol -> capture -> poing colossal 45% PV
    EARTH_QUAKE_SLAM,       // Jardin Combo ← → + CAP : écrasement sismique + projection aérienne
    THORN_UPPERCUT,         // Jardin Combo → ← + C : uppercut d'écorce dorée ascendant
    CHOPSTICK_FLURRY,       // Maître Chen : tourbillon rapide aux baguettes
    ULTI_CHOPSTICKS,        // Maître Chen : déluge multi-coups de baguettes
    DRAGON_DASH_KICK,       // Maître Chen Combo ← → + CAP : ruée fulgurante du dragon enflammé
    CHOPSTICK_VOLLEY,       // Maître Chen Combo → ← + C : lancer triple de baguettes rapides
    SWEEP_KICK,             // Daron Brésilien : balayette basse au sol
    ULTI_TROPICAL_SLAM,     // Daron Brésilien : charge blindée + méga-balayette
    SAMBA_METEOR_DROP,      // Daron Brésilien Combo ← → + CAP : bond blindé + onde surdo tropicale
    COCONUT_BOMB_TOSS       // Daron Brésilien Combo → ← + C : lancer de noix de coco explosive
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
 */
data class FighterStats(
    val maxHp: Int,
    val moveSpeed: Float,
    val jumpPower: Float,
    val defensePercent: Int,
    val heightScale: Float = 1.0f,
    val widthScale: Float = 1.0f,
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
    JUMP_CLASSIC("Saut + Classique", "↑+C"),
    DIR_LEFT_RIGHT_BASE("← → + Capacité", "←→+CAP"),
    DIR_RIGHT_LEFT_CLASSIC("→ ← + Classique", "→←+C")
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

data class FighterDefinition(
    val id: String,
    val name: String,
    val title: String,
    val archetype: String,
    val lore: String,
    val introAbilityTag: String,
    val introQuote: String,
    val introStatBadges: List<String>,
    val primaryColor: Color,
    val accentColor: Color,
    @param:DrawableRes val portraitRes: Int,
    @param:DrawableRes val victorySplashRes: Int,
    val victoryTagline: String,
    val victoryQuote: String,
    val visualStyle: FighterVisualStyle,
    val stats: FighterStats,
    val classicMove: MoveSpec,
    val baseAbility: MoveSpec,
    val comboBaseMove: MoveSpec,
    val comboClassicMove: MoveSpec,
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
 * Registre officiel et équilibré des 4 combattants et des 5 arènes de Clash Arena.
 */
object FighterRoster {

    val stages: List<ArenaStage> = listOf(
        ArenaStage(
            id = "cyber_street",
            name = "Rue Néo-Dojo",
            subtitle = "Asphalte humide & néons d'arcade",
            backgroundRes = R.drawable.img_arena_cyber_gg,
            floorColor = Color(0xFF10172B),
            accentGlow = Color(0xFF00E5FF)
        ),
        ArenaStage(
            id = "sunset_dojo",
            name = "Temple du Crépuscule",
            subtitle = "Toit sacré & cerisiers en fleurs",
            backgroundRes = R.drawable.img_arena_dojo_gg,
            floorColor = Color(0xFF28161C),
            accentGlow = Color(0xFFFF6D00)
        ),
        ArenaStage(
            id = "forest_ancient",
            name = "Forêt Ancestrale",
            subtitle = "Racines bioluminescentes & spores magiques",
            backgroundRes = R.drawable.img_arena_forest,
            floorColor = Color(0xFF0B1E14),
            accentGlow = Color(0xFF00E676)
        ),
        ArenaStage(
            id = "rio_nocturne",
            name = "Rio de Nuit",
            subtitle = "Copacabana, feux d'artifice & néons tropicaux",
            backgroundRes = R.drawable.img_arena_rio,
            floorColor = Color(0xFF1F1235),
            accentGlow = Color(0xFFFF2A85)
        ),
        ArenaStage(
            id = "orbital_station",
            name = "Station Orbitale",
            subtitle = "Passerelle spatiale en orbite & panneaux holographiques",
            backgroundRes = R.drawable.img_arena_station,
            floorColor = Color(0xFF0A1426),
            accentGlow = Color(0xFF00E5FF)
        )
    )

    val fighters: List<FighterDefinition> = listOf(
        // 1. GLITCHERMAN (PV 1650, Contrôle EMP + Frappe Orbitale, budget 300)
        FighterDefinition(
            id = "glitcherman",
            name = "Glitcherman",
            title = "Génie Cybernétique",
            archetype = "Contrôle / Orbital",
            lore = "Prodige cybernétique armé d'impulsions électromagnétiques à 360° et d'un satellite orbital prêt à foudroyer l'arène.",
            introAbilityTag = "ONDE EMP 360° • FRAPPE ORBITALE",
            introQuote = "« Onde EMP armée. Verrouillage orbital sur cible. »",
            introStatBadges = listOf("CONTRÔLE 66", "VITESSE 64", "1650 PV"),
            primaryColor = Color(0xFF00E5FF),
            accentColor = Color(0xFFFF2A85),
            portraitRes = R.drawable.img_portrait_glitcherman_sf6,
            victorySplashRes = R.drawable.img_victory_glitcherman,
            victoryTagline = "LÉVITATION CYBER • ÉCRAN PULVÉRISÉ",
            victoryQuote = "« Trop rapide pour ton pare-feu. Écran fragmenté en pixels ! »",
            visualStyle = FighterVisualStyle.GLITCHERMAN,
            stats = FighterStats(
                maxHp = 1650,
                moveSpeed = 0.50f,
                jumpPower = 1.44f,
                defensePercent = 10,
                heightScale = 1.0f,
                widthScale = 1.0f,
                ratingHp = 62,
                ratingAttack = 66,
                ratingSpeed = 64,
                ratingDefense = 50,
                ratingRange = 58
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Frappe Pixel",
                description = "Coup rapide amplifié par un gant de données numériques.",
                damage = 48,
                cooldownMs = 420L,
                range = 0.18f,
                startupMs = 75L,
                activeMs = 205L,
                knockback = 0.04f,
                ultiGainOnHit = 4,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Onde EMP",
                description = "Libère une impulsion électromagnétique à 360° qui brouille les commandes adverses pendant 3s.",
                damage = 85,
                cooldownMs = 3000L,
                range = 0.30f,
                startupMs = 115L,
                activeMs = 340L,
                knockback = 0.22f,
                ultiGainOnHit = 5,
                effectType = MoveEffectType.EMP_PULSE
            ),

            comboBaseMove = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Blink Cyber",
                description = "Combo ← → + CAPACITÉ : se téléporte à travers la grille et assène une entaille néon critique.",
                damage = 108,
                cooldownMs = 2400L,
                range = 0.40f,
                startupMs = 90L,
                activeMs = 290L,
                knockback = 0.14f,
                ultiGainOnHit = 13,
                effectType = MoveEffectType.GLITCH_BLINK_STRIKE
            ),
            comboClassicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Salve Pixel",
                description = "Combo → ← + CLASSIQUE : projette 2 éclats laser cybernétiques traversant l'arène à haute vitesse.",
                damage = 46,
                cooldownMs = 580L,
                range = 0.85f,
                startupMs = 70L,
                activeMs = 220L,
                knockback = 0.06f,
                ultiGainOnHit = 8,
                effectType = MoveEffectType.GLITCH_SHARD_BARRAGE
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Frappe Orbitale",
                description = "Verrouille la position ennemie et déclenche un rayon laser satellitaire tombant du ciel.",
                damage = 320,
                cooldownMs = 1000L,
                range = 0.58f,
                startupMs = 180L,
                activeMs = 640L,
                knockback = 0.20f,
                ultiGainOnHit = 0,
                ultiCost = 100,
                effectType = MoveEffectType.ULTI_ORBITAL_STRIKE
            ),
            comboChallenges = listOf(
                ComboChallenge(
                    id = "glitch_c1",
                    title = "Décharge EMP",
                    subtitle = "Frappe Pixel suivie d'Onde EMP repoussante",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.BASE_ABILITY)
                ),
                ComboChallenge(
                    id = "glitch_c2",
                    title = "Protocole Directionnel",
                    subtitle = "Salve Pixel (→ ← + C) puis Blink Cyber (← → + CAP)",
                    steps = listOf(ComboInputStep.DIR_RIGHT_LEFT_CLASSIC, ComboInputStep.DIR_LEFT_RIGHT_BASE)
                ),
                ComboChallenge(
                    id = "glitch_c3",
                    title = "Verrouillage Orbital",
                    subtitle = "Saut offensif, Classique, puis Frappe Orbitale",
                    steps = listOf(ComboInputStep.JUMP_CLASSIC, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 2. JARDIN (PV 1800, Puissance 76, budget 300)
        FighterDefinition(
            id = "jardin",
            name = "Jardin",
            title = "Colosse Végétal",
            archetype = "Puissance Lourde",
            lore = "Géant sylvestre massif aux coups lourds et aux ronces redoutables, désormais plus fluide entre ses frappes.",
            introAbilityTag = "VIGNES & POING COLOSSAL • FORCE BRUTE",
            introQuote = "« Mes racines et mon poing broient tout sur passage. »",
            introStatBadges = listOf("1800 PV", "PUISSANCE 76", "DÉF 62"),
            primaryColor = Color(0xFF00E676),
            accentColor = Color(0xFFFFC400),
            portraitRes = R.drawable.img_portrait_jardin_sf6,
            victorySplashRes = R.drawable.img_victory_jardin,
            victoryTagline = "COLOSSE SYLVESTRE • CAMÉRA PULVÉRISÉE",
            victoryQuote = "« La forêt reprend toujours ses droits ! Même l'objectif explose ! »",
            visualStyle = FighterVisualStyle.JARDIN,
            stats = FighterStats(
                maxHp = 1800,
                moveSpeed = 0.37f,
                jumpPower = 1.24f,
                defensePercent = 14,
                heightScale = 1.20f,
                widthScale = 1.14f,
                ratingHp = 76,
                ratingAttack = 76,
                ratingSpeed = 34,
                ratingDefense = 62,
                ratingRange = 52
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Poing Racinaire",
                description = "Coup lourd d'écorce à bonne portée et cadence rééquilibrée.",
                damage = 64,
                cooldownMs = 620L,
                range = 0.20f,
                startupMs = 145L,
                activeMs = 270L,
                knockback = 0.07f,
                ultiGainOnHit = 7,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Fouet de Ronces",
                description = "Projette de grosses lianes épineuses pour punir à mi-distance.",
                damage = 105,
                cooldownMs = 3100L,
                range = 0.32f,
                startupMs = 180L,
                activeMs = 360L,
                knockback = 0.12f,
                ultiGainOnHit = 10,
                effectType = MoveEffectType.VINE_WHIP
            ),
            comboBaseMove = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Séisme Sylvestre",
                description = "Combo ← → + CAPACITÉ : fracasse le sol à 360° et projette l'ennemi en l'air par une onde émeraude.",
                damage = 128,
                cooldownMs = 3100L,
                range = 0.36f,
                startupMs = 145L,
                activeMs = 340L,
                knockback = 0.14f,
                ultiGainOnHit = 13,
                effectType = MoveEffectType.EARTH_QUAKE_SLAM
            ),
            comboClassicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Uppercut d'Écorce",
                description = "Combo → ← + CLASSIQUE : crochet ascendant d'écorce dorée qui soulève l'adversaire pour enchaîner.",
                damage = 88,
                cooldownMs = 650L,
                range = 0.24f,
                startupMs = 110L,
                activeMs = 280L,
                knockback = 0.08f,
                ultiGainOnHit = 5,
                effectType = MoveEffectType.THORN_UPPERCUT
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Vignes & Poing Colossal",
                description = "Fait pousser des vignes au sol : poing colossal équilibré.",
                damage = 340,
                cooldownMs = 1000L,
                range = 0.26f,
                startupMs = 150L,
                activeMs = 1200L,
                knockback = 0.24f,
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
                    title = "Secousse de la Forêt",
                    subtitle = "Uppercut d'Écorce (→ ← + C) puis Séisme Sylvestre (← → + CAP)",
                    steps = listOf(ComboInputStep.DIR_RIGHT_LEFT_CLASSIC, ComboInputStep.DIR_LEFT_RIGHT_BASE)
                ),
                ComboChallenge(
                    id = "jardin_c3",
                    title = "Piège des Vignes",
                    subtitle = "Poing Racinaire puis Vignes & Poing Colossal",
                    steps = listOf(ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 3. LE CHINOIS / MAÎTRE CHEN (PV 1550, Vitesse 74, ATK 68, budget 300)
        FighterDefinition(
            id = "le_chinois",
            name = "Maître Chen",
            title = "Le Chinois aux Baguettes",
            archetype = "Vitesse / Baguettes",
            lore = "Virtuose du kung-fu maniant ses baguettes de jade avec une précision fulgurante et des enchaînements aériens.",
            introAbilityTag = "TOURBILLON DE BAGUETTES • FURIE ÉCLAIR",
            introQuote = "« Trop rapide pour tes yeux : mes baguettes ont déjà frappé. »",
            introStatBadges = listOf("VITESSE 74", "ATK 68", "1550 PV"),
            primaryColor = Color(0xFFFF3D00),
            accentColor = Color(0xFFFFD600),
            portraitRes = R.drawable.img_portrait_le_chinois_sf6,
            victorySplashRes = R.drawable.img_victory_chen,
            victoryTagline = "TEMPLE SAMOURAÏ • FESTIN DE NOUILLES",
            victoryQuote = "« Rien de tel qu'un grand bol de nouilles au temple après le combat ! »",
            visualStyle = FighterVisualStyle.CHINOIS_BAGUETTES,
            stats = FighterStats(
                maxHp = 1550,
                moveSpeed = 0.62f,
                jumpPower = 1.54f,
                defensePercent = 8,
                heightScale = 0.96f,
                widthScale = 0.95f,
                ratingHp = 54,
                ratingAttack = 68,
                ratingSpeed = 74,
                ratingDefense = 46,
                ratingRange = 58
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Double Baguettes",
                description = "Estocades rapides à deux baguettes à cadence soutenue.",
                damage = 42,
                cooldownMs = 340L,
                range = 0.20f,
                startupMs = 55L,
                activeMs = 175L,
                knockback = 0.035f,
                ultiGainOnHit = 6,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Tourbillon de Baguettes",
                description = "Ruée éclair frappant avec une rafale dorée précise.",
                damage = 96,
                cooldownMs = 2300L,
                range = 0.34f,
                startupMs = 90L,
                activeMs = 300L,
                knockback = 0.09f,
                ultiGainOnHit = 9,
                effectType = MoveEffectType.CHOPSTICK_FLURRY
            ),
            comboBaseMove = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Ruée du Dragon",
                description = "Combo ← → + CAPACITÉ : charge supersonique enflammée et coup de pied fulgurant à longue portée.",
                damage = 114,
                cooldownMs = 2300L,
                range = 0.42f,
                startupMs = 80L,
                activeMs = 300L,
                knockback = 0.14f,
                ultiGainOnHit = 12,
                effectType = MoveEffectType.DRAGON_DASH_KICK
            ),
            comboClassicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Salve de Baguettes",
                description = "Combo → ← + CLASSIQUE : lance 3 baguettes de jade dorées tournoyantes en rafale à travers l'arène.",
                damage = 38,
                cooldownMs = 540L,
                range = 0.85f,
                startupMs = 60L,
                activeMs = 210L,
                knockback = 0.04f,
                ultiGainOnHit = 7,
                effectType = MoveEffectType.CHOPSTICK_VOLLEY
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Déluge Impérial",
                description = "Enchaînement supersonique de baguettes d'or dévastateur.",
                damage = 325,
                cooldownMs = 1000L,
                range = 0.44f,
                startupMs = 90L,
                activeMs = 560L,
                knockback = 0.18f,
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
                    title = "Arts du Dragon",
                    subtitle = "Salve de Baguettes (→ ← + C) puis Ruée du Dragon (← → + CAP)",
                    steps = listOf(ComboInputStep.DIR_RIGHT_LEFT_CLASSIC, ComboInputStep.DIR_LEFT_RIGHT_BASE)
                ),
                ComboChallenge(
                    id = "chen_c3",
                    title = "Combo Impérial",
                    subtitle = "Tourbillon, Double Baguettes, puis Déluge Impérial",
                    steps = listOf(ComboInputStep.BASE_ABILITY, ComboInputStep.CLASSIC, ComboInputStep.ULTI)
                )
            )
        ),

        // 4. DARON BRÉSILIEN (PV 1950, Défense 78, budget 300)
        FighterDefinition(
            id = "daron_bresilien",
            name = "Daron Brésilien",
            title = "Tank en Veste d'Été",
            archetype = "Tank / Balayette",
            lore = "Colosse chaleureux en chemise d'été tropicale, doté d'une solide armure et d'une balayette redoutable.",
            introAbilityTag = "SAMBA BULLDOZER • BALAYETTE CARIOCA",
            introQuote = "« 1950 PV d'armure et une balayette fatale, amigo ! »",
            introStatBadges = listOf("1950 PV", "DÉFENSE 78", "18% ARMURE"),
            primaryColor = Color(0xFFFFD600),
            accentColor = Color(0xFF00C853),
            portraitRes = R.drawable.img_portrait_daron_bresilien_sf6,
            victorySplashRes = R.drawable.img_victory_daron,
            victoryTagline = "COPACABANA • TRANSAT & NOIX DE COCO",
            victoryQuote = "« Posé sur mon transat avec ma noix de coco et sa paille, amigo ! »",
            visualStyle = FighterVisualStyle.DARON_BRESILIEN,
            stats = FighterStats(
                maxHp = 1950,
                moveSpeed = 0.40f,
                jumpPower = 1.26f,
                defensePercent = 18,
                heightScale = 1.05f,
                widthScale = 1.32f,
                ratingHp = 84,
                ratingAttack = 62,
                ratingSpeed = 40,
                ratingDefense = 78,
                ratingRange = 36
            ),
            classicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Torgnole Tropicale",
                description = "Grosse claque de tank au corps-à-corps.",
                damage = 54,
                cooldownMs = 520L,
                range = 0.18f,
                startupMs = 115L,
                activeMs = 250L,
                knockback = 0.07f,
                ultiGainOnHit = 7,
                effectType = MoveEffectType.MELEE
            ),
            baseAbility = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Balayette Carioca",
                description = "Glisse au ras du sol pour faucher les jambes de l'adversaire.",
                damage = 92,
                cooldownMs = 2600L,
                range = 0.33f,
                startupMs = 110L,
                activeMs = 330L,
                knockback = 0.13f,
                ultiGainOnHit = 10,
                effectType = MoveEffectType.SWEEP_KICK
            ),
            comboBaseMove = MoveSpec(
                slot = MoveSlot.BASE_ABILITY,
                name = "Météore Samba",
                description = "Combo ← → + CAPACITÉ : bond offensif et écrasement surdo déclenchant une onde tropicale dévastatrice.",
                damage = 122,
                cooldownMs = 2600L,
                range = 0.36f,
                startupMs = 130L,
                activeMs = 350L,
                knockback = 0.21f,
                ultiGainOnHit = 13,
                effectType = MoveEffectType.SAMBA_METEOR_DROP
            ),
            comboClassicMove = MoveSpec(
                slot = MoveSlot.CLASSIC,
                name = "Coco Explosive",
                description = "Combo → ← + CLASSIQUE : projette une noix de coco enflammée qui explose à l'impact.",
                damage = 88,
                cooldownMs = 650L,
                range = 0.85f,
                startupMs = 100L,
                activeMs = 250L,
                knockback = 0.16f,
                ultiGainOnHit = 11,
                effectType = MoveEffectType.COCONUT_BOMB_TOSS
            ),
            ultimateMove = MoveSpec(
                slot = MoveSlot.ULTI,
                name = "Samba Bulldozer",
                description = "Charge blindée en veste d'été suivie d'une méga-balayette sismique.",
                damage = 315,
                cooldownMs = 1000L,
                range = 0.40f,
                startupMs = 140L,
                activeMs = 600L,
                knockback = 0.21f,
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
                    title = "Carnaval Explosif",
                    subtitle = "Coco Explosive (→ ← + C) puis Météore Samba (← → + CAP)",
                    steps = listOf(ComboInputStep.DIR_RIGHT_LEFT_CLASSIC, ComboInputStep.DIR_LEFT_RIGHT_BASE)
                ),
                ComboChallenge(
                    id = "daron_c3",
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
