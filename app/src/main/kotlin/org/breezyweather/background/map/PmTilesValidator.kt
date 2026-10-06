/*
 * Part of Alerta EC, based on Breezy Weather.
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.breezyweather.background.map

import java.io.File

object PmTilesValidator {

    private val magic = byteArrayOf(
        0x50, 0x4D, 0x54, 0x69, 0x6C, 0x65, 0x73,
    )

    private const val VERSION_3: Int = 3

    fun isValidVersion3(file: File): Boolean {
        if (!file.isFile || file.length() < 8L) {
            return false
        }

        return file.inputStream().buffered().use { input ->
            val header = ByteArray(8)
            if (input.read(header) != header.size) {
                return@use false
            }

            magic.indices.all { index -> header[index] == magic[index] } &&
                header[7].toInt() == VERSION_3
        }
    }
}
