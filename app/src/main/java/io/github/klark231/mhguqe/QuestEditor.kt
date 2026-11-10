@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.klark231.mhguqe

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

enum class QuestTab(@StringRes val title: Int, val dataOnly: Boolean) {
    GENERAL(R.string.tab_general, true),
    OBJECTIVES(R.string.tab_objectives, true),
    MONSTERS(R.string.tab_monsters, true),
    ITEMS(R.string.tab_items, true),
    CONDITIONS(R.string.tab_conditions, true),
    TEXT(R.string.tab_text, false),
    REWARDS(R.string.tab_rewards, false),
    SMALL(R.string.tab_small, false),
    LARGE(R.string.tab_large, false),
    SUPPLIES(R.string.tab_supplies, false),
    PLUS(R.string.tab_plus, false),
}

@Composable
fun QuestPage(vm: AppViewModel, d: QuestData, q: QuestArc?, tab: QuestTab) {
    when (tab) {
        QuestTab.GENERAL -> GeneralTab(vm, d, q)
        QuestTab.OBJECTIVES -> ObjectivesTab(vm, d, q)
        QuestTab.MONSTERS -> MonstersTab(vm, d, q)
        QuestTab.ITEMS -> ItemsTab(vm, d)
        QuestTab.CONDITIONS -> ConditionsTab(vm, d)
        QuestTab.TEXT -> TextTab(vm, q!!)
        QuestTab.REWARDS -> RewardsTab(vm, q!!)
        QuestTab.SMALL -> SmallMonstersTab(vm, q!!)
        QuestTab.LARGE -> LargeMonstersTab(vm, q!!)
        QuestTab.SUPPLIES -> SuppliesTab(vm, q!!)
        QuestTab.PLUS -> PlusTab(vm, q!!)
    }
}

// shorthands so the field lists stay readable
private const val I32 = Int.MAX_VALUE

@Composable
private fun AppViewModel.num(label: String, get: () -> Int, set: (Int) -> Unit, m: Modifier, min: Int = 0, max: Int = 255) =
    NumField(label, get, { v -> edit { set(v) } }, m, min, max)

@Composable
private fun AppViewModel.pick(label: String, opts: List<Opt>, get: () -> Int, set: (Int) -> Unit, m: Modifier = Modifier.fillMaxWidth()) =
    ChoiceField(label, opts, get, { v -> edit { set(v) } }, m)

// general

