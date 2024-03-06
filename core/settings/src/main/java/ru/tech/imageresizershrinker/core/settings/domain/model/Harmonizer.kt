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

package ru.tech.imageresizershrinker.core.settings.domain.model

sealed class Harmonizer(
    val ordinal: Int
) {
    data class Custom(
        val color: Int
    ) : Harmonizer(color)

    data object Primary : Harmonizer(1)
    data object Secondary : Harmonizer(2)
    data object Tertiary : Harmonizer(3)

    companion object {
        val entries by lazy {
            listOf(
                Primary,
                Secondary,
                Tertiary,
                Custom(0)
            )
        }

        fun fromInt(ordinal: Int) = when (ordinal) {
            1 -> Primary
            2 -> Secondary
            3 -> Tertiary
            else -> Custom(ordinal)
        }
    }

}