# Ascendant Skills

Ascendant Skills is a Harmfy modpack add-on for Minecraft 1.21.1 + NeoForge 21.1.248.

It currently contains:

- `ascendant_skills_datapack`: Puffish Skills tree data.
- `ascendant_skills_mod`: NeoForge integration mod for parties, boss requirements, perks, debug commands, and Puffish unlock validation.

## Current MVP

- Skill costs are treated as whole Minecraft XP levels.
- Puffish points are synchronized silently from the player's current XP level.
- When a Puffish skill unlocks, the mod validates level and boss requirements.
- Valid unlocks consume XP levels and grant an internal perk id.
- Invalid unlocks are locked again and the player receives a message.
- Boss participation is tracked by damage in the last 5 minutes.
- If parties participate, the party with most damage wins boss credit.
- If no valid party participates, the top 3 solo damage dealers get credit.
- Skill requirements are loaded from `config/ascendant_skills/requirements.json`.
- Gameplay toggles are loaded from `config/ascendant_skills/gameplay.json`.
- Vanilla jump criticals and fully charged bow critical arrows are disabled by default.
- The 0.4.0 Luchador branch implements dynamic melee perks, stacks, cooldowns, balanced global resistance, knockback protection, healing hooks, and a small client HUD for perk state.

## Implemented perks

- `combate`: +5% outgoing damage, +5% global attack speed, +1 HP.
- `luchador`: +5% melee damage.
- `danzante_de_acero`: steel combo grants +2% attack speed per melee hit, up to +10%, decaying by 1 stack after 3 seconds without hitting.
- `verdugo`: +5% melee crit chance, +10% melee crit damage, and Critical Eye guarantees a melee critical against targets below 25% health with a 10 second cooldown reset on kill.
- `inamovible`: +5% global resistance. Global resistance uses a soft cap and reduced environmental scaling from `config/ascendant_skills/gameplay.json`.
- `veterano_de_guerra`: +5% melee damage and struck enemies deal -5% damage to you for 3 seconds.
- `berserker`: +1 flat melee damage with swords/axes, 4% melee life steal, and below 20% health gains +15% melee damage, +12% attack speed, and 10% total melee life steal.
- `juggernaut`: -5% outgoing melee damage, -5% incoming melee damage, and 5 seconds of knockback immunity after taking melee damage with a 15 second cooldown.
- `conquistador`: +5% melee damage, +3% incoming melee resistance, +5% bonus damage against armored targets, and hostile kills grant +2% melee damage for 5 seconds up to 5 stacks.
- `senor_de_la_guerra`: +10% melee damage, +10% healing received, and -5% incoming damage from nearby enemies.
- `cazador`: +5% projectile damage and +3% ranged crit chance.
- `punteria`: +5% projectile damage.
- `perforador`: +5% projectile damage.
- `maestro_tirador`: +10% projectile damage and +20% ranged crit damage.
- `francotirador`: +20% projectile damage from 30+ blocks, -5% below that.
- `ojo_certero`: +5% ranged crit chance and +10% ranged crit damage.
- `deadeye`: +10% ranged crit chance and +20% ranged crit damage from 30+ blocks.
- `bastion`: +5% global resistance.
- `fortaleza`: -10% incoming melee damage.
- `invencible`: +5% global resistance.
- `titan`: +5% global resistance.
- `muralla`: -5% incoming projectile damage.

## Config

The mod creates these files when the game starts:

```text
<minecraft-profile>/config/ascendant_skills/requirements.json
<minecraft-profile>/config/ascendant_skills/gameplay.json
```

`requirements.json` is a direct `skill_id -> requirement` map:

```json
{
  "cazador": {
    "levels": 1,
    "boss": "mowziesmobs:naga"
  },
  "luchador": {
    "levels": 7
  }
}
```

Use `"boss": null` or omit a skill to make it level-only. Unknown skills default to `1` level.

## Commands

```text
/ascendant_skills party invite <player>
/ascendant_skills party accept
/ascendant_skills party leave
/ascendant_skills party info
/ascendant_skills party admin invite <leader> <target>
/ascendant_skills party admin join <leader> <target>
/ascendant_skills party admin kick <player>
/ascendant_skills party admin disband <player>

/ascendant_skills boss unlock <player> <boss_id>
/ascendant_skills boss clear <player> <boss_id>
/ascendant_skills boss list <player>

/ascendant_skills perk <player> <perk_id>
/ascendant_skills perk grant <player> <perk_id>
/ascendant_skills perk revoke <player> <perk_id>
/ascendant_skills perk list <player>

/ascendant_skills attribute <player> <attribute_id> <value>

/ascendant_skills reset <player>

/ascendant_skills puffish open
/ascendant_skills puffish sync
/ascendant_skills puffish refresh [player]
/ascendant_skills puffish status

/ascendant_skills config reload
/ascendant_skills config path
```

## Building

This project depends on the Puffish Skills API. For local development, copy the matching Puffish jar into:

```text
ascendant_skills_mod/libs/puffish_skills-0.18.3-1.21-neoforge.jar
```

Then build:

```text
cd ascendant_skills_mod
./gradlew build
```

On Windows:

```text
cd ascendant_skills_mod
.\gradlew.bat build
```

The built jar appears in:

```text
ascendant_skills_mod/build/libs/
```