@Composable
fun GeneralTab(vm: AppViewModel, d: QuestData, q: QuestArc?) {
    val g = vm.game
    Page {
        if (q != null) QuestNameSection(vm, q)
        Section(stringResource(R.string.sec_quest)) {
            FieldRow {
                vm.num(stringResource(R.string.f_quest_id), { d.id }, { d.id = it }, Modifier.weight(1f), 0, 9999999)
                vm.num(stringResource(R.string.f_index), { d.index }, { d.index = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
            Hint(stringResource(R.string.quest_id_hint))
            FieldRow {
                vm.pick(stringResource(R.string.f_type), g.questTypes, { d.type }, { d.type = it }, Modifier.weight(1f))
                vm.pick(stringResource(R.string.f_category), g.subTypes, { d.subType }, { d.subType = it }, Modifier.weight(1f))
            }
            FieldRow {
                vm.pick(stringResource(R.string.f_level), g.levels, { d.level }, { d.level = it }, Modifier.weight(1f))
                vm.pick(stringResource(R.string.f_monster_rank), g.enemyLevels, { d.enemyLevel }, { d.enemyLevel = it }, Modifier.weight(1f))
            }
            FieldRow {
                vm.pick(stringResource(R.string.f_map), g.maps, { d.map }, { d.map = it }, Modifier.weight(1f))
                vm.pick(stringResource(R.string.f_start), g.startTypes, { d.startType }, { d.startType = it }, Modifier.weight(1f))
            }
            FieldRow {
                vm.num(stringResource(R.string.f_time), { d.time }, { d.time = it }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_faints), { d.faints }, { d.faints = it }, Modifier.weight(1f))
            }
            FieldRow {
                vm.pick(stringResource(R.string.f_bgm), g.bgm, { d.bgm }, { d.bgm = it }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_arena_set), { d.arenaEquipId }, { d.arenaEquipId = it }, Modifier.weight(1f))
            }
        }
        Section(stringResource(R.string.sec_requirements)) {
            vm.pick(stringResource(R.string.f_req1), g.requirements, { d.req1 }, { d.req1 = it })
            vm.pick(stringResource(R.string.f_req2), g.requirements, { d.req2 }, { d.req2 = it })
            vm.pick(stringResource(R.string.f_req_combo), g.requirements, { d.comboReq }, { d.comboReq = it })
        }
        Section(stringResource(R.string.sec_rewards)) {
            FieldRow {
                vm.num(stringResource(R.string.f_fee), { d.fee }, { d.fee = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_zenny), { d.reward }, { d.reward = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_sub_zenny), { d.subReward }, { d.subReward = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_hrp), { d.hrp }, { d.hrp = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_sub_hrp), { d.subHrp }, { d.subHrp = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_vp), { d.villagePoints }, { d.villagePoints = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_clear_vp), { d.clearVillagePoints }, { d.clearVillagePoints = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_fail_vp), { d.failVillagePoints }, { d.failVillagePoints = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_sub_vp), { d.subVillagePoints }, { d.subVillagePoints = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_vp_g), { d.villagePointsG }, { d.villagePointsG = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
            }
        }
        Section(stringResource(R.string.sec_icons)) {
            for (i in 0 until 5) vm.pick(stringResource(R.string.f_icon, i + 1), g.icons, { d.icon(i) }, { d.setIcon(i, it) })
        }
        Section(stringResource(R.string.sec_misc)) {
            FieldRow {
                vm.num(stringResource(R.string.f_repel_hp), { d.gekitaiHp }, { d.gekitaiHp = it }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_boss_rush), { d.bossRush }, { d.bossRush = it }, Modifier.weight(1f), -128, 127)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_prog), { d.progNum }, { d.progNum = it }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                vm.num(stringResource(R.string.f_flags), { d.flags }, { d.flags = it }, Modifier.weight(1f), 0, 65535)
            }
        }
    }
}

// objectives

@Composable
fun ObjectivesTab(vm: AppViewModel, d: QuestData, q: QuestArc?) {
    val g = vm.game
    Page {
        Section(stringResource(R.string.sec_clear)) {
            vm.pick(stringResource(R.string.f_clear_type), g.clearTypes, { d.clearType }, { d.clearType = it })
        }
        val titles = listOf(R.string.obj_main1, R.string.obj_main2, R.string.obj_sub)
        for (i in 0 until 3) Section(stringResource(titles[i])) {
            tick()
            vm.pick(stringResource(R.string.f_objective), g.objectives, { d.condParam(i) }, { d.setCondParam(i, it) })
            when (d.condParam(i)) {
                1, 2, 3, 4 -> {
                    vm.pick(stringResource(R.string.f_target), g.monsters + g.smallMonsters, { d.condValue(i) }, { d.setCondValue(i, it) })
                    // a hunt counts every monster with this id, also ones placed in small monster waves
                    val id = d.condValue(i)
                    if (id != 0) {
                        val inSlots = d.monsters.count { it.id == id }
                        val inWaves = q?.esls?.sumOf { w -> w.packs.sumOf { p -> p.count { it.monster == id } } } ?: 0
                        val total = inSlots + inWaves
                        Hint(stringResource(R.string.obj_count, total, inSlots, inWaves))
                        if (total > 0 && total != d.condCount(i)) TextButton(onClick = { vm.edit { d.setCondCount(i, total) } }) {
                            Text(stringResource(R.string.obj_set_count, total))
                        }
                    }
                }
                5 -> vm.pick(stringResource(R.string.f_item), g.items, { d.condValue(i) }, { d.setCondValue(i, it) })
                else -> {}
            }
            FieldRow {
                if (d.condParam(i) !in 1..5) vm.num(stringResource(R.string.f_value), { d.condValue(i) }, { d.setCondValue(i, it) }, Modifier.weight(1f), 0, 65535)
                vm.num(stringResource(R.string.f_amount), { d.condCount(i) }, { d.setCondCount(i, it) }, Modifier.weight(1f), 0, 65535)
            }
        }
    }
}

// monsters

@Composable
fun MonstersTab(vm: AppViewModel, d: QuestData, q: QuestArc?) {
    val g = vm.game
    Page {
        d.monsters.forEachIndexed { i, m ->
            Section(stringResource(R.string.monster_n, i + 1), action = {
                // same stats into every slot without a monster, that's where wave extras seem to read theirs
                IconButton(onClick = {
                    vm.edit {
                        for (o in d.monsters) if (o !== m && o.id == 0) {
                            o.hpTable = m.hpTable; o.atkTable = m.atkTable; o.otherTable = m.otherTable
                            o.size = m.size; o.sizeTable = m.sizeTable; o.staminaTable = m.staminaTable
                            o.difficulty = m.difficulty; o.restore = m.restore
                        }
                    }
                }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.stats_to_empty)) }
            }) {
                tick()
                vm.pick(stringResource(R.string.f_monster), g.monsters, { m.id }, { m.id = it })
                if (m.id == 0 && q != null && q.esls.any { w -> w.packs.any { p -> p.any { it.monster in 1 until 0x1000 } } }) {
                    Text(stringResource(R.string.slot_extra_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                val hp = (g.baseHp[m.id] ?: 0) * (g.stats.rows.getOrNull(m.hpTable)?.get(0) ?: 0f)
                if (m.id != 0) Hint(stringResource(R.string.actual_hp, hp.toInt()))
                vm.pick(stringResource(R.string.f_hp), g.hpRows, { m.hpTable }, { m.hpTable = it })
                vm.pick(stringResource(R.string.f_attack), g.atkRows, { m.atkTable }, { m.atkTable = it })
                vm.pick(stringResource(R.string.f_other), g.otherRows, { m.otherTable }, { m.otherTable = it })
                FieldRow {
                    vm.num(stringResource(R.string.f_size), { m.size }, { m.size = it }, Modifier.weight(1f), 0, 65535)
                    vm.num(stringResource(R.string.f_size_table), { m.sizeTable }, { m.sizeTable = it }, Modifier.weight(1f))
                }
                FieldRow {
                    vm.num(stringResource(R.string.f_stamina_table), { m.staminaTable }, { m.staminaTable = it }, Modifier.weight(1f))
                    vm.num(stringResource(R.string.f_difficulty), { m.difficulty }, { m.difficulty = it }, Modifier.weight(1f))
                }
                FieldRow {
                    vm.num(stringResource(R.string.f_special), { m.subType }, { m.subType = it }, Modifier.weight(1f))
                    vm.num(stringResource(R.string.f_state), { m.aura }, { m.aura = it }, Modifier.weight(1f), -128, 127)
                    vm.num(stringResource(R.string.f_restore), { m.restore }, { m.restore = it }, Modifier.weight(1f))
                }
            }
        }
        if (q != null) ExtrasSection(vm, q)
        Section(stringResource(R.string.sec_small_stats)) {
            vm.pick(stringResource(R.string.f_hp), g.hpRows, { d.smallHp }, { d.smallHp = it })
            vm.pick(stringResource(R.string.f_attack), g.atkRows, { d.smallAtk }, { d.smallAtk = it })
            vm.pick(stringResource(R.string.f_other), g.otherRows, { d.smallOther }, { d.smallOther = it })
        }
        Hint(stringResource(R.string.other_hint))
    }
}

// items

@Composable
fun ItemsTab(vm: AppViewModel, d: QuestData) {
    val g = vm.game
    Page {
        Section(stringResource(R.string.sec_gathering)) {
            vm.pick(stringResource(R.string.f_carve), g.carveLevels, { d.carveLevel }, { d.carveLevel = it })
            vm.pick(stringResource(R.string.f_gather), g.gatherLevels, { d.gatherLevel }, { d.gatherLevel = it })
            vm.pick(stringResource(R.string.f_fish), g.fishLevels, { d.fishLevel }, { d.fishLevel = it })
        }
        Section(stringResource(R.string.sec_boxes)) {
            FieldRow {
                vm.num(stringResource(R.string.f_box_a), { d.remAddFrame(0) }, { d.setRemAddFrame(0, it) }, Modifier.weight(1f), -128, 127)
                vm.num(stringResource(R.string.f_box_b), { d.remAddFrame(1) }, { d.setRemAddFrame(1, it) }, Modifier.weight(1f), -128, 127)
            }
            FieldRow {
                vm.num(stringResource(R.string.f_extra_max), { d.remAddLotMax }, { d.remAddLotMax = it }, Modifier.weight(1f), -128, 127)
                vm.num(stringResource(R.string.f_tickets), { d.extraTickets }, { d.extraTickets = it }, Modifier.weight(1f))
            }
        }
        for (i in 0 until 2) SupplySetSection(vm, d, i)
    }
}

@Composable
private fun SupplySetSection(vm: AppViewModel, d: QuestData, i: Int) {
    Section(stringResource(R.string.supply_set, 'A' + i)) {
        FieldRow {
            vm.num(stringResource(R.string.f_section), { d.suppLabel(i) }, { d.setSuppLabel(i, it) }, Modifier.weight(1f), -128, 127)
            vm.num(stringResource(R.string.f_supp_when), { d.suppType(i) }, { d.setSuppType(i, it) }, Modifier.weight(1f), -128, 127)
        }
        FieldRow {
            vm.num(stringResource(R.string.f_supp_target), { d.suppTarget(i) }, { d.setSuppTarget(i, it) }, Modifier.weight(1f), 0, 65535)
            vm.num(stringResource(R.string.f_amount), { d.suppCount(i) }, { d.setSuppCount(i, it) }, Modifier.weight(1f), 0, 65535)
        }
    }
}

// spawn conditions (the numbers in quest data, not the .sem files)

@Composable
fun ConditionsTab(vm: AppViewModel, d: QuestData) {
    Page {
        for (i in 0 until 2) Section(stringResource(R.string.enemy_set, i + 2)) {
            FieldRow {
                vm.num(stringResource(R.string.f_type), { d.setType(i) }, { d.setSetType(i, it) }, Modifier.weight(1f), -128, 127)
                vm.num(stringResource(R.string.f_target), { d.setTarget(i) }, { d.setSetTarget(i, it) }, Modifier.weight(1f), 0, 65535)
                vm.num(stringResource(R.string.f_amount), { d.setCount(i) }, { d.setSetCount(i, it) }, Modifier.weight(1f), 0, 65535)
            }
        }
        Section(stringResource(R.string.sec_appear)) {
            for (i in 0 until 5) {
                Text(stringResource(R.string.monster_n, i + 1), style = MaterialTheme.typography.labelLarge)
                FieldRow {
                    vm.num(stringResource(R.string.f_type), { d.spawnType(i) }, { d.setSpawnType(i, it) }, Modifier.weight(1f), -128, 127)
                    vm.num(stringResource(R.string.f_target), { d.spawnTarget(i) }, { d.setSpawnTarget(i, it) }, Modifier.weight(1f), 0, 65535)
                    vm.num(stringResource(R.string.f_amount), { d.spawnCount(i) }, { d.setSpawnCount(i, it) }, Modifier.weight(1f), 0, 65535)
                }
            }
        }
        Section(stringResource(R.string.sec_stray)) {
            FieldRow {
                vm.num(stringResource(R.string.f_stray_start), { d.strayStart }, { d.strayStart = it }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_stray_start_rand), { d.strayStartRand }, { d.strayStartRand = it }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_stray_rand), { d.strayRand }, { d.strayRand = it }, Modifier.weight(1f))
            }
            for (i in 0 until 3) FieldRow {
                vm.num(stringResource(R.string.f_stray_limit, i + 3), { d.strayLimit(i) }, { d.setStrayLimit(i, it) }, Modifier.weight(1f), -128, 127)
                vm.num(stringResource(R.string.f_stray_chance, i + 3), { d.strayRand345(i) }, { d.setStrayRand345(i, it) }, Modifier.weight(1f))
            }
        }
    }
}

