package ru.tech.imageresizershrinker.presentation.root.widget.color_picker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.t8rin.dynamic.theme.ColorTuple
import com.t8rin.dynamic.theme.ColorTupleItem
import com.t8rin.dynamic.theme.rememberColorScheme
import ru.tech.imageresizershrinker.R
import ru.tech.imageresizershrinker.presentation.root.theme.defaultColorTuple
import ru.tech.imageresizershrinker.presentation.root.theme.icons.CreateAlt
import ru.tech.imageresizershrinker.presentation.root.theme.icons.PaletteSwatch
import ru.tech.imageresizershrinker.presentation.root.theme.inverse
import ru.tech.imageresizershrinker.presentation.root.theme.outlineVariant
import ru.tech.imageresizershrinker.presentation.root.utils.helper.ListUtils.nearestFor
import ru.tech.imageresizershrinker.presentation.root.utils.modifier.alertDialog
import ru.tech.imageresizershrinker.presentation.root.widget.sheets.SimpleSheet
import ru.tech.imageresizershrinker.presentation.root.widget.text.AutoSizeText
import ru.tech.imageresizershrinker.presentation.root.widget.text.TitleItem
import ru.tech.imageresizershrinker.presentation.root.widget.utils.LocalSettingsState

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun AvailableColorTuplesSheet(
    visible: MutableState<Boolean>,
    colorTupleList: List<ColorTuple>,
    currentColorTuple: ColorTuple,
    openColorPicker: () -> Unit,
    borderWidth: Dp = LocalSettingsState.current.borderWidth,
    colorPicker: @Composable (onUpdateColorTuples: (List<ColorTuple>) -> Unit) -> Unit,
    onPickTheme: (ColorTuple) -> Unit,
    onUpdateColorTuples: (List<ColorTuple>) -> Unit,
) {
    val showEditColorPicker = rememberSaveable { mutableStateOf(false) }

    SimpleSheet(
        visible = visible,
        endConfirmButtonPadding = 0.dp,
        title = {
            var showConfirmDeleteDialog by remember { mutableStateOf(false) }
            val settingsState = LocalSettingsState.current

            if (showConfirmDeleteDialog) {
                AlertDialog(
                    modifier = Modifier.alertDialog(),
                    onDismissRequest = { showConfirmDeleteDialog = false },
                    confirmButton = {
                        OutlinedButton(
                            colors = ButtonDefaults.buttonColors(),
                            border = BorderStroke(
                                settingsState.borderWidth,
                                MaterialTheme.colorScheme.outlineVariant(onTopOf = MaterialTheme.colorScheme.primary)
                            ),
                            onClick = { showConfirmDeleteDialog = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            border = BorderStroke(
                                settingsState.borderWidth,
                                MaterialTheme.colorScheme.outlineVariant(onTopOf = MaterialTheme.colorScheme.secondaryContainer)
                            ),
                            onClick = {
                                showConfirmDeleteDialog = false
                                if ((colorTupleList - currentColorTuple).isEmpty()) {
                                    onPickTheme(defaultColorTuple)
                                } else {
                                    colorTupleList.nearestFor(currentColorTuple)
                                        ?.let { onPickTheme(it) }
                                }
                                onUpdateColorTuples(colorTupleList - currentColorTuple)
                            }
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    title = {
                        Text(stringResource(R.string.delete_color_scheme_title))
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ColorTupleItem(
                                colorTuple = currentColorTuple,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(64.dp)
                                    .border(
                                        borderWidth,
                                        MaterialTheme.colorScheme.outlineVariant(
                                            0.2f
                                        ),
                                        MaterialTheme.shapes.medium
                                    )
                                    .clip(MaterialTheme.shapes.medium),
                                backgroundColor = rememberColorScheme(
                                    LocalSettingsState.current.isNightMode,
                                    LocalSettingsState.current.isDynamicColors,
                                    currentColorTuple
                                ).surfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.delete_color_scheme_warn))
                        }
                    }
                )
            }
            Row {
                OutlinedButton(
                    onClick = {
                        showConfirmDeleteDialog = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    border = BorderStroke(
                        borderWidth,
                        MaterialTheme.colorScheme.outlineVariant(
                            onTopOf = MaterialTheme.colorScheme.errorContainer
                        )
                    ),
                ) {
                    Icon(Icons.Rounded.Delete, null)
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        showEditColorPicker.value = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    border = BorderStroke(
                        borderWidth,
                        MaterialTheme.colorScheme.outlineVariant(
                            onTopOf = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ),
                ) {
                    Icon(Icons.Rounded.CreateAlt, null)
                }
            }
        },
        sheetContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TitleItem(
                    text = stringResource(R.string.color_scheme),
                    icon = Icons.Rounded.PaletteSwatch
                )
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                HorizontalDivider(
                    Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(100f)
                )
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp, horizontal = 2.dp)
                ) {
                    colorTupleList.forEach { colorTuple ->
                        ColorTupleItem(
                            colorTuple = colorTuple, modifier = Modifier
                                .padding(2.dp)
                                .size(64.dp)
                                .border(
                                    borderWidth,
                                    MaterialTheme.colorScheme.outlineVariant(
                                        0.2f
                                    ),
                                    MaterialTheme.shapes.medium
                                )
                                .clip(MaterialTheme.shapes.medium)
                                .combinedClickable(
                                    onClick = {
                                        onPickTheme(colorTuple)
                                    },
                                ),
                            backgroundColor = rememberColorScheme(
                                LocalSettingsState.current.isNightMode,
                                LocalSettingsState.current.isDynamicColors,
                                colorTuple
                            ).surfaceVariant.copy(alpha = 0.8f)
                        ) {
                            AnimatedContent(
                                targetState = colorTuple == currentColorTuple
                            ) { selected ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    animateColorAsState(
                                                        colorTuple.primary.inverse(
                                                            fraction = { cond ->
                                                                if (cond) 0.8f
                                                                else 0.5f
                                                            },
                                                            darkMode = colorTuple.primary.luminance() < 0.3f
                                                        )
                                                    ).value,
                                                    CircleShape
                                                ),
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Done,
                                            contentDescription = null,
                                            tint = colorTuple.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ColorTupleItem(
                        colorTuple = ColorTuple(
                            primary = MaterialTheme.colorScheme.secondary,
                            secondary = MaterialTheme.colorScheme.secondary,
                            tertiary = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .padding(2.dp)
                            .size(64.dp)
                            .border(
                                borderWidth,
                                MaterialTheme.colorScheme.outlineVariant(
                                    0.2f
                                ),
                                MaterialTheme.shapes.medium
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { openColorPicker() },
                        backgroundColor = MaterialTheme.colorScheme
                            .surfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                HorizontalDivider(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(100f)
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = {
                    visible.value = false
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = BorderStroke(
                    borderWidth,
                    MaterialTheme.colorScheme.outlineVariant(
                        onTopOf = MaterialTheme.colorScheme.primary
                    )
                ),
            ) {
                AutoSizeText(stringResource(R.string.close))
            }
        },
    )
    ColorTuplePicker(
        visible = showEditColorPicker,
        colorTuple = currentColorTuple,
        onColorChange = {
            onUpdateColorTuples(colorTupleList + it - currentColorTuple)
            onPickTheme(it)
        }
    )
    colorPicker(onUpdateColorTuples)
}