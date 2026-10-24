package com.rique.gramap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import gramap.shared.generated.resources.Res
import gramap.shared.generated.resources.action_continue
import gramap.shared.generated.resources.action_go_to_current
import gramap.shared.generated.resources.action_retry
import gramap.shared.generated.resources.back_to_worlds
import gramap.shared.generated.resources.feedback_lesson_unavailable
import gramap.shared.generated.resources.feedback_world_list_unavailable
import gramap.shared.generated.resources.stage_accessibility_completed
import gramap.shared.generated.resources.stage_accessibility_current
import gramap.shared.generated.resources.stage_accessibility_locked
import gramap.shared.generated.resources.block_label
import gramap.shared.generated.resources.stage_completed_detail
import gramap.shared.generated.resources.stage_completed_title
import gramap.shared.generated.resources.stage_current_detail
import gramap.shared.generated.resources.stage_locked_detail
import gramap.shared.generated.resources.stage_locked_title
import gramap.shared.generated.resources.stage_number
import gramap.shared.generated.resources.stage_topic_title
import gramap.shared.generated.resources.stage_world_progress
import gramap.shared.generated.resources.world_subtitle
import gramap.shared.generated.resources.world_title

private val Ink = Color(0xFF29231F)
private val Cream = Color(0xFFFFF9F1)
private val SkyTop = Color(0xFFA8DFF2)
private val SkyBottom = Color(0xFFE6F6FB)
private val Orange = Color(0xFFFF7843)
private val Teal = Color(0xFF2AB5A8)
private val Blue = Color(0xFF5F7FD0)

private enum class StageStatus { COMPLETED, CURRENT, LOCKED }

private data class WorldStage(
    val number: Int,
    val topic: String? = null,
    val status: StageStatus = StageStatus.LOCKED,
)