// text

private val textLabels = listOf(
    R.string.t_name, R.string.t_client, R.string.t_description, R.string.t_small,
    R.string.t_objective, R.string.t_failure, R.string.t_sub,
)

@Composable
fun TextTab(vm: AppViewModel, q: QuestArc) {
    val present = (0 until 7).filter { q.text[it] != null }
    if (present.isEmpty()) { Page { Hint(stringResource(R.string.no_text)) }; return }
    val start = appLang(q)
    var lang by rememberSaveable { mutableIntStateOf(if (start in present) start else present.first()) }
    var askCopy by remember { mutableStateOf(false) }
    val gmd = q.text[lang] ?: q.text[present.first()]!!
    Page {
        ChipBar(Lang.names, lang, { lang = it }, { q.text[it] != null })
        Section(Lang.names[lang], action = {
            TextButton(onClick = { askCopy = true }) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.copy_to_all))
            }
        }) {
            tick()
            gmd.entries.indices.forEach { i ->
                val single = i == 0 || i == 1 || i == 6
                TextArea(
                    if (i < textLabels.size) stringResource(textLabels[i]) else stringResource(R.string.t_line, i + 1),
                    { gmd.entries.getOrElse(i) { "" }.replace("\r\n", "\n") },
                    { s -> vm.edit { gmd.entries[i] = s.replace("\n", "\r\n") } },
                    single = single,
                )
            }
        }
        Hint(stringResource(R.string.text_hint))
    }
    if (askCopy) ConfirmDialog(
        stringResource(R.string.copy_to_all), stringResource(R.string.copy_to_all_body, Lang.names[lang]), stringResource(R.string.copy),
        {
            vm.edit {
                for (o in present) if (o != lang) q.text[o]!!.entries.let { t -> t.clear(); t.addAll(gmd.entries) }
            }
        },
        { askCopy = false },
    )
}

