/*
 * This file is part of Breezy Weather.
 *
 * Breezy Weather is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, version 3 of the License.
 *
 * Breezy Weather is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Breezy Weather. If not, see <https://www.gnu.org/licenses/>.
 */

package org.breezyweather.domain.settings

import android.content.Context

class SourceConfigStore(
    context: Context,
    sourceId: String,
) : ConfigStore(context, "source_" + sourceId + "_preferences") {
    companion object {
        /**
         * Destinos activos por defecto para cada fuente de broadcast, mientras
         * el usuario no haya elegido nada. Gadgetbridge envía el clima a
         * CLICK TV Launcher. Si el usuario desmarca la opción, se guarda una
         * cadena vacía y el valor por defecto ya no se aplica.
         */
        fun defaultPackages(sourceId: String): String = when (sourceId) {
            "gadgetbridge" -> "com.clicktv.launcher"
            else -> ""
        }
    }
}
