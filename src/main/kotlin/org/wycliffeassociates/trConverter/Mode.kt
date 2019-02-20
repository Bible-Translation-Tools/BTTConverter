package org.wycliffeassociates.trConverter

data class Mode(
        var mode: String,
        var projectName: String
) {
    override fun toString(): String = projectName
}