// rewards

@Composable
fun RewardsTab(vm: AppViewModel, q: QuestArc) {
    val g = vm.game
    var list by rememberSaveable { mutableIntStateOf(0) }
    val names = listOf(R.string.rem_main_a, R.string.rem_main_b, R.string.rem_extra_a, R.string.rem_extra_b, R.string.rem_sub).map { stringResource(it) }
    val rem = q.rems[list]
    var showFlags by remember { mutableStateOf(false) }
    val saveText = rememberCreator { u -> vm.exportText(u, TextLists.rewards(rem) { g.itemName(it) }) }
    val loadText = rememberOpener { u -> vm.importText(u) { rem.setRewards(TextLists.parseRewards(it)) } }
    Page {
        ChipBar(names, list, { list = it })
        val rewards = run { tick(); rem.rewards() }
        Section(names[list], action = {
            Text(stringResource(R.string.total_weight, rewards.sumOf { it.weight }), style = MaterialTheme.typography.labelLarge)
        }) {
            tick()
            if (rewards.isEmpty()) Hint(stringResource(R.string.no_rewards))
            rewards.forEachIndexed { i, r ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceField(
                        stringResource(R.string.f_item), g.items, { rem.rewards().getOrNull(i)?.item ?: 0 },
                        { v -> vm.edit { rem.setRewards(rem.rewards().toMutableList().also { it[i] = it[i].copy(item = v) }) } },
                        Modifier.weight(2.2f),
                    )
                    NumField(
                        stringResource(R.string.f_qty), { rem.rewards().getOrNull(i)?.amount ?: 0 },
                        { v -> vm.edit { rem.setRewards(rem.rewards().toMutableList().also { it[i] = it[i].copy(amount = v) }) } },
                        Modifier.weight(1f), 0, 255,
                    )
                    NumField(
                        stringResource(R.string.f_weight), { rem.rewards().getOrNull(i)?.weight ?: 0 },
                        // 255 marks the end of the list, can't be used as a weight
                        { v -> vm.edit { rem.setRewards(rem.rewards().toMutableList().also { it[i] = it[i].copy(weight = v) }) } },
                        Modifier.weight(1f), 0, 254,
                    )
                    IconButton(onClick = { vm.edit { rem.setRewards(rem.rewards().toMutableList().also { it.removeAt(i) }) } }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.remove))
                    }
                }
            }
            FilledTonalButton(
                onClick = { vm.edit { rem.setRewards(rem.rewards() + Rem.Reward(0, 1, 1)) } },
                enabled = rewards.size < Rem.MAX - 1,
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.add_reward))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { saveText(q.link.file(QuestLink.REM.first + list) + ".txt") }) { Text(stringResource(R.string.export_text)) }
                OutlinedButton(onClick = loadText) { Text(stringResource(R.string.import_text)) }
            }
        }
        Section(stringResource(R.string.sec_rem_flags), action = {
            TextButton(onClick = { showFlags = !showFlags }) { Text(stringResource(if (showFlags) R.string.hide else R.string.show)) }
        }) {
            if (showFlags) for (i in 0 until 8) FieldRow {
                vm.num(stringResource(R.string.f_flag, i + 1), { rem.flag(i) }, { rem.setFlag(i, it) }, Modifier.weight(1f))
                vm.num(stringResource(R.string.f_value), { rem.flagValue(i) }, { rem.setFlagValue(i, it) }, Modifier.weight(1f))
            }
        }
        Hint(stringResource(R.string.rewards_hint))
    }
}

