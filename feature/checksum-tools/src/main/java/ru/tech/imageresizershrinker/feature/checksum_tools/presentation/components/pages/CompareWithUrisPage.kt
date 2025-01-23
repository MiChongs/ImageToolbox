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

package ru.tech.imageresizershrinker.feature.checksum_tools.presentation.components.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.tech.imageresizershrinker.core.resources.R
import ru.tech.imageresizershrinker.core.ui.utils.content_pickers.FileType
import ru.tech.imageresizershrinker.core.ui.utils.content_pickers.rememberFilePicker
import ru.tech.imageresizershrinker.core.ui.utils.provider.rememberLocalEssentials
import ru.tech.imageresizershrinker.core.ui.widget.buttons.PagerScrollPanel
import ru.tech.imageresizershrinker.core.ui.widget.modifier.fadingEdges
import ru.tech.imageresizershrinker.core.ui.widget.modifier.negativePadding
import ru.tech.imageresizershrinker.core.ui.widget.other.InfoContainer
import ru.tech.imageresizershrinker.core.ui.widget.other.LoadingIndicator
import ru.tech.imageresizershrinker.core.ui.widget.preferences.PreferenceItemDefaults
import ru.tech.imageresizershrinker.core.ui.widget.preferences.PreferenceRow
import ru.tech.imageresizershrinker.feature.checksum_tools.presentation.components.ChecksumEnterField
import ru.tech.imageresizershrinker.feature.checksum_tools.presentation.components.ChecksumResultCard
import ru.tech.imageresizershrinker.feature.checksum_tools.presentation.components.UriWithHashItem
import ru.tech.imageresizershrinker.feature.checksum_tools.presentation.screenLogic.ChecksumToolsComponent

@Composable
internal fun ColumnScope.CompareWithUrisPage(
    component: ChecksumToolsComponent
) {
    val essentials = rememberLocalEssentials()
    val onCopyText: (String) -> Unit = essentials::copyToClipboard

    var previousFolder by rememberSaveable {
        mutableStateOf<Uri?>(null)
    }

    val isFilesLoading = component.filesLoadingProgress >= 0

    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { treeUri ->
            treeUri?.let { uri ->
                previousFolder = uri
                component.setDataForBatchComparisonFromTree(uri)
            }
        }
    )

    val pickDirectory: () -> Unit = {
        runCatching {
            openDirectoryLauncher.launch(previousFolder)
        }.onFailure {
            essentials.showActivateFilesToast()
        }
    }

    val filePicker = rememberFilePicker(
        type = FileType.Multiple,
        onSuccess = component::setDataForBatchComparison
    )

    val page = component.compareWithUrisPage

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Max)
    ) {
        PreferenceRow(
            title = stringResource(R.string.pick_files),
            onClick = filePicker::pickFile,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                bottomStart = 16.dp,
                topEnd = 4.dp,
                bottomEnd = 4.dp
            ),
            titleFontStyle = PreferenceItemDefaults.TitleFontStyleCenteredSmall,
            startIcon = Icons.Outlined.FileCopy,
            drawStartIconContainer = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(0.5f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        PreferenceRow(
            title = stringResource(R.string.pick_directory),
            onClick = pickDirectory,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = 16.dp,
                bottomEnd = 16.dp
            ),
            titleFontStyle = PreferenceItemDefaults.TitleFontStyleCenteredSmall,
            startIcon = Icons.Outlined.FolderOpen,
            drawStartIconContainer = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(0.5f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    val nestedPagerState = rememberPagerState { page.uris.size }

    AnimatedContent(
        targetState = page.uris.isNotEmpty() to isFilesLoading,
        modifier = Modifier.padding(vertical = 4.dp)
    ) { (isNotEmpty, isLoading) ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    progress = component.filesLoadingProgress
                )
            }
        } else if (isNotEmpty) {
            HorizontalPager(
                state = nestedPagerState,
                pageSpacing = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .negativePadding(horizontal = 20.dp)
                    .fadingEdges(nestedPagerState),
                contentPadding = PaddingValues(horizontal = 20.dp),
                beyondViewportPageCount = 10
            ) { nestedPage ->
                UriWithHashItem(
                    uriWithHash = page.uris[nestedPage],
                    onCopyText = onCopyText
                )
            }
        } else {
            InfoContainer(
                text = stringResource(R.string.pick_files_to_checksum),
                modifier = Modifier.padding(8.dp),
            )
        }
    }

    AnimatedVisibility(page.uris.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (page.uris.size > 1) {
                PagerScrollPanel(nestedPagerState)
            }

            ChecksumEnterField(
                value = page.targetChecksum,
                onValueChange = {
                    component.setDataForBatchComparison(
                        targetChecksum = it
                    )
                }
            )
        }
    }

    AnimatedVisibility(page.targetChecksum.isNotEmpty() && page.uris.isNotEmpty()) {
        val isCorrect =
            page.targetChecksum == page.uris[nestedPagerState.currentPage].checksum

        ChecksumResultCard(isCorrect)
    }
}