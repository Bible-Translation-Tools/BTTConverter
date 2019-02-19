package org.wycliffeassociates.trConverter

import org.json.JSONArray
import org.json.JSONException
import java.io.InputStream
import java.util.*

class BookParser {
    internal var arrayOfBooks = JSONArray()

    init {
        try {
            val fis = javaClass.classLoader.getResourceAsStream("assets/books.json")
            val json = readInputStream(fis)
            arrayOfBooks = JSONArray(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getAnthology(slug: String): String {
        var ant = ""

        try {
            for (i in 0 until arrayOfBooks.length()) {
                val jsonBook = arrayOfBooks.getJSONObject(i)
                val bookSlug = jsonBook.getString("slug")

                if (slug == bookSlug) {
                    ant = jsonBook.getString("anth")
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return ant
    }

    fun getBookNumber(slug: String): Int {
        var bn = -1

        try {
            for (i in 0 until arrayOfBooks.length()) {
                val jsonBook = arrayOfBooks.getJSONObject(i)
                val bookSlug = jsonBook.getString("slug")
                if (slug == bookSlug) {
                    bn = jsonBook.getInt("num")
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return bn
    }

    private fun readInputStream(fis: InputStream): String {
        Scanner(fis).use { scanner -> return scanner.useDelimiter("\\A").next() }
    }
}