package com.example.ytdlp

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ytdlp.download.DownloadFileActions
import com.example.ytdlp.download.DownloadHistoryItem
import com.example.ytdlp.download.DownloadHistoryRepository
import com.example.ytdlp.download.DownloadThumbnailLoader
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private data class DeleteDialogStrings(
    val title: String,
    val singleMessage: String,
    val multipleMessage: String,
    val cancel: String,
    val confirm: String
)

private data class QuickDeleteDialogStrings(
    val title: String,
    val message: String,
    val removeFromHistory: String,
    val deleteFromDevice: String,
    val cancel: String
)

@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val layoutDirection = LocalLayoutDirection.current

    val deleteDialogStrings =
        DeleteDialogStrings(
            title = stringResource(R.string.delete_from_device),
            singleMessage = stringResource(
                R.string.delete_device_confirmation_single
            ),
            multipleMessage = stringResource(
                R.string.delete_device_confirmation_multiple,
                0
            ),
            cancel = stringResource(R.string.cancel),
            confirm = stringResource(R.string.delete_from_device)
        )

    val quickDeleteDialogStrings =
        QuickDeleteDialogStrings(
            title = stringResource(R.string.quick_delete_title),
            message = stringResource(R.string.quick_delete_message),
            removeFromHistory = stringResource(
                R.string.delete_from_history
            ),
            deleteFromDevice = stringResource(
                R.string.delete_from_device
            ),
            cancel = stringResource(R.string.cancel)
        )

    LaunchedEffect(Unit) {
        DownloadHistoryRepository.initialize(
            context.applicationContext
        )
    }

    val historyItems by
    DownloadHistoryRepository.items.collectAsState()

    var selectionMode by remember {
        mutableStateOf(false)
    }

    var selectedIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var showDeviceDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var quickDeleteItem by remember {
        mutableStateOf<DownloadHistoryItem?>(null)
    }

    var quickDeviceDeleteItem by remember {
        mutableStateOf<DownloadHistoryItem?>(null)
    }

    fun leaveSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(
        item: DownloadHistoryItem
    ) {
        selectedIds =
            if (item.id in selectedIds) {
                selectedIds - item.id
            } else {
                selectedIds + item.id
            }
    }

    LaunchedEffect(historyItems) {
        val existingIds =
            historyItems.map { it.id }.toSet()

        selectedIds =
            selectedIds.intersect(existingIds)

        if (quickDeleteItem?.id !in existingIds) {
            quickDeleteItem = null
        }

        if (quickDeviceDeleteItem?.id !in existingIds) {
            quickDeviceDeleteItem = null
        }

        if (
            selectionMode &&
            historyItems.isEmpty()
        ) {
            leaveSelectionMode()
        }
    }

    quickDeleteItem?.let { item ->
        QuickDeleteGlassDialog(
            layoutDirection = layoutDirection,
            strings = quickDeleteDialogStrings,
            onDismiss = {
                quickDeleteItem = null
            },
            onRemoveFromHistory = {
                quickDeleteItem = null

                DownloadHistoryRepository.removeFromHistory(
                    context = context,
                    ids = setOf(item.id)
                )

                Toast.makeText(
                    context,
                    resources.getString(
                        R.string.deleted_from_history_single
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDeleteFromDevice = {
                quickDeleteItem = null
                quickDeviceDeleteItem = item
            }
        )
    }

    quickDeviceDeleteItem?.let { item ->
        DeleteDeviceGlassDialog(
            layoutDirection = layoutDirection,
            selectedCount = 1,
            strings = deleteDialogStrings,
            onDismiss = {
                quickDeviceDeleteItem = null
            },
            onConfirm = {
                val deleted =
                    DownloadFileActions.deleteFromDevice(
                        context = context,
                        item = item
                    )

                if (deleted) {
                    DownloadHistoryRepository.removeFromHistory(
                        context = context,
                        ids = setOf(item.id)
                    )

                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.deleted_from_device_single
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.delete_result_partial,
                            0,
                            1
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }

                quickDeviceDeleteItem = null
            }
        )
    }

    if (showDeviceDeleteConfirmation) {
        DeleteDeviceGlassDialog(
            layoutDirection = layoutDirection,
            selectedCount = selectedIds.size,
            strings =
                if (selectedIds.size <= 1) {
                    deleteDialogStrings
                } else {
                    deleteDialogStrings.copy(
                        multipleMessage =
                            resources.getString(
                                R.string.delete_device_confirmation_multiple,
                                selectedIds.size
                            )
                    )
                },
            onDismiss = {
                showDeviceDeleteConfirmation = false
            },
            onConfirm = {
                val selectedItems =
                    historyItems.filter {
                        it.id in selectedIds
                    }

                val successfullyDeletedIds =
                    mutableSetOf<String>()

                var failedCount = 0

                selectedItems.forEach { item ->
                    val deleted =
                        DownloadFileActions.deleteFromDevice(
                            context = context,
                            item = item
                        )

                    if (deleted) {
                        successfullyDeletedIds.add(item.id)
                    } else {
                        failedCount++
                    }
                }

                if (successfullyDeletedIds.isNotEmpty()) {
                    DownloadHistoryRepository.removeFromHistory(
                        context = context,
                        ids = successfullyDeletedIds
                    )
                }

                val deletedCount =
                    successfullyDeletedIds.size

                showDeviceDeleteConfirmation = false

                if (failedCount == 0) {
                    Toast.makeText(
                        context,
                        if (deletedCount == 1) {
                            resources.getString(
                                R.string.deleted_from_device_single
                            )
                        } else {
                            resources.getString(
                                R.string.deleted_from_device_multiple,
                                deletedCount
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    leaveSelectionMode()
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(
                            R.string.delete_result_partial,
                            deletedCount,
                            failedCount
                        ),
                        Toast.LENGTH_LONG
                    ).show()

                    selectedIds =
                        selectedIds - successfullyDeletedIds

                    if (selectedIds.isEmpty()) {
                        leaveSelectionMode()
                    }
                }
            }
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 18.dp)
    ) {
        if (selectionMode) {
            SelectionHeader(
                selectedCount = selectedIds.size,
                allSelected =
                    historyItems.isNotEmpty() &&
                            selectedIds.size == historyItems.size,
                onSelectAll = {
                    selectedIds =
                        if (
                            selectedIds.size ==
                            historyItems.size
                        ) {
                            emptySet()
                        } else {
                            historyItems
                                .map { it.id }
                                .toSet()
                        }
                },
                onClose = {
                    leaveSelectionMode()
                }
            )
        } else {
            NormalHeader(
                showDelete = historyItems.isNotEmpty(),
                onDeleteClick = {
                    selectionMode = true
                }
            )
        }

        if (
            selectionMode &&
            selectedIds.isNotEmpty()
        ) {
            DeleteActionsRow(
                onRemoveFromHistory = {
                    val idsToRemove = selectedIds

                    DownloadHistoryRepository.removeFromHistory(
                        context = context,
                        ids = idsToRemove
                    )

                    Toast.makeText(
                        context,
                        if (idsToRemove.size == 1) {
                            resources.getString(
                                R.string.deleted_from_history_single
                            )
                        } else {
                            resources.getString(
                                R.string.deleted_from_history_multiple,
                                idsToRemove.size
                            )
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    leaveSelectionMode()
                },
                onDeleteFromDevice = {
                    showDeviceDeleteConfirmation = true
                }
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )
        }

        if (historyItems.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyDownloadsCard()
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentPadding =
                    PaddingValues(bottom = 24.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = historyItems,
                    key = { it.id }
                ) { item ->
                    val selected =
                        item.id in selectedIds

                    DownloadHistoryCard(
                        item = item,
                        selectionMode = selectionMode,
                        selected = selected,
                        onSelect = {
                            toggleSelection(item)
                        },
                        onOpen = {
                            val opened =
                                DownloadFileActions.open(
                                    context = context,
                                    item = item
                                )

                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    resources.getString(
                                        R.string.file_open_failed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShare = {
                            val shared =
                                DownloadFileActions.share(
                                    context = context,
                                    item = item
                                )

                            if (!shared) {
                                Toast.makeText(
                                    context,
                                    resources.getString(
                                        R.string.file_share_failed
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onQuickDelete = {
                            quickDeleteItem = item
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDeleteGlassDialog(
    layoutDirection: LayoutDirection,
    strings: QuickDeleteDialogStrings,
    onDismiss: () -> Unit,
    onRemoveFromHistory: () -> Unit,
    onDeleteFromDevice: () -> Unit
) {
    SyDownSettingsDialog(
        onDismissRequest = onDismiss,
        layoutDirection = layoutDirection
    ) {
        SyDownGlassCard(
            modifier = Modifier.fillMaxWidth(),
            radius = 28.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 24.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                GlassRed.copy(alpha = 0.09f)
                            )
                            .syDownGlassBorder(
                                radius = 50.dp
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = GlassRed,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = strings.title,
                    color =
                        Color.White.copy(alpha = 0.96f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(9.dp)
                )

                Text(
                    text = strings.message,
                    color =
                        Color.White.copy(alpha = 0.58f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                QuickDeleteOptionButton(
                    text = strings.removeFromHistory,
                    icon = Icons.Rounded.Delete,
                    destructive = false,
                    onClick = onRemoveFromHistory
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                QuickDeleteOptionButton(
                    text = strings.deleteFromDevice,
                    icon = Icons.Rounded.DeleteForever,
                    destructive = true,
                    onClick = onDeleteFromDevice
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                GlassDialogButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = strings.cancel,
                    destructive = false,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun QuickDeleteOptionButton(
    text: String,
    icon: ImageVector,
    destructive: Boolean,
    onClick: () -> Unit
) {
    val radius = 16.dp

    val accent =
        if (destructive) {
            GlassRed
        } else {
            Color.White
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(
                    RoundedCornerShape(radius)
                )
                .background(
                    if (destructive) {
                        GlassRed.copy(alpha = 0.08f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }
                )
                .syDownGlassBorder(
                    radius = radius
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent.copy(alpha = 0.92f),
            modifier = Modifier.size(19.dp)
        )

        Text(
            text = text,
            color = accent.copy(alpha = 0.92f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeleteDeviceGlassDialog(
    layoutDirection: LayoutDirection,
    selectedCount: Int,
    strings: DeleteDialogStrings,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    SyDownSettingsDialog(
        onDismissRequest = onDismiss,
        layoutDirection = layoutDirection
    ) {
        SyDownGlassCard(
            modifier = Modifier.fillMaxWidth(),
            radius = 28.dp,
            strong = true
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 24.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                GlassRed.copy(alpha = 0.10f)
                            )
                            .syDownGlassBorder(
                                radius = 50.dp
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = GlassRed,
                        modifier = Modifier.size(29.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(17.dp)
                )

                Text(
                    text = strings.title,
                    color =
                        Color.White.copy(alpha = 0.96f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        if (selectedCount == 1) {
                            strings.singleMessage
                        } else {
                            strings.multipleMessage
                        },
                    color =
                        Color.White.copy(alpha = 0.58f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(
                    modifier = Modifier.height(22.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    GlassDialogButton(
                        modifier = Modifier.weight(1f),
                        text = strings.cancel,
                        destructive = false,
                        onClick = onDismiss
                    )

                    GlassDialogButton(
                        modifier = Modifier.weight(1f),
                        text = strings.confirm,
                        destructive = true,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassDialogButton(
    modifier: Modifier = Modifier,
    text: String,
    destructive: Boolean,
    onClick: () -> Unit
) {
    val radius = 16.dp

    val accent =
        if (destructive) {
            GlassRed
        } else {
            GlassGreen
        }

    Box(
        modifier =
            modifier
                .height(47.dp)
                .clip(
                    RoundedCornerShape(radius)
                )
                .background(
                    accent.copy(
                        alpha =
                            if (destructive) {
                                0.11f
                            } else {
                                0.07f
                            }
                    )
                )
                .syDownGlassBorder(
                    radius = radius
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accent.copy(alpha = 0.96f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NormalHeader(
    showDelete: Boolean,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 18.dp,
                    bottom = 16.dp
                )
                .height(42.dp)
    ) {
        Text(
            modifier =
                Modifier.align(Alignment.Center),
            text =
                stringResource(
                    R.string.download_history
                ),
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        if (showDelete) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            GlassRed.copy(alpha = 0.09f)
                        )
                        .clickable(
                            onClick = onDeleteClick
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription =
                        stringResource(R.string.delete),
                    tint =
                        GlassRed.copy(alpha = 0.92f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 18.dp,
                    bottom = 16.dp
                )
                .height(42.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(alpha = 0.045f)
                    )
                    .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint =
                    Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            text =
                if (selectedCount == 0) {
                    stringResource(
                        R.string.delete_selected
                    )
                } else {
                    stringResource(
                        R.string.selected_count,
                        selectedCount
                    )
                },
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Box(
            modifier =
                Modifier
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        GlassGreen.copy(
                            alpha =
                                if (allSelected) {
                                    0.14f
                                } else {
                                    0.065f
                                }
                        )
                    )
                    .clickable(
                        onClick = onSelectAll
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 9.dp
                    )
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector =
                        Icons.Rounded.SelectAll,
                    contentDescription = null,
                    tint = GlassGreen,
                    modifier = Modifier.size(17.dp)
                )

                Text(
                    text =
                        stringResource(
                            R.string.select_all
                        ),
                    color = GlassGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DeleteActionsRow(
    onRemoveFromHistory: () -> Unit,
    onDeleteFromDevice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        DeleteActionButton(
            modifier = Modifier.weight(1f),
            text =
                stringResource(
                    R.string.delete_from_history
                ),
            destructive = false,
            onClick = onRemoveFromHistory
        )

        DeleteActionButton(
            modifier = Modifier.weight(1f),
            text =
                stringResource(
                    R.string.delete_from_device
                ),
            destructive = true,
            onClick = onDeleteFromDevice
        )
    }
}

@Composable
private fun DeleteActionButton(
    modifier: Modifier = Modifier,
    text: String,
    destructive: Boolean,
    onClick: () -> Unit
) {
    val color =
        if (destructive) {
            GlassRed
        } else {
            Color.White
        }

    val radius = 15.dp

    Box(
        modifier =
            modifier
                .height(52.dp)
                .clip(
                    RoundedCornerShape(radius)
                )
                .background(
                    if (destructive) {
                        GlassRed.copy(alpha = 0.075f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }
                )
                .syDownGlassBorder(
                    radius = radius
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null,
                tint = color.copy(alpha = 0.90f),
                modifier = Modifier.size(17.dp)
            )

            Text(
                modifier =
                    Modifier.weight(
                        1f,
                        fill = false
                    ),
                text = text,
                color = color.copy(alpha = 0.90f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DownloadHistoryCard(
    item: DownloadHistoryItem,
    selectionMode: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onQuickDelete: () -> Unit
) {
    val radius = 22.dp

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (selectionMode) {
                        Modifier.clickable(
                            onClick = onSelect
                        )
                    } else {
                        Modifier
                    }
                )
    ) {
        SyDownGlassCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (selected) {
                            Modifier.syDownGlassBorder(
                                radius = radius,
                                selected = true
                            )
                        } else {
                            Modifier
                        }
                    ),
            radius = radius
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(13.dp)
                ) {
                    HistoryMediaPreview(
                        item = item,
                        selected = selected
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            color =
                                Color.White.copy(
                                    alpha = 0.94f
                                ),
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        if (!item.uploader.isNullOrBlank()) {
                            Text(
                                text = item.uploader,
                                color =
                                    Color.White.copy(
                                        alpha = 0.48f
                                    ),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
                            HistoryBadge(
                                text =
                                    item.qualityLabel
                                        .ifBlank {
                                            if (
                                                item.type.equals(
                                                    "AUDIO",
                                                    ignoreCase = true
                                                )
                                            ) {
                                                "Audio"
                                            } else {
                                                "Video"
                                            }
                                        }
                            )

                            val sizeText =
                                formatFileSize(
                                    item.sizeBytes
                                )

                            if (sizeText.isNotBlank()) {
                                Text(
                                    text = sizeText,
                                    color =
                                        Color.White.copy(
                                            alpha = 0.42f
                                        ),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (item.downloadedAt > 0L) {
                            Text(
                                text =
                                    formatDownloadDate(
                                        item.downloadedAt
                                    ),
                                color =
                                    Color.White.copy(
                                        alpha = 0.30f
                                    ),
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (!selectionMode) {
                        QuickDeleteButton(
                            onClick = onQuickDelete
                        )
                    }
                }

                if (!selectionMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        HistoryActionButton(
                            modifier = Modifier.weight(1f),
                            icon =
                                Icons.Rounded.FolderOpen,
                            text =
                                stringResource(
                                    R.string.open
                                ),
                            onClick = onOpen
                        )

                        HistoryActionButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Share,
                            text =
                                stringResource(
                                    R.string.share
                                ),
                            onClick = onShare
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDeleteButton(
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    GlassRed.copy(alpha = 0.07f)
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Delete,
            contentDescription =
                stringResource(R.string.delete),
            tint =
                GlassRed.copy(alpha = 0.82f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HistoryMediaPreview(
    item: DownloadHistoryItem,
    selected: Boolean
) {
    val context = LocalContext.current

    val isAudio =
        item.type.equals(
            "AUDIO",
            ignoreCase = true
        )

    var thumbnail by remember(item.id) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(
        item.id,
        item.thumbnailPath,
        item.contentUri,
        item.filePath,
        item.sizeBytes,
        isAudio
    ) {
        thumbnail = null

        if (!isAudio) {
            thumbnail =
                DownloadThumbnailLoader.load(
                    context = context,
                    item = item
                )
        }
    }

    val radius = 16.dp

    Box(
        modifier =
            Modifier
                .size(58.dp)
                .clip(
                    RoundedCornerShape(radius)
                )
                .background(
                    if (selected) {
                        GlassGreen.copy(alpha = 0.15f)
                    } else {
                        GlassGreen.copy(alpha = 0.09f)
                    }
                )
                .syDownGlassBorder(
                    radius = radius,
                    selected = selected
                ),
        contentAlignment = Alignment.Center
    ) {
        if (
            !isAudio &&
            thumbnail != null
        ) {
            Image(
                bitmap =
                    thumbnail!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha =
                                    if (selected) {
                                        0.40f
                                    } else {
                                        0.08f
                                    }
                            )
                        )
            )
        }

        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Color.Black.copy(
                                alpha = 0.64f
                            )
                        )
                        .syDownGlassBorder(
                            radius = 50.dp,
                            selected = true
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = GlassGreen,
                    modifier = Modifier.size(21.dp)
                )
            }
        } else if (
            isAudio ||
            thumbnail == null
        ) {
            Icon(
                imageVector =
                    if (isAudio) {
                        Icons.Rounded.AudioFile
                    } else {
                        Icons.Rounded.Movie
                    },
                contentDescription = null,
                tint = GlassGreen,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun HistoryActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val radius = 15.dp

    Box(
        modifier =
            modifier
                .height(44.dp)
                .clip(
                    RoundedCornerShape(radius)
                )
                .background(
                    GlassGreen.copy(alpha = 0.065f)
                )
                .syDownGlassBorder(
                    radius = radius
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlassGreen,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = text,
                color =
                    Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HistoryBadge(
    text: String
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(
                    GlassGreen.copy(alpha = 0.10f)
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 3.dp
                )
    ) {
        Text(
            text = text,
            color =
                GlassGreen.copy(alpha = 0.92f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyDownloadsCard() {
    SyDownGlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = 26.dp,
        strong = false
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 24.dp,
                        vertical = 38.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            GlassGreen.copy(alpha = 0.09f)
                        )
                        .syDownGlassBorder(
                            radius = 50.dp
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        Icons.Rounded.Download,
                    contentDescription = null,
                    tint = GlassGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text =
                    stringResource(
                        R.string.no_downloads_yet
                    ),
                color =
                    Color.White.copy(alpha = 0.92f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "SyDown",
                color =
                    Color.White.copy(alpha = 0.38f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatFileSize(
    bytes: Long
): String {
    if (bytes <= 0L) {
        return ""
    }

    val kilo = 1024.0
    val mega = kilo * 1024.0
    val giga = mega * 1024.0
    val locale = Locale.getDefault()

    return when {
        bytes >= giga ->
            String.format(
                locale,
                "%.2f GB",
                bytes / giga
            )

        bytes >= mega ->
            String.format(
                locale,
                "%.2f MB",
                bytes / mega
            )

        bytes >= kilo ->
            String.format(
                locale,
                "%.1f KB",
                bytes / kilo
            )

        else ->
            "$bytes B"
    }
}

private fun formatDownloadDate(
    timestamp: Long
): String {
    return try {
        DateFormat
            .getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
            )
            .format(Date(timestamp))
    } catch (
        _: Throwable
    ) {
        ""
    }
}