// small monsters

@Composable
fun SmallMonstersTab(vm: AppViewModel, q: QuestArc) {
    var set by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Ems?>(null) }
    var askClear by remember { mutableStateOf(false) }
    var addArea by remember { mutableStateOf(false) }
    val esl = q.esls[set]
    val d = q.data
    val pickEsl = rememberOpener { vm.importEsl(it, set) }
    val g = vm.game
    Page {
        ChipBar((1..3).map { stringResource(R.string.esl_set, it) }, set, { set = it })
        tick()
        Hint(q.link.file(QuestLink.ESL.first + set).let { if (q.link.isEmpty(QuestLink.ESL.first + set)) stringResource(R.string.esl_new) else it })
        if (set > 0) Section(stringResource(R.string.wave_trigger, set + 1)) {
            FieldRow {
                vm.num(stringResource(R.string.f_type), { d.setType(set - 1) }, { d.setSetType(set - 1, it) }, Modifier.weight(1f), -128, 127)
                vm.num(stringResource(R.string.f_target), { d.setTarget(set - 1) }, { d.setSetTarget(set - 1, it) }, Modifier.weight(1f), 0, 65535)
                vm.num(stringResource(R.string.f_amount), { d.setCount(set - 1) }, { d.setSetCount(set - 1, it) }, Modifier.weight(1f), 0, 65535)
            }
        }
        esl.packs.forEachIndexed { slot, list ->
            if (list.isNotEmpty()) Section(stringResource(R.string.area_n, slot + 1), action = {
                IconButton(onClick = { vm.edit { list += Ems().also { it.area = slot + 1 } } }) { Icon(Icons.Default.Add, stringResource(R.string.add_monster)) }
                // leaves the slot empty, the other areas keep their slots
                IconButton(onClick = { vm.edit { list.clear() } }) { Icon(Icons.Default.Delete, stringResource(R.string.remove)) }
            }) {
                tick()
                list.forEachIndexed { i, e ->
                    val large = e.monster in 1 until 0x1000
                    ListItem(
                        modifier = Modifier.clickable { editing = e },
                        headlineContent = { Text(g.smallName(e.monster), color = if (large) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                        supportingContent = {
                            Text(stringResource(R.string.ems_summary, e.area, e.condition, e.pos(0).toInt(), e.pos(1).toInt(), e.pos(2).toInt()))
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { vm.edit { list.add(i - 1, list.removeAt(i)) } }, enabled = i > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up))
                                }
                                IconButton(onClick = { vm.edit { list.add(i + 1, list.removeAt(i)) } }, enabled = i < list.lastIndex) {
                                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down))
                                }
                                IconButton(onClick = { vm.edit { list.add(i + 1, e.copy()) } }) {
                                    Icon(Icons.Default.ContentCopy, stringResource(R.string.duplicate))
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { addArea = true }, enabled = esl.freeSlot() >= 0) { Text(stringResource(R.string.add_pack)) }
            OutlinedButton(onClick = pickEsl) { Text(stringResource(R.string.import_esl)) }
            if (!esl.isEmpty) TextButton(onClick = { askClear = true }) { Text(stringResource(R.string.clear_all)) }
        }
        Hint(stringResource(R.string.wave_hint))
        Hint(stringResource(R.string.small_hint))
    }
    if (addArea) {
        val free = esl.packs.indices.filter { esl.packs[it].isEmpty() }.map { Opt(it + 1, stringResource(R.string.area_n, it + 1)) }
        PickerDialog(stringResource(R.string.add_pack), free, -1, { area ->
            vm.edit { esl.packs[area - 1] += Ems().also { it.area = area } }
            addArea = false
        }) { addArea = false }
    }
    editing?.let { e ->
        // a changed area moves the monster to that area's slot once the dialog closes
        val close = { vm.edit { esl.regroup() }; editing = null }
        EmsDialog(vm, q, e, close, onDuplicate = {
            vm.edit { esl.packs.firstOrNull { e in it }?.let { w -> w.add(w.indexOf(e) + 1, e.copy()) }; esl.regroup() }
            editing = null
        }, onDelete = {
            vm.edit { esl.packs.forEach { it.remove(e) } }
            editing = null
        })
    }
    if (askClear) ConfirmDialog(
        stringResource(R.string.clear_all), stringResource(R.string.clear_all_body), stringResource(R.string.clear_all),
        { vm.edit { esl.packs.forEach { it.clear() } } }, { askClear = false },
    )
}

