# Ascendant Skills

Ascendant Skills is a Harmfy modpack add-on for Minecraft 1.21.1 + NeoForge 21.1.248.

It currently contains:

- `ascendant_skills_datapack`: Puffish Skills tree data.
- `ascendant_skills_mod`: NeoForge integration mod for parties, boss requirements, perks, debug commands, and Puffish unlock validation.

## Current MVP

- Skill costs are treated as whole Minecraft XP levels.
- Puffish points are synchronized from the player's current XP level.
- When a Puffish skill unlocks, the mod validates level and boss requirements.
- Valid unlocks consume XP levels and grant an internal perk id.
- Invalid unlocks are locked again and the player receives a message.
- Boss participation is tracked by damage in the last 5 minutes.
- If parties participate, the party with most damage wins boss credit.
- If no valid party participates, the top 3 solo damage dealers get credit.
- Skill requirements are loaded from `config/ascendant_skills/requirements.json`.
- Gameplay toggles are loaded from `config/ascendant_skills/gameplay.json`.
- Vanilla jump criticals and fully charged bow critical arrows are disabled by default.

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
  "combatiente": {
    "levels": 1,
    "boss": "mowziesmobs:umvuthi"
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

/ascendant_skills boss unlock <player> <boss_id>
/ascendant_skills boss clear <player> <boss_id>
/ascendant_skills boss list <player>

/ascendant_skills perk <player> <perk_id>
/ascendant_skills perk grant <player> <perk_id>
/ascendant_skills perk revoke <player> <perk_id>
/ascendant_skills perk list <player>

/ascendant_skills attribute <player> <attribute_id> <value>

/ascendant_skills puffish open
/ascendant_skills puffish sync
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
