# MHGU Quest Editor for Android

Quest editor for Monster Hunter Generations Ultimate / Monster Hunter XX (Switch), as an Android app.

Made by **Klark231**. Based on the desktop [MHGUQuestEditor](https://github.com/Fexty12573/MHGUQuestEditor)
by Fexty, with the missing pieces filled in.

## What it does

- Pick your game's `romfs` folder once. Quests, quest lists and area maps are found in one go, whichever
  language folder they're in (the quest and map folders can still be set one by one in Settings).
- The home screen lists every quest by name, with stars, map and monster
  icon, filterable by Village (`vq.arc`), Hub (`gq.arc`), Hub G rank (`gq2.arc`) and Training (`tq.arc`).
  Saving writes the quest into whichever of those lists it's in (every copy, a dump can have two).
- Quest language picker on the home screen (the translate icon): which of the 7 game languages you see and
  edit for quest names and text. The app's own language is in Settings (or Android's per-app language on 13+).
- **Extra large monsters:** add any number of copies of a large monster to an area on top of the 5 slots.
  They go into wave 1 of that area at spots the game uses, get the stats of the real one through the next
  empty slot, and the hunt objective counts them. Same layout as a working MHXX custom quest with 3 Fatalis.

## What it edits

- Quest arcs (`qXXXXXXX.arc`) and bare quest data (`.ext` / `.mib`)
- Quest data: type, rank, map, time, fees, zenny, points, requirements, icons, objectives, monsters and their stat tables, item levels, reward boxes, appearance conditions, intruders
- Quest text in all 7 game languages (`.gmd`)
- Rewards (`.rem`), all five lists plus their flags
- Small monsters (`.esl`) as 3 waves, each split by area, with move up/down, duplicate and a trigger editor for waves 2 and 3
- Large monster spawn points (`.sem`), with the preset spawn points for each map. One tap copies a point to every monster, so they all start in the same area
- Large monsters can be picked in small monster sets too (marked ★). No MHGU quest does this; in MH4U it works but can crash on area change, resets the monster when everyone leaves and restores broken parts
- **Supply box (`.sup`)**: 40 slots split into sections, the quest's two supply sets pick a section each
- **QuestPlus (`.qdp`)**: arena fence, barricades and fort HP, plus the bytes nobody has named yet
- Spawn points picked on the area map. Red dots are positions the game's own quests use (tap one for exact X/Y/Z),
  blue-grey dots are the preset spawn points that also set the facing. Tapping elsewhere is approximate and
  takes the height from the nearest dot
- Rewards and supply lists as plain text, to share or paste between quests
- Arena equipment (`.ape`, or the arc holding `quest\ac_equip\ac_pl_equip`)
- Quest lists (`vq.arc`, `gq.arc`, `gq2.arc`, `tq.arc`), updated automatically when you save
- Training quests (5 languages) and MHXX 3DS quest data (1 language) load too, the record just has fewer language slots

Saving a quest you didn't touch gives back the exact same file. That was checked against all 1849
quest arcs in the game.

## Differences from the desktop editor

- `.sup` and `.qdp` editing (the two unchecked boxes in the original README). Layouts were worked out by
  comparing every quest in the game, notes are in `Formats.kt`.
- Arc data starts on a 0x8000 boundary like the game's own arcs (the desktop editor pads to 0x10).
- QuestPlus entries are kept when saving. The desktop editor sorts by `.plus` but the type resolves to
  `.qdp`, so it dropped them.
- Small monster packs stay in their slot. Game files leave empty slots between packs; packing them
  together moves later packs to a different slot.
- Files you didn't change are written back untouched, including linked files that are empty on purpose.
- The "Other" stat table loaded from the health index, new esl names were missing the `d`, `.ext` save
  as didn't write anything. Fixed.
- Every quest data field is editable, including fee, failure points, repel HP, boss rush type and flags.

Adding brand new quests to the game isn't covered.

## Building

CI builds a signed release APK on every push to `main` and attaches it to a GitHub release.

From Termux, in this folder:

```sh
sh termux-setup.sh
```

`sh termux-setup.sh --fresh` deletes the GitHub repo first and starts it over (no old commits, runs or
releases). That logs in with `gh` if needed, makes a signing key in `~/.mhgu-quest-editor` (back it up, updates
need the same key), sets the repo secrets, pushes, waits for the build and drops the APK in
`Download/mhgu-quest-editor`.

Translations live in `i18n/*.py`. After changing one, run `python3 i18n/gen.py` to rewrite the
`strings.xml` files. CI fails if they're out of sync.

## License

Copyright (C) 2026 Klark231. Android app by Klark231, file formats and original desktop editor by Fexty.

GPLv3, same as the original. Unofficial, not made or endorsed by Capcom or Fexty.