@Composable
private fun EmsDialog(vm: AppViewModel, q: QuestArc, e: Ems, onDismiss: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.small_monster)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.pick(stringResource(R.string.f_monster), vm.game.smallAndLarge, { e.monster }, { e.monster = it })
                if (run { tick(); e.monster in 1 until 0x1000 }) {
                    Text(stringResource(R.string.ems_large_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Hint(stringResource(R.string.ems_large_tip))
                }
                FieldRow {
                    vm.num(stringResource(R.string.f_area), { e.area }, { e.area = it }, Modifier.weight(1f))
                    vm.num(stringResource(R.string.f_condition), { e.condition }, { e.condition = it }, Modifier.weight(1f), -128, 127)
                }
                OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Place, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.pick_spawn))
                }
                FieldRow {
                    FloatField("X", { e.pos(0) }, { v -> vm.edit { e.setPos(0, v) } }, Modifier.weight(1f))
                    FloatField("Y", { e.pos(1) }, { v -> vm.edit { e.setPos(1, v) } }, Modifier.weight(1f))
                }
                FieldRow {
                    FloatField("Z", { e.pos(2) }, { v -> vm.edit { e.setPos(2, v) } }, Modifier.weight(1f))
                    FloatField(stringResource(R.string.f_angle), { e.angle }, { v -> vm.edit { e.angle = v } }, Modifier.weight(1f))
                }
                Text(stringResource(R.string.unknown_values), style = MaterialTheme.typography.labelLarge)
                FieldRow {
                    vm.num("1", { e.unk1(0) }, { e.setUnk1(0, it) }, Modifier.weight(1f))
                    vm.num("2", { e.unk1(1) }, { e.setUnk1(1, it) }, Modifier.weight(1f))
                }
                FieldRow {
                    vm.num("3", { e.unk2(0) }, { e.setUnk2(0, it) }, Modifier.weight(1f), 0, 65535)
                    vm.num("4", { e.unk2(1) }, { e.setUnk2(1, it) }, Modifier.weight(1f), 0, 65535)
                }
                FieldRow {
                    vm.num("5", { e.unk3(0) }, { e.setUnk3(0, it) }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                    vm.num("6", { e.unk3(1) }, { e.setUnk3(1, it) }, Modifier.weight(1f), Int.MIN_VALUE, I32)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.remove)) }
                TextButton(onClick = onDuplicate) { Text(stringResource(R.string.duplicate)) }
            }
        },
    )
    if (picking) MapPickerDialog(vm, q.data.map % 100, e.area, { p ->
        vm.edit {
            e.area = p.area; e.setPos(0, p.x); e.setPos(1, p.y); e.setPos(2, p.z)
            p.angle?.let { e.angle = it }
        }
        picking = false
    }) { picking = false }
}

// large monster spawn points

@Composable
fun LargeMonstersTab(vm: AppViewModel, q: QuestArc) {
    var presetFor by remember { mutableStateOf(-1) }
    Page {
        for (i in 0 until 5) {
            val name = run { tick(); q.data.monsters[i].id.let { if (it == 0) null else vm.game.monsterName(it) } }
            Section(stringResource(R.string.monster_n, i + 1) + (name?.let { " · $it" } ?: ""), action = {
                IconButton(onClick = { presetFor = i }) { Icon(Icons.Default.Place, stringResource(R.string.pick_spawn)) }
                // same point for every slot that has a monster, so they all start in one area
                IconButton(onClick = {
                    vm.edit { for (o in 0 until 5) if (o != i && q.data.monsters[o].id != 0) q.spawns[o] = q.spawns[i] }
                }) { Icon(Icons.Default.ContentCopy, stringResource(R.string.spawn_all)) }
                IconButton(onClick = { vm.edit { q.spawns[i] = Spawn() } }) { Icon(Icons.Default.Clear, stringResource(R.string.clear)) }
            }) {
                tick()
                val sp = q.spawns[i]
                Hint(if (sp.isEmpty) stringResource(R.string.spawn_unused) else q.link.file(i).takeIf { !q.link.isEmpty(i) } ?: stringResource(R.string.spawn_new))
                FieldRow {
                    vm.num(stringResource(R.string.f_area), { q.spawns[i].area }, { q.spawns[i] = q.spawns[i].copy(area = it) }, Modifier.weight(1f), 0, I32)
                    vm.num(stringResource(R.string.f_round), { q.spawns[i].round }, { q.spawns[i] = q.spawns[i].copy(round = it) }, Modifier.weight(1f), 0, I32)
                    FloatField(stringResource(R.string.f_angle), { q.spawns[i].angle }, { v -> vm.edit { q.spawns[i] = q.spawns[i].copy(angle = v) } }, Modifier.weight(1f))
                }
                FieldRow {
                    FloatField("X", { q.spawns[i].x }, { v -> vm.edit { q.spawns[i] = q.spawns[i].copy(x = v) } }, Modifier.weight(1f))
                    FloatField("Y", { q.spawns[i].y }, { v -> vm.edit { q.spawns[i] = q.spawns[i].copy(y = v) } }, Modifier.weight(1f))
                    FloatField("Z", { q.spawns[i].z }, { v -> vm.edit { q.spawns[i] = q.spawns[i].copy(z = v) } }, Modifier.weight(1f))
                }
            }
        }
        Hint(stringResource(R.string.large_hint))
    }
    if (presetFor >= 0) {
        val slot = presetFor
        MapPickerDialog(vm, q.data.map % 100, q.spawns[slot].area, { p ->
            vm.edit {
                val old = q.spawns[slot]
                q.spawns[slot] = old.copy(area = p.area, x = p.x, y = p.y, z = p.z, angle = p.angle ?: old.angle)
            }
            presetFor = -1
        }) { presetFor = -1 }
    }
}

