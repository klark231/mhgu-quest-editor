@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.klark231.mhguqe

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ArenaPage(vm: AppViewModel, ac: AcEquip) {
    val a = vm.game.arena
    var qi by rememberSaveable { mutableIntStateOf(0) }
    var si by rememberSaveable { mutableIntStateOf(0) }
    var askDelete by remember { mutableStateOf(false) }
    if (ac.quests.isEmpty()) { Page { Hint(stringResource(R.string.arena_empty)) }; return }
    tick()
    val quest = ac.quests[qi.coerceIn(0, ac.quests.lastIndex)]

    fun e(block: () -> Unit) = vm.edit(block)

    Page {
        val questOpts = ac.quests.mapIndexed { i, q -> Opt(i, "q%07d · %dp".format(q.id, q.players)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChoiceField(stringResource(R.string.arena_quest), questOpts, { qi }, { qi = it; si = 0 }, Modifier.weight(1f))
            IconButton(onClick = { askDelete = true }, enabled = ac.quests.size > 1) { Icon(Icons.Default.Delete, stringResource(R.string.remove)) }
        }
        Section(stringResource(R.string.sec_quest)) {
            FieldRow {
                NumField(stringResource(R.string.f_quest_id), { quest.id }, { v -> e { quest.id = v } }, Modifier.weight(1f), 0, 9999999)
                NumField(stringResource(R.string.f_players), { quest.players }, { v -> e { quest.players = v } }, Modifier.weight(1f))
            }
            for (r in 0 until 3) FieldRow {
                NumField(stringResource(R.string.f_rank_time, "ABC"[r]), { quest.rankTime(r) }, { v -> e { quest.setRankTime(r, v) } }, Modifier.weight(1f), 0, Int.MAX_VALUE)
                NumField(stringResource(R.string.f_rank_points, "ABC"[r]), { quest.rankPoints(r) }, { v -> e { quest.setRankPoints(r, v) } }, Modifier.weight(1f), 0, Int.MAX_VALUE)
            }
            FieldRow {
                for (x in 0 until 3) NumField(stringResource(R.string.f_arts_flag, x + 1), { quest.artsFlag(x) }, { v -> e { quest.setArtsFlag(x, v) } }, Modifier.weight(1f))
            }
        }

        ChipBar(
            quest.sets.mapIndexed { i, s ->
                val t = a.weaponTypes.firstOrNull { it.value == s.weaponType }?.label ?: "?"
                "${i + 1} · $t"
            },
            si, { si = it },
        )
        EquipSetEditor(vm, quest.sets[si.coerceIn(0, 4)])
    }

    if (askDelete) ConfirmDialog(
        stringResource(R.string.delete_quest), stringResource(R.string.delete_quest_body, "q%07d".format(quest.id)), stringResource(R.string.remove),
        { vm.edit { ac.quests.removeAt(qi); qi = (qi - 1).coerceAtLeast(0) } },
        { askDelete = false },
    )
}

@Composable
private fun EquipSetEditor(vm: AppViewModel, s: EquipSet) {
    val g = vm.game
    val a = g.arena
    fun e(block: () -> Unit) = vm.edit(block)
    tick()
    val weapons = a.weapons[s.weaponType] ?: emptyList()
    val weaponOpts = weapons.mapIndexed { i, w -> Opt(i, w.name) }

    Section(stringResource(R.string.sec_weapon)) {
        tick()
        ChoiceField(stringResource(R.string.f_weapon_type), a.weaponTypes, { s.weaponType }, { v ->
            e { s.weaponType = v; s.weaponId = 0; s.weaponLevel = 0 }
        }, Modifier.fillMaxWidth())
        ChoiceField(
            stringResource(R.string.f_weapon), weaponOpts,
            { weapons.indexOfFirst { it.family == s.weaponId && it.level == s.weaponLevel } },
            { i -> e { s.weaponId = weapons[i].family; s.weaponLevel = weapons[i].level } },
            Modifier.fillMaxWidth(),
            fallback = { "#${s.weaponId} / ${s.weaponLevel}" },
        )
        FieldRow {
            ChoiceField(stringResource(R.string.f_style), a.styles, { s.style }, { v -> e { s.style = v } }, Modifier.weight(1f))
            NumField(stringResource(R.string.f_custom_attr), { s.customAttr }, { v -> e { s.customAttr = v } }, Modifier.weight(1f))
        }
        for (i in 0 until 3) ChoiceField(stringResource(R.string.f_art, i + 1), a.arts, { s.art(i) }, { v -> e { s.setArt(i, v) } }, Modifier.fillMaxWidth())
        for (i in 0 until 3) ChoiceField(stringResource(R.string.f_deco, i + 1), g.decorations, { s.weaponDeco(i) }, { v -> e { s.setWeaponDeco(i, v) } }, Modifier.fillMaxWidth())
    }

    if (s.weaponType == 20) Section(stringResource(R.string.sec_kinsect)) {
        val names = listOf(
            R.string.k_id, R.string.k_level, R.string.k_power, R.string.k_stamina, R.string.k_speed,
            R.string.k_fire, R.string.k_water, R.string.k_thunder, R.string.k_ice, R.string.k_dragon,
        )
        for (row in 0 until 5) FieldRow {
            for (c in 0 until 2) {
                val i = row * 2 + c
                NumField(stringResource(names[i]), { s.insect(i) }, { v -> e { s.setInsect(i, v) } }, Modifier.weight(1f))
            }
        }
    }

    val parts = listOf(R.string.p_head, R.string.p_chest, R.string.p_arms, R.string.p_waist, R.string.p_legs)
    Section(stringResource(R.string.sec_armor)) {
        tick()
        parts.forEachIndexed { p, label ->
            val max = a.maxLevel[s.armorId(p)] ?: 1
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FieldRow {
                ChoiceField(stringResource(R.string.f_piece), a.armor[p], { s.armorId(p) }, { v ->
                    e { s.setArmorId(p, v); s.setArmorLevel(p, s.armorLevel(p).coerceIn(1, a.maxLevel[v] ?: 1)) }
                }, Modifier.weight(2.4f))
                NumField(stringResource(R.string.f_level_short), { s.armorLevel(p) }, { v -> e { s.setArmorLevel(p, v) } }, Modifier.weight(1f), 0, max)
            }
            for (dd in 0 until 3) ChoiceField(
                stringResource(R.string.f_deco, dd + 1), g.decorations, { s.armorDeco(p, dd) }, { v -> e { s.setArmorDeco(p, dd, v) } }, Modifier.fillMaxWidth(),
            )
        }
    }

    Section(stringResource(R.string.sec_charm)) {
        FieldRow {
            ChoiceField(stringResource(R.string.f_skill, 1), a.skills, { s.charmSkill1 }, { v -> e { s.charmSkill1 = v } }, Modifier.weight(2.4f))
            NumField(stringResource(R.string.f_points), { s.charmLevel1 }, { v -> e { s.charmLevel1 = v } }, Modifier.weight(1f), -128, 127)
        }
        FieldRow {
            ChoiceField(stringResource(R.string.f_skill, 2), a.skills, { s.charmSkill2 }, { v -> e { s.charmSkill2 = v } }, Modifier.weight(2.4f))
            NumField(stringResource(R.string.f_points), { s.charmLevel2 }, { v -> e { s.charmLevel2 = v } }, Modifier.weight(1f), -128, 127)
        }
        for (dd in 0 until 3) ChoiceField(stringResource(R.string.f_deco, dd + 1), g.decorations, { s.armorDeco(5, dd) }, { v -> e { s.setArmorDeco(5, dd, v) } }, Modifier.fillMaxWidth())
    }

    var showItems by rememberSaveable { mutableStateOf(false) }
    Section(stringResource(R.string.sec_pouch), action = {
        IconButton(onClick = { showItems = !showItems }) { Icon(if (showItems) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null) }
    }) {
        tick()
        val used = (0 until EquipSet.ITEMS).count { s.itemId(it) != 0 }
        Hint(stringResource(R.string.pouch_count, used, EquipSet.ITEMS))
        if (showItems) for (i in 0 until EquipSet.ITEMS) FieldRow {
            ChoiceField(stringResource(R.string.slot_n, i + 1), g.items, { s.itemId(i) }, { v -> e { s.setItemId(i, v) } }, Modifier.weight(2.4f))
            NumField(stringResource(R.string.f_qty), { s.itemCount(i) }, { v -> e { s.setItemCount(i, v) } }, Modifier.weight(1f), 0, 99)
        }
    }
}