private val previewStages = listOf(
    WorldStage(1, status = StageStatus.COMPLETED),
    WorldStage(2, topic = "Acentuação", status = StageStatus.CURRENT),
) + (3..12).map { WorldStage(it) }

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun App(
    onBackToWorlds: (() -> Unit)? = null,
    onOpenLesson: ((stageNumber: Int, onCompleted: () -> Unit) -> Unit)? = null,
) {
    MaterialTheme {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val mapScrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        val lessonUnavailableMessage = stringResource(Res.string.feedback_lesson_unavailable)
        val worldListUnavailableMessage = stringResource(Res.string.feedback_world_list_unavailable)
        var stages by remember { mutableStateOf(previewStages) }
        var selectedStageNumber by rememberSaveable { mutableIntStateOf(2) }
        var blockedStageNumber by rememberSaveable { mutableIntStateOf(0) }
        var lockedTapSequence by remember { mutableIntStateOf(0) }
        val currentStageIndex = stages.indexOfFirst { it.status == StageStatus.CURRENT }.coerceAtLeast(0)
        val currentStage = stages[currentStageIndex]
        val selectedStage = stages.firstOrNull { it.number == selectedStageNumber } ?: currentStage
        val blockedStage = stages.firstOrNull { it.number == blockedStageNumber }
        val cardStage = blockedStage ?: selectedStage
        val completedCount = stages.count { it.status == StageStatus.COMPLETED }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Cream,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(Res.string.world_title), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(Res.string.world_subtitle), color = Color(0xFF6F6258), fontSize = 14.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (onBackToWorlds != null) onBackToWorlds()
                                else coroutineScope.launch {
                                    snackbarHostState.showSnackbar(worldListUnavailableMessage)
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(15.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0E2D6)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back_to_worlds), tint = Ink)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Column(
                    Modifier.fillMaxWidth().background(Cream).navigationBarsPadding()
                ) {
                    ProgressPanel(
                        stage = cardStage,
                        isBlocked = blockedStage != null,
                        blockedBy = (blockedStage?.number ?: 0) - 1,
                        completedCount = completedCount,
                        totalCount = stages.size,
                        onAction = {
                            val launchStage = selectedStage
                            if (onOpenLesson != null) {
                                onOpenLesson(launchStage.number) {
                                    if (launchStage.status == StageStatus.CURRENT && stages.getOrNull(launchStage.number - 1)?.status == StageStatus.CURRENT) {
                                        stages = stages.map { stage ->
                                            when (stage.number) {
                                                launchStage.number -> stage.copy(status = StageStatus.COMPLETED)
                                                launchStage.number + 1 -> stage.copy(status = StageStatus.CURRENT)
                                                else -> stage
                                            }
                                        }
                                        selectedStageNumber = (launchStage.number + 1).coerceAtMost(stages.size)
                                        blockedStageNumber = 0
                                    }
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(lessonUnavailableMessage)
                                }
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
                val viewportHeight = maxHeight
                val contentHeight = mapContentHeight(stages.size, viewportHeight)
                val density = LocalDensity.current
                val currentStageTopPx = with(density) { viewportHeight.toPx() * stageTop(currentStageIndex, stages.size) }
                val viewportHeightPx = with(density) { viewportHeight.toPx() }
                val showCurrentShortcut by remember(currentStageTopPx, viewportHeightPx) {
                    derivedStateOf {
                        val currentBottom = currentStageTopPx + with(density) { 110.dp.toPx() }
                        currentBottom < mapScrollState.value || currentStageTopPx > mapScrollState.value + viewportHeightPx
                    }
                }
                LaunchedEffect(currentStageIndex, viewportHeightPx) {
                    val target = (currentStageTopPx - viewportHeightPx * .66f).coerceAtLeast(0f).toInt()
                    if (target > 0) mapScrollState.animateScrollTo(target.coerceAtMost(mapScrollState.maxValue))
                }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().verticalScroll(mapScrollState)) {
                    IslandMap(
                        stages = stages,
                        viewportHeight = viewportHeight,
                        modifier = Modifier.fillMaxWidth().height(contentHeight),
                        onStageClick = { stage ->
                            if (stage.status == StageStatus.LOCKED) {
                                blockedStageNumber = stage.number
                                lockedTapSequence++
                            } else {
                                selectedStageNumber = stage.number
                                blockedStageNumber = 0
                            }
                        },
                        selectedStageNumber = selectedStage.number,
                        lockedStageNumber = blockedStageNumber,
                        lockedTapSequence = lockedTapSequence,
                    )
                    }
                    AnimatedVisibility(
                        visible = showCurrentShortcut,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).zIndex(1f),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                coroutineScope.launch {
                                    val target = (currentStageTopPx - viewportHeightPx * .66f).coerceAtLeast(0f).toInt()
                                    mapScrollState.animateScrollTo(target.coerceAtMost(mapScrollState.maxValue))
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(stringResource(Res.string.action_go_to_current))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandMap(
    stages: List<WorldStage>,
    viewportHeight: androidx.compose.ui.unit.Dp,
    selectedStageNumber: Int,
    lockedStageNumber: Int,
    lockedTapSequence: Int,
    onStageClick: (WorldStage) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        val w = maxWidth
        val currentPulse = rememberInfiniteTransition(label = "current-stage-pulse")
        val currentHaloScale by currentPulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.24f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "current-stage-halo-scale",
        )
        val currentHaloAlpha by currentPulse.animateFloat(
            initialValue = .42f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "current-stage-halo-alpha",
        )
        val bandCount = (stages.size + 3) / 4
        repeat(bandCount) { band ->
            val y = viewportHeight * band
            SvgAsset("nuvem", Modifier.offset((-20).dp, y + viewportHeight * .035f).width(w * .19f).height(w * .11f))
            SvgAsset("nuvem", Modifier.offset(w * .72f, y + viewportHeight * .01f).width(w * .25f).height(w * .14f))
            SvgAsset("nuvem", Modifier.offset(w * .77f, y + viewportHeight * .48f).width(w * .22f).height(w * .13f))
            SvgAsset("nuvem", Modifier.offset((-w.value * .02f).dp, y + viewportHeight * .53f).width(w * .21f).height(w * .12f))
            SvgAsset("nuvem", Modifier.offset((-w.value * .02f).dp, y + viewportHeight * .86f).width(w * .22f).height(w * .13f))
            SvgAsset("nuvem", Modifier.offset(w * .70f, y + viewportHeight * .89f).width(w * .29f).height(w * .16f))
        }

        stages.indices.filter { it > 0 && it % 4 == 0 }.forEach { blockStart ->
            val dividerY = (stageTop(blockStart - 1, stages.size) + stageTop(blockStart, stages.size)) / 2f
            Text(
                stringResource(Res.string.block_label, blockStart / 4 + 1),
                Modifier.offset(18.dp, viewportHeight * dividerY).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = .88f)).padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color(0xFF527789), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            )
        }

        val routeProgress = stages.dropLast(1).mapIndexed { index, stage ->
            val litFraction by animateFloatAsState(
                targetValue = if (stage.status == StageStatus.COMPLETED) 1f else 0f,
                animationSpec = tween(if (stage.status == StageStatus.COMPLETED) 850 else 180),
                label = "route-progress-$index",
            )
            litFraction
        }

        Canvas(Modifier.fillMaxSize()) {
            if (stages.size > 1) {
                val centers = stages.indices.map { index ->
                    Offset(
                        x = size.width * stageCenterX(index),
                        y = with(this) { viewportHeight.toPx() * stageTop(index, stages.size) + 36.dp.toPx() },
                    )
                }
                val path = Path().apply {
                    moveTo(centers.first().x, centers.first().y)
                    centers.zipWithNext().forEach { (start, end) ->
                        val middleY = (start.y + end.y) / 2f
                        cubicTo(start.x, middleY, end.x, middleY, end.x, end.y)
                    }
                }
                drawPath(path, Color.White.copy(alpha = .96f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color(0xFF91C7D4), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                stages.zipWithNext().forEachIndexed { index, _ ->
                    if (routeProgress[index] > 0f) {
                        val from = centers[index]
                        val to = centers[index + 1]
                        val litSegment = Path().apply {
                            moveTo(from.x, from.y)
                            val middleY = (from.y + to.y) / 2f
                            cubicTo(from.x, middleY, to.x, middleY, to.x, to.y)
                        }
                        drawPath(litSegment, Teal.copy(alpha = routeProgress[index]), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
        }

        // The star island is decorative; phase islands are generated from the supplied list.
        Island(Modifier.offset(w * .61f, viewportHeight * .15f).width(w * .34f).height(viewportHeight * .20f))
        Text("☆", Modifier.offset(w * .72f, viewportHeight * .115f), color = Color(0xFFF4A900), fontSize = 39.sp, fontWeight = FontWeight.Bold)

        stages.forEachIndexed { index, _ ->
            val islandWidth = w * islandWidthFraction(index)
            val islandHeight = viewportHeight * islandHeightFraction(index)
            val top = viewportHeight * stageTop(index, stages.size)
            Island(
                Modifier.offset(w * stageCenterX(index) - islandWidth / 2, top)
                    .width(islandWidth).height(islandHeight)
            )
        }

        stages.forEachIndexed { index, stage ->
            val markerSize = if (stage.status == StageStatus.CURRENT) 76.dp else 66.dp
            StageMarker(
                stage = stage,
                selected = stage.number == selectedStageNumber,
                haloScale = currentHaloScale,
                haloAlpha = currentHaloAlpha,
                shakeSequence = if (stage.number == lockedStageNumber) lockedTapSequence else 0,
                onClick = { onStageClick(stage) },
                modifier = Modifier.offset(
                    x = w * stageCenterX(index) - markerSize / 2,
                    y = viewportHeight * stageTop(index, stages.size),
                ),
            )
        }
    }
}

private fun mapContentHeight(stageCount: Int, viewportHeight: androidx.compose.ui.unit.Dp) =
    viewportHeight * (1f + (stageCount - 4).coerceAtLeast(0) * .20f)

private fun stageCenterX(index: Int): Float = listOf(.30f, .575f, .27f, .71f)[index % 4]

private fun stageTop(index: Int, stageCount: Int): Float =
    if (stageCount <= 4) {
        listOf(.79f, .66f, .46f, .19f)[index.coerceIn(0, 3)]
    } else {
        .19f + .20f * (stageCount - 1 - index)
    }

private fun islandWidthFraction(index: Int): Float = listOf(.46f, .39f, .43f, .31f)[index % 4]

private fun islandHeightFraction(index: Int): Float = listOf(.25f, .22f, .22f, .17f)[index % 4]

@Composable
private fun Island(modifier: Modifier) {
    SvgAsset("ilha_a", modifier)
}

@Composable
private fun StageMarker(
    stage: WorldStage,
    selected: Boolean,
    haloScale: Float,
    haloAlpha: Float,
    shakeSequence: Int,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val color = when {
        stage.status == StageStatus.CURRENT -> Orange
        stage.status == StageStatus.COMPLETED -> Teal
        else -> Blue
    }
    val markerSize = if (stage.status == StageStatus.CURRENT) 76.dp else 66.dp
    val selectedScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "stage-selection-scale",
    )
    val shakeX = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(shakeSequence) {
        if (shakeSequence > 0) {
            listOf(-8f, 8f, -5f, 5f, 0f).forEach { offset ->
                shakeX.animateTo(with(density) { offset.dp.toPx() }, tween(42))
            }
        }
    }
    val announcement = when (stage.status) {
        StageStatus.COMPLETED -> stringResource(Res.string.stage_accessibility_completed, stage.number)
        StageStatus.CURRENT -> stringResource(Res.string.stage_accessibility_current, stage.number)
        StageStatus.LOCKED -> stringResource(Res.string.stage_accessibility_locked, stage.number, stage.number - 1)
    }
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = announcement; role = Role.Button }
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(markerSize).graphicsLayer {
                scaleX = selectedScale
                scaleY = selectedScale
                translationX = shakeX.value
            },
            contentAlignment = Alignment.Center,
        ) {
            if (stage.status == StageStatus.CURRENT) {
                Box(
                    Modifier.size(markerSize).scale(haloScale)
                        .border(2.dp, Orange.copy(alpha = haloAlpha), CircleShape)
                )
            }
            if (selected) {
                Box(Modifier.size(markerSize + 10.dp).border(2.dp, Color(0xFFFFD46A), CircleShape))
            }
            Box(
                Modifier.size(markerSize - 8.dp).shadow(2.dp, CircleShape).clip(CircleShape)
                    .background(Color.White).padding(5.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = stage.status, animationSpec = tween(180), label = "stage-state-icon") { status ->
                    when (status) {
                        StageStatus.COMPLETED -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(31.dp))
                        StageStatus.CURRENT -> Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        StageStatus.LOCKED -> Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        Text(
            stringResource(Res.string.stage_number, stage.number),
            Modifier.padding(top = 6.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
                .padding(horizontal = 12.dp, vertical = 3.dp),
            color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ProgressPanel(
    stage: WorldStage,
    isBlocked: Boolean,
    blockedBy: Int,
    completedCount: Int,
    totalCount: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().background(Cream).padding(top = 18.dp)
            .shadow(2.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Crossfade(targetState = stage to isBlocked, animationSpec = tween(200), label = "selected-stage-card") { (shownStage, shownBlocked) ->
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val title = when {
                        shownBlocked -> "${stringResource(Res.string.stage_number, shownStage.number)} · ${stringResource(Res.string.stage_locked_title)}"
                        shownStage.status == StageStatus.COMPLETED -> "${stringResource(Res.string.stage_number, shownStage.number)} · ${stringResource(Res.string.stage_completed_title)}"
                        shownStage.topic != null -> stringResource(Res.string.stage_topic_title, shownStage.number, shownStage.topic)
                        else -> stringResource(Res.string.stage_number, shownStage.number)
                    }
                    Text(title, Modifier.weight(1f), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (totalCount == 0) 0f else completedCount.toFloat() / totalCount },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Teal,
                    trackColor = Color(0xFFEDE7E1),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when {
                        shownBlocked -> stringResource(Res.string.stage_locked_detail, blockedBy)
                        shownStage.status == StageStatus.COMPLETED -> stringResource(Res.string.stage_completed_detail)
                        else -> stringResource(Res.string.stage_current_detail)
                    },
                    color = Color(0xFF6F6258), fontSize = 13.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (!isBlocked) {
            Button(
                onClick = onAction, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Ink),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
            ) {
                Text(
                    text = stringResource(if (stage.status == StageStatus.COMPLETED) Res.string.action_retry else Res.string.action_continue),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(stringResource(Res.string.stage_world_progress, completedCount, totalCount), color = Color(0xFF6F6258), fontSize = 12.sp)
        }
    }
}
