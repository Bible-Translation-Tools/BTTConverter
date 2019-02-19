package org.wycliffeassociates.trConverter

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val converter = Converter(args)
            converter.analyze()
            converter.getModeFromUser()
            converter.convert()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}