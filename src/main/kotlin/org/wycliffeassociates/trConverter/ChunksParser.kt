package org.wycliffeassociates.trConverter

import org.json.JSONArray
import org.json.JSONException
import java.io.InputStream
import java.util.*

class ChunksParser(file: String) {
    private var arrayOfChunks = JSONArray()

    init {
        try {
            val fis = javaClass.classLoader.getResourceAsStream(file)
            val json = readInputStream(fis)
            arrayOfChunks = JSONArray(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLastVerse(id: String): Int {
        var chkLastVs = -1

        for (i in 0 until arrayOfChunks.length()) {
            try {
                val jsonChunk = arrayOfChunks.getJSONObject(i)
                val chkId = jsonChunk.getString("id")

                if (id == chkId) {
                    val chkLastVsStr = jsonChunk.getString("lastvs")
                    chkLastVs = Integer.parseInt(chkLastVsStr)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }

        return chkLastVs
    }

    private fun readInputStream(fis: InputStream): String {
        Scanner(fis).use { scanner -> return scanner.useDelimiter("\\A").next() }
    }
}