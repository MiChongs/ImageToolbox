/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
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

package ru.tech.imageresizershrinker.feature.watermarking.presentation.screenLogic

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import coil.transform.Transformation
import com.arkivanov.decompose.ComponentContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import ru.tech.imageresizershrinker.core.data.utils.toCoil
import ru.tech.imageresizershrinker.core.domain.dispatchers.DispatchersHolder
import ru.tech.imageresizershrinker.core.domain.image.ImageCompressor
import ru.tech.imageresizershrinker.core.domain.image.ImageGetter
import ru.tech.imageresizershrinker.core.domain.image.ImageScaler
import ru.tech.imageresizershrinker.core.domain.image.ShareProvider
import ru.tech.imageresizershrinker.core.domain.image.model.ImageFormat
import ru.tech.imageresizershrinker.core.domain.image.model.ImageInfo
import ru.tech.imageresizershrinker.core.domain.image.model.Quality
import ru.tech.imageresizershrinker.core.domain.image.model.ResizeType
import ru.tech.imageresizershrinker.core.domain.saving.FileController
import ru.tech.imageresizershrinker.core.domain.saving.model.ImageSaveTarget
import ru.tech.imageresizershrinker.core.domain.saving.model.SaveResult
import ru.tech.imageresizershrinker.core.domain.saving.model.onSuccess
import ru.tech.imageresizershrinker.core.domain.transformation.GenericTransformation
import ru.tech.imageresizershrinker.core.domain.utils.smartJob
import ru.tech.imageresizershrinker.core.ui.utils.BaseComponent
import ru.tech.imageresizershrinker.core.ui.utils.state.update
import ru.tech.imageresizershrinker.feature.watermarking.domain.WatermarkApplier
import ru.tech.imageresizershrinker.feature.watermarking.domain.WatermarkParams

class WatermarkingComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val initialUris: List<Uri>?,
    private val fileController: FileController,
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val shareProvider: ShareProvider<Bitmap>,
    private val imageGetter: ImageGetter<Bitmap, ExifInterface>,
    private val imageScaler: ImageScaler<Bitmap>,
    private val watermarkApplier: WatermarkApplier<Bitmap>,
    dispatchersHolder: DispatchersHolder
) : BaseComponent(dispatchersHolder, componentContext) {

    init {
        debounce {
            initialUris?.let {
                setUris(
                    uris = it,
                    onFailure = {}
                )
            }
        }
    }

    private val _internalBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val internalBitmap: Bitmap? by _internalBitmap

    private val _previewBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val previewBitmap: Bitmap? by _previewBitmap

    private val _keepExif = mutableStateOf(false)
    val keepExif by _keepExif

    private val _selectedUri = mutableStateOf(Uri.EMPTY)
    val selectedUri: Uri by _selectedUri

    private val _uris = mutableStateOf<List<Uri>>(emptyList())
    val uris by _uris

    override val _isImageLoading: MutableState<Boolean> = mutableStateOf(false)

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _watermarkParams = mutableStateOf(WatermarkParams.Default)
    val watermarkParams by _watermarkParams

    private val _imageFormat: MutableState<ImageFormat> = mutableStateOf(ImageFormat.Default)
    val imageFormat by _imageFormat

    private val _quality: MutableState<Quality> = mutableStateOf(Quality.Base())
    val quality by _quality

    private val _done: MutableState<Int> = mutableIntStateOf(0)
    val done by _done

    private val _left: MutableState<Int> = mutableIntStateOf(-1)
    val left by _left


    private fun updateBitmap(
        bitmap: Bitmap,
        onComplete: () -> Unit = {}
    ) {
        componentScope.launch {
            _isImageLoading.value = true
            _internalBitmap.value = imageScaler.scaleUntilCanShow(bitmap)
            checkBitmapAndUpdate()
            _isImageLoading.value = false
            onComplete()
        }
    }

    private fun checkBitmapAndUpdate() {
        debouncedImageCalculation {
            _previewBitmap.value = _internalBitmap.value?.let {
                getWatermarkedBitmap(it)
            }
        }
    }

    private var savingJob: Job? by smartJob {
        _isSaving.update { false }
    }

    fun saveBitmaps(
        oneTimeSaveLocationUri: String?,
        onResult: (List<SaveResult>) -> Unit
    ) {
        savingJob = componentScope.launch(defaultDispatcher) {
            _left.value = -1
            _isSaving.value = true
            val results = mutableListOf<SaveResult>()
            _done.value = 0
            _left.value = uris.size
            uris.forEach { uri ->
                getWatermarkedBitmap(
                    data = uri.toString(),
                    originalSize = true
                )?.let { localBitmap ->
                    val imageInfo = ImageInfo(
                        imageFormat = imageFormat,
                        width = localBitmap.width,
                        height = localBitmap.height,
                        quality = quality
                    )

                    results.add(
                        fileController.save(
                            saveTarget = ImageSaveTarget<ExifInterface>(
                                imageInfo = imageInfo,
                                originalUri = uri.toString(),
                                sequenceNumber = _done.value + 1,
                                data = imageCompressor.compressAndTransform(
                                    image = localBitmap,
                                    imageInfo = imageInfo
                                )
                            ),
                            keepOriginalMetadata = keepExif,
                            oneTimeSaveLocationUri = oneTimeSaveLocationUri
                        )
                    )

                } ?: results.add(
                    SaveResult.Error.Exception(Throwable())
                )

                _done.value += 1
            }
            onResult(results.onSuccess(::registerSave))
            _isSaving.value = false
        }
    }

    private suspend fun getWatermarkedBitmap(
        data: Any,
        originalSize: Boolean = false
    ): Bitmap? = withContext(defaultDispatcher) {
        imageGetter.getImage(data, originalSize)?.let { image ->
            watermarkApplier.applyWatermark(
                image = image,
                originalSize = originalSize,
                params = watermarkParams
            )
        }
    }

    fun shareBitmaps(onComplete: () -> Unit) {
        _left.value = -1
        savingJob = componentScope.launch {
            _isSaving.value = true
            _done.value = 0
            _left.value = uris.size
            shareProvider.shareImages(
                uris.map { it.toString() },
                imageLoader = { uri ->
                    getWatermarkedBitmap(
                        data = uri,
                        originalSize = true
                    )?.let {
                        it to ImageInfo(
                            width = it.width,
                            height = it.height,
                            imageFormat = imageFormat,
                            quality = quality
                        )
                    }
                },
                onProgressChange = {
                    if (it == -1) {
                        onComplete()
                        _isSaving.value = false
                        _done.value = 0
                    } else {
                        _done.value = it
                    }
                }
            )
            _isSaving.value = false
            _left.value = -1
        }
    }

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
        _left.value = -1
    }

    fun setQuality(quality: Quality) {
        _quality.update { quality }
        registerChanges()
    }

    fun setImageFormat(imageFormat: ImageFormat) {
        _imageFormat.update { imageFormat }
        registerChanges()
    }

    fun updateWatermarkParams(watermarkParams: WatermarkParams) {
        _watermarkParams.update { watermarkParams }
        registerChanges()
        checkBitmapAndUpdate()
    }

    fun updateSelectedUri(
        uri: Uri,
        onFailure: (Throwable) -> Unit = {}
    ) {
        componentScope.launch {
            _selectedUri.value = uri
            _isImageLoading.value = true
            imageGetter.getImageAsync(
                uri = uri.toString(),
                originalSize = false,
                onGetImage = { imageData ->
                    updateBitmap(imageData.image)
                    _isImageLoading.value = false
                    setImageFormat(imageData.imageInfo.imageFormat)
                },
                onFailure = {
                    _isImageLoading.value = false
                    onFailure(it)
                }
            )
        }
    }

    fun updateUrisSilently(removedUri: Uri) {
        componentScope.launch(defaultDispatcher) {
            if (selectedUri == removedUri) {
                val index = uris.indexOf(removedUri)
                if (index == 0) {
                    uris.getOrNull(1)?.let(::updateSelectedUri)
                } else {
                    uris.getOrNull(index - 1)?.let(::updateSelectedUri)
                }
            }
            _uris.update {
                it.toMutableList().apply {
                    remove(removedUri)
                }
            }
        }
    }

    fun setUris(
        uris: List<Uri>,
        onFailure: (Throwable) -> Unit = {}
    ) {
        _uris.update { uris }
        uris.firstOrNull()?.let { updateSelectedUri(it, onFailure) }
    }

    fun toggleKeepExif(value: Boolean) {
        _keepExif.update { value }
        registerChanges()
    }

    fun getWatermarkTransformation(): Transformation {
        return GenericTransformation<Bitmap>(watermarkParams) { input, size ->
            imageScaler.scaleImage(
                image = getWatermarkedBitmap(input) ?: input,
                width = size.width,
                height = size.height,
                resizeType = ResizeType.Flexible
            )
        }.toCoil()
    }

    fun cacheCurrentImage(onComplete: (Uri) -> Unit) {
        _isSaving.value = false
        savingJob?.cancel()
        savingJob = componentScope.launch {
            _isSaving.value = true
            getWatermarkedBitmap(
                data = selectedUri,
                originalSize = true
            )?.let {
                it to ImageInfo(
                    width = it.width,
                    height = it.height,
                    imageFormat = imageFormat,
                    quality = quality
                )
            }?.let { (image, imageInfo) ->
                shareProvider.cacheImage(
                    image = image,
                    imageInfo = imageInfo.copy(originalUri = selectedUri.toString())
                )?.let { uri ->
                    onComplete(uri.toUri())
                }
            }
            _isSaving.value = false
        }
    }

    fun cacheImages(
        onComplete: (List<Uri>) -> Unit
    ) {
        savingJob = componentScope.launch {
            _isSaving.value = true
            _done.value = 0
            _left.value = uris.size
            val list = mutableListOf<Uri>()
            uris.forEach { uri ->
                getWatermarkedBitmap(
                    data = uri,
                    originalSize = true
                )?.let {
                    it to ImageInfo(
                        width = it.width,
                        height = it.height,
                        imageFormat = imageFormat,
                        quality = quality
                    )
                }?.let { (image, imageInfo) ->
                    shareProvider.cacheImage(
                        image = image,
                        imageInfo = imageInfo.copy(originalUri = uri.toString())
                    )?.let { uri ->
                        list.add(uri.toUri())
                    }
                }
                _done.value += 1
            }
            onComplete(list)
            _isSaving.value = false
        }
    }

    fun selectLeftUri() {
        uris
            .indexOf(selectedUri)
            .takeIf { it >= 0 }
            ?.let {
                uris.getOrNull(it - 1)
            }
            ?.let(::updateSelectedUri)
    }

    fun selectRightUri() {
        uris
            .indexOf(selectedUri)
            .takeIf { it >= 0 }
            ?.let {
                uris.getOrNull(it + 1)
            }
            ?.let(::updateSelectedUri)
    }

    fun getFormatForFilenameSelection(): ImageFormat? =
        if (uris.size == 1) imageFormat
        else null


    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            initialUris: List<Uri>?,
        ): WatermarkingComponent
    }
}