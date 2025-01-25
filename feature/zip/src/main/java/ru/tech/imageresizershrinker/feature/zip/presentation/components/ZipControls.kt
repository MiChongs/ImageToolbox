/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2025 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package ru.tech.imageresizershrinker.feature.zip.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.settings.presentation.provider.LocalSettingsState
import ru.tech.imageresizershrinker.core.ui.theme.Green
import ru.tech.imageresizershrinker.core.ui.theme.outlineVariant
import ru.tech.imageresizershrinker.core.ui.utils.content_pickers.rememberFilePicker
import ru.tech.imageresizershrinker.core.ui.utils.helper.isPortraitOrientationAsState
import ru.tech.imageresizershrinker.core.ui.utils.provider.rememberLocalEssentials
import ru.tech.imageresizershrinker.core.ui.widget.enhanced.EnhancedButton
import ru.tech.imageresizershrinker.core.ui.widget.image.UrisPreview
import ru.tech.imageresizershrinker.core.ui.widget.modifier.container
import ru.tech.imageresizershrinker.core.ui.widget.text.AutoSizeText
import ru.tech.imageresizershrinker.core.ui.widget.text.RoundedTextField
import ru.tech.imageresizershrinker.feature.zip.presentation.screenLogic.ZipComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ColumnScope.ZipControls(
    component: ZipComponent,
    lazyListState: LazyListState
) {
    val isPortrait by isPortraitOrientationAsState()
    val settingsState = LocalSettingsState.current

    val essentials = rememberLocalEssentials()
    val showConfetti: () -> Unit = essentials::showConfetti

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = {
            it?.let { uri ->
                component.saveResultTo(
                    uri = uri,
                    onResult = essentials::parseFileSaveResult
                )
            }
        }
    )

    val additionalFilePicker = rememberFilePicker(onSuccess = component::addUris)

    AnimatedVisibility(visible = component.byteArray != null) {
        LaunchedEffect(lazyListState) {
            lazyListState.animateScrollToItem(0)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .container(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme
                        .colorScheme
                        .surfaceContainerHighest,
                    resultPadding = 0.dp
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .border(
                            width = settingsState.borderWidth,
                            color = MaterialTheme.colorScheme.outlineVariant(),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    stringResource(R.string.file_proceed),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(R.string.store_file_desc),
                fontSize = 13.sp,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                lineHeight = 14.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            var name by rememberSaveable(component.byteArray, component.uris) {
                val count = component.uris.size.let {
                    if (it > 1) "($it)"
                    else ""
                }
                val timeStamp = SimpleDateFormat(
                    "yyyy-MM-dd_HH-mm-ss",
                    Locale.getDefault()
                ).format(Date())

                mutableStateOf("ZIP${count}_$timeStamp.zip")
            }
            RoundedTextField(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .container(
                        shape = MaterialTheme.shapes.large,
                        resultPadding = 8.dp
                    ),
                value = name,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = false,
                onValueChange = { name = it },
                label = {
                    Text(stringResource(R.string.filename))
                }
            )

            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
            ) {
                EnhancedButton(
                    onClick = {
                        runCatching {
                            saveLauncher.launch(name)
                        }.onFailure {
                            essentials.showActivateFilesToast()
                        }
                    },
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .fillMaxWidth(0.5f)
                        .height(50.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoSizeText(
                            text = stringResource(id = R.string.save),
                            maxLines = 1
                        )
                    }
                }
                EnhancedButton(
                    onClick = {
                        component.byteArray?.let {
                            component.shareFile(
                                it = it,
                                filename = name,
                                onComplete = showConfetti
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .fillMaxWidth()
                        .height(50.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AutoSizeText(
                            text = stringResource(id = R.string.share),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    UrisPreview(
        uris = component.uris,
        isPortrait = isPortrait,
        onRemoveUri = component::removeUri,
        onAddUris = additionalFilePicker::pickFile
    )
}