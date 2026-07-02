# DnD Weapons Mod — Test Plan (MC 26.1.2)

## Setup
Use Creative mode. Run `/give @s dndweapons:<weapon>` or grab from the **DnD Weapons** creative tab. Use a dummy mob (Zombie, Armor Stand) as target.

---

## Test Area 1: Creative Tab & Item Registration

| # | Test | Pass Criteria |
|---|------|---------------|
| 1.1 | Open creative tab "DnD Weapons" | Tab appears with longsword icon; all 81 items visible (27 weapons × 3 tiers) |
| 1.2 | Verify iron/diamond/netherite naming | Items show "Diamond Longsword", "Netherite Longsword" prefixes |
| 1.3 | Vanilla-mapped weapons absent from tab | No shortsword, shortbow, light crossbow, trident in the DnD tab |

---

## Test Area 2: Tooltips

| # | Test | Pass Criteria |
|---|------|---------------|
| 2.1 | Hover any weapon (e.g. Longsword) | Shows stat block: "1d8 Slashing · Versatile (1d10)" |
| 2.2 | Hover Rapier | Shows "1d8 Piercing · Finesse" + "Sprint bonus: +20% damage when sprinting" |
| 2.3 | Hover Scimitar | Shows "1d6 Slashing · Finesse · Light" with both Finesse and Light bonus lines |
| 2.4 | Hover Quarterstaff | Shows "1d6 Bludgeoning · Versatile (1d8)" + versatile bonus line |
| 2.5 | Hover Lance | Shows SPECIAL_LANCE line: "Mount bonus: full damage only when mounted" |
| 2.6 | Hover Glaive | Shows "1d10 Slashing · Heavy · Reach · Two-Handed" |

---

## Test Area 3: Attack Damage (hit a Zombie, note HP lost)

| # | Weapon | Expected Dmg | Pass Criteria |
|---|--------|-------------|---------------|
| 3.1 | Club (iron) | 4 | Zombie loses ~4 HP |
| 3.2 | Greatsword (iron) | 9 | Zombie loses ~9 HP |
| 3.3 | Dagger (iron) | 4 | Zombie loses ~4 HP |
| 3.4 | Longsword (diamond) | 7 (6+1) | Diamond deals +1 vs iron |
| 3.5 | Longsword (netherite) | 8 (6+2) | Netherite deals +2 vs iron |
| 3.6 | Rapier — sprint attack | ~7.2 (6 × 1.2) | Visibly higher than standing attack |
| 3.7 | Quarterstaff — hold normally | 5 | Offhand occupied |
| 3.8 | Quarterstaff — offhand empty | 6 (versatile) | +1 damage vs occupied offhand |

---

## Test Area 4: LIGHT / Dual-Wield Bonus

| # | Test | Pass Criteria |
|---|------|---------------|
| 4.1 | Mainhand Dagger + Offhand Dagger | +1 damage bonus (both LIGHT) |
| 4.2 | Mainhand Scimitar + Offhand Scimitar | +1 damage bonus (both LIGHT) |
| 4.3 | Mainhand Rapier + Offhand Dagger | No light bonus (rapier is not LIGHT) |
| 4.4 | Mainhand Longsword + Offhand Dagger | No light bonus (longsword is not LIGHT) |

---

## Test Area 5: REACH Weapons

| # | Test | Pass Criteria |
|---|------|---------------|
| 5.1 | Glaive — stand 3 blocks from Zombie | Can hit; vanilla sword cannot at same distance |
| 5.2 | Pike, Halberd, Whip | Same reach test passes |
| 5.3 | Longsword at same 3-block distance | Cannot hit (no reach property) |

---

## Test Area 6: Lance Special Mechanic

| # | Test | Pass Criteria |
|---|------|---------------|
| 6.1 | Ride a horse, equip Lance, attack | Full damage (spec: 1d10 = 7) |
| 6.2 | Dismount, attack same Zombie with Lance | ~3-4 damage (50% penalty on foot) |

---

## Test Area 7: HEAVY Weapons — Knockback

| # | Test | Pass Criteria |
|---|------|---------------|
| 7.1 | Hit Zombie with Greataxe | Zombie knocked back visibly more than vanilla sword |
| 7.2 | Hit Zombie with Longsword | Normal knockback (no HEAVY) |

---

## Test Area 8: Attack Speed

| # | Test | Pass Criteria |
|---|------|---------------|
| 8.1 | Equip Greataxe (speed 0.9) | Slower attack cooldown than vanilla sword |
| 8.2 | Equip Scimitar (speed 1.8) | Very fast attack cooldown; no/minimal delay |
| 8.3 | Equip Rapier (speed 1.6) | Fast, but slight delay vs scimitar |

---

## Test Area 9: Crafting Recipes

| # | Test | Pass Criteria |
|---|------|---------------|
| 9.1 | Craft Dagger: 1 iron ingot + 1 stick | Produces iron dagger |
| 9.2 | Craft Longsword: 2 iron ingots + 1 stick | Produces iron longsword |
| 9.3 | Craft any weapon → check recipe book | Recipe shows up in recipe book under correct category |

---

## Test Area 10: Smithing Upgrades (Phase 4)

| # | Test | Pass Criteria |
|---|------|---------------|
| 10.1 | Craft Diamond Template Fragment | Recipe works using paper + flint + diamonds |
| 10.2 | Assemble Weapon Smithing Binding | Intermediate ingredient crafts correctly |
| 10.3 | Upgrade iron Longsword to Diamond Longsword via smithing table | Diamond longsword produced |
| 10.4 | Upgrade diamond Longsword to Netherite Longsword | Netherite longsword produced |

---

## Test Area 11: Loot & Acquisition

| # | Test | Pass Criteria |
|---|------|---------------|
| 11.1 | Find a Stronghold Library chest | May contain quarterstaff, whip, sickle, or rapier |
| 11.2 | Find a Desert Pyramid chest | May contain scimitar, spear, or dart |
| 11.3 | Kill a Vindicator | ~8% chance of dropping battleaxe |
| 11.4 | Trade with Weaponsmith Villager (Lv1) | Mace available for ~3 emeralds |
| 11.5 | Trade with Fletcher Villager (Lv1) | Darts available |

---

## Test Area 12: Netherite Durability & Fire Immunity

| # | Test | Pass Criteria |
|---|------|---------------|
| 12.1 | Drop Netherite weapon into lava | Item does not burn |
| 12.2 | Drop Iron weapon into lava | Item burns |

---

## Known Edge Cases to Watch
- **Sweep damage** — damage bonuses (finesse, dual-wield) should only apply to the primary target, not swept enemies
- **Versatile + offhand occupied** — equipping anything in offhand should revert versatile bonus
- **Vanilla-mapped role tags** — `/give @s minecraft:stone_sword` with role tag for shortsword should show DnD tooltip (if applicable)
