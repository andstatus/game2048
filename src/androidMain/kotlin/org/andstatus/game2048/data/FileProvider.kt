/*
 * Copyright (C) 2020 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andstatus.game2048.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class FileProvider : ContentProvider() {
    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context1 = context ?: return null
        val fileName = uriToCachedFilename(uri) ?: throw FileNotFoundException(uri.toString())
        val file = File(context1.cacheDir, fileName)
        if (!file.exists()) throw FileNotFoundException("Cached file: " + file.name)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun uriToCachedFilename(uri: Uri): String? = when (uri.pathSegments[0]) {
            CACHED_FILE_PATH -> uri.pathSegments[1]
            else -> throw FileNotFoundException("Unknown URI $uri")
    }

    override fun onCreate(): Boolean {
        return false
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    companion object {
        const val AUTHORITY = "org.andstatus.game2048" + ".data.FileProvider"
        const val CACHED_FILE_PATH = "cachedFile"
        val CACHED_FILE_URI = Uri.parse("content://" + AUTHORITY + "/" + CACHED_FILE_PATH)
        const val currentGameFileName = "current.game2048.json"

        fun cachedFilenameToUri(filename: String?): Uri {
            return Uri.withAppendedPath(CACHED_FILE_URI, filename)
        }
    }
}