// supply box (.sup)

@Composable
fun SuppliesTab(vm: AppViewModel, q: QuestArc) {
    val sup = q.supply
    val export = rememberCreator { u -> sup?.let { vm.exportRaw(u, it.raw) } }
    val pickFile = rememberOpener { u -> sup?.let { vm.importRaw(u, it.raw) } }
    val saveText = rememberCreator { u -> sup?.let { vm.exportText(u, TextLists.supply(it) { id -> vm.game.itemName(id) }) } }
    val loadText = rememberOpener { u -> sup?.let { s -> vm.importText(u) { TextLists.applySupply(it, s) } } }
    if (sup == null) { Page { Hint(stringResource(R.string.no_supply)) }; return }
    val d = q.data
    Page {
        Hint(stringResource(R.string.supply_hint))
        for (i in 0 until 2) SupplySetSection(vm, d, i)
        val sections = run { tick(); sup.sections() }
        sections.forEach { (num, range) ->
            val users = run { tick(); (0 until 2).filter { d.suppLabel(it) == num }.map { 'A' + it } }
            val usedBy = users.map { stringResource(R.string.supply_used_by, it) }
            val title = stringResource(R.string.slots_range, num, range.first + 1, range.last + 1) +
                if (usedBy.isNotEmpty()) " · " + usedBy.joinToString() else ""
            Section(title) {
                for (s in range) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceField(stringResource(R.string.slot_n, s + 1), vm.game.items, { sup.item(s) }, { v -> vm.edit { sup.setItem(s, v) } }, Modifier.weight(2.4f))
                    NumField(stringResource(R.string.f_qty), { sup.count(s) }, { v -> vm.edit { sup.setCount(s, v) } }, Modifier.weight(1f))
                    // slot 1 is always the start of section 0, its marker stays 0
                    if (s > 0) NumField(stringResource(R.string.f_section), { sup.marker(s) }, { v -> vm.edit { sup.setMarker(s, v) } }, Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { saveText(q.link.file(QuestLink.SUPP) + ".txt") }) { Text(stringResource(R.string.export_text)) }
            OutlinedButton(onClick = loadText) { Text(stringResource(R.string.import_text)) }
        }
        RawButtons(q.link.file(QuestLink.SUPP), export, pickFile)
    }
}

@Composable
private fun RawButtons(name: String, export: (String) -> Unit, pickFile: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { export(name.ifEmpty { "file" }) }) { Text(stringResource(R.string.export_file)) }
        OutlinedButton(onClick = pickFile) { Text(stringResource(R.string.import_file)) }
    }
}

// questplus (.qdp)

@Composable
fun PlusTab(vm: AppViewModel, q: QuestArc) {
    val plus = q.plus
    val export = rememberCreator { u -> plus?.let { vm.exportRaw(u, it.raw) } }
    val pickFile = rememberOpener { u -> plus?.let { vm.importRaw(u, it.raw) } }
    if (plus == null) { Page { Hint(stringResource(R.string.no_plus)) }; return }

    @Composable
    fun b8(label: String, o: Int, seen: String?, m: Modifier) = Column(m) {
        NumField(label, { plus.u8(o) }, { v -> vm.edit { plus.setU8(o, v) } }, Modifier.fillMaxWidth())
        if (seen != null) Hint(stringResource(R.string.qp_seen, seen))
    }

    @Composable
    fun b16(label: String, o: Int, seen: String?, m: Modifier) = Column(m) {
        NumField(label, { plus.u16(o) }, { v -> vm.edit { plus.setU16(o, v) } }, Modifier.fillMaxWidth(), 0, 65535)
        if (seen != null) Hint(stringResource(R.string.qp_seen, seen))
    }

    Page {
        Hint(stringResource(R.string.plus_hint))
        Section(stringResource(R.string.sec_qp_arena)) {
            FieldRow { b8(stringResource(R.string.qp_fence), 0x00, "0, 1", Modifier.weight(1f)) }
            FieldRow {
                b16(stringResource(R.string.qp_fence_t1), 0x02, "90, 40", Modifier.weight(1f))
                b16(stringResource(R.string.qp_fence_t2), 0x06, "20", Modifier.weight(1f))
            }
        }
        Section(stringResource(R.string.sec_qp_fort)) {
            FieldRow {
                b8(stringResource(R.string.qp_barricades), 0x01, "0, 1", Modifier.weight(1f))
                b8(stringResource(R.string.qp_siege), 0x08, "0, 1", Modifier.weight(1f))
            }
            FieldRow {
                b16(stringResource(R.string.qp_front_hp), 0x0D, "600", Modifier.weight(1f))
                b16(stringResource(R.string.qp_main_hp), 0x0F, "600", Modifier.weight(1f))
            }
            FieldRow { b16(stringResource(R.string.qp_weapon_timer), 0x0B, "600", Modifier.weight(1f)) }
        }
        Section(stringResource(R.string.unknown_values)) {
            FieldRow {
                b16(stringResource(R.string.qp_unknown, "0x04"), 0x04, "0", Modifier.weight(1f))
                b16(stringResource(R.string.qp_unknown, "0x09"), 0x09, "0", Modifier.weight(1f))
            }
            tick()
            Text(
                q.link.file(QuestLink.PLUS).let { "questPlus_$it  " } + (0 until QuestPlus.SIZE).joinToString(" ") { "%02x".format(plus.u8(it)) },
                fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
            )
        }
        RawButtons("questPlus_" + q.link.file(QuestLink.PLUS), export, pickFile)
    }
}

// quest name and short text up front, the rest lives on the Text tab

@Composable
private fun QuestNameSection(vm: AppViewModel, q: QuestArc) {
    val lang = appLang(q)
    val gmd = q.text[lang] ?: return
    Section(stringResource(R.string.sec_name) + " · " + Lang.names[lang]) {
        tick()
        // every language is its own gmd in the arc, this switches which one the fields below edit
        ChipBar(Lang.names, lang, { vm.changeQuestLang(it) }, { q.text[it] != null })
        for (i in listOf(0, 1, 2, 4)) if (i < gmd.entries.size) TextArea(
            stringResource(textLabels[i]),
            { gmd.entries[i].replace("\r\n", "\n") },
            { s -> vm.edit { gmd.entries[i] = s.replace("\n", "\r\n") } },
            single = i < 2,
        )
        Hint(stringResource(R.string.name_hint))
    }
}

/** game language matching the phone's, falling back to the first one the quest has */
@Composable
fun appLang(q: QuestArc?): Int {
    val loc = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val picked = LocalQuestLang.current
    val want = if (picked in 0..6) picked else when (loc.language) {
        "fr" -> 1; "de" -> 2; "it" -> 3; "es" -> 4
        "zh" -> if (loc.country in listOf("TW", "HK", "MO") || loc.script == "Hant") 5 else 6
        else -> 0
    }
    if (q == null || q.text.getOrNull(want) != null) return want
    return (0 until 7).firstOrNull { q.text[it] != null } ?: 0
}

// extra large monsters, see Extras in Quest.kt

@Composable
private fun ExtrasSection(vm: AppViewModel, q: QuestArc) {
    val g = vm.game
    val d = q.data
    var id by remember { mutableIntStateOf(d.monsters.firstOrNull { it.id != 0 }?.id ?: 1) }
    var area by remember { mutableIntStateOf(q.spawns.firstOrNull { !it.isEmpty }?.area?.coerceIn(1, 14) ?: 1) }
    var copies by remember { mutableIntStateOf(1) }
    Section(stringResource(R.string.sec_extras)) {
        tick()
        Hint(stringResource(R.string.extras_hint))
        // what's there now, per monster
        val present = q.esls.flatMap { w -> w.packs.flatten() }.map { it.monster }.filter { Extras.isLarge(it) }.distinct()
        for (m in present) {
            val (slots, waves) = Extras.count(q, m)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.extras_line, waves, g.monsterName(m)), style = MaterialTheme.typography.bodyLarge)
                    Hint(stringResource(R.string.obj_count, slots + waves, slots, waves))
                }
                TextButton(onClick = { vm.edit { Extras.remove(q, m) } }) { Text(stringResource(R.string.remove)) }
            }
        }
        vm.pick(stringResource(R.string.f_monster), g.monsters.filter { it.value != 0 }, { id }, { id = it })
        FieldRow {
            NumField(stringResource(R.string.f_area), { area }, { area = it }, Modifier.weight(1f), 1, 14)
            NumField(stringResource(R.string.extras_copies), { copies }, { copies = it }, Modifier.weight(1f), 1, 30)
        }
        Button(onClick = {
            val spots = g.positions(d.map % 100, area)
            vm.edit { Extras.add(q, id, area, copies, spots) }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.extras_add, copies, g.monsterName(id)))
        }
        Hint(stringResource(R.string.extras_after))
    }
}
