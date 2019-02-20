package org.wycliffeassociates.trConverter

import com.matthewrussell.trwav.CuePoint
import com.matthewrussell.trwav.Metadata
import com.matthewrussell.trwav.WavFileReader
import com.matthewrussell.trwav.WavFileWriter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Converter(args: Array<String>) {
    private var modes: MutableList<Mode> = ArrayList()

    private val reader = Scanner(System.`in`)
    private var rootFolder = "."
    private val trFolder = "TranslationRecorder"
    private val trArchiveFolder = "TranslationRecorderArchive"
    private var tr: File
    private var tra: File
    private var dtDir: File
    private var backupCreated: Boolean = false
    private var isCli = true

    init {
        if (args.isNotEmpty()) {
            rootFolder = args[0]
        }

        if (args.size > 1) {
            isCli = args[1] === "c"
        }

        tr = File(rootFolder + File.separator + trFolder)
        tra = File(rootFolder + File.separator + trArchiveFolder)

        val dtStr = getDateTimeStr()
        dtDir = File(tra.toString() + File.separator + dtStr)
    }

    fun analyze() {
        if (!tr.exists()) return

        val takes = FileUtils.listFiles(tr, null, true)
        for (take in takes) {
            if (FilenameUtils.getExtension(take.name).toLowerCase() == "wav"
                    && take.name != "chapter.wav") {

                val parts = take.nameWithoutExtension.split("_")
                val lang = if (parts.isNotEmpty()) parts[0] else ""
                val version = if (parts.size > 1) parts[1] else ""
                val book = if (parts.size > 3) parts[3] else ""

                if (lang.isNotEmpty() && version.isNotEmpty() && book.isNotEmpty()) {
                    val projectName = String.format("%s | %s | %s",
                            lang, version, book)

                    if (getMode(projectName) == null) {
                        modes.add(Mode(detectMode(take), projectName))
                    }
                }
            }
        }
    }

    fun convert(): Int? {
        createBackup()

        if (!backupCreated) return -1

        var counter = 0

        val takes = FileUtils.listFiles(tr, null, true)
        for (take in takes) {
            if (FilenameUtils.getExtension(take.name).toLowerCase() == "wav"
                    && take.name != "chapter.wav") {

                val parts = take.name.split("_")
                val lang = if (parts.isNotEmpty()) parts[0] else ""
                val version = if (parts.size > 1) parts[1] else ""
                val book = if (parts.size > 3) parts[3] else ""

                if (lang.isNotEmpty() && version.isNotEmpty() && book.isNotEmpty()) {
                    val projectName = String.format("%s | %s | %s",
                            lang, version, book)

                    val bookMode = getMode(projectName) ?: continue
                    val mode = bookMode.mode

                    //val wf = WavFile(take)
                    val wf = WavFileReader().read(take)
                    val wmd = wf.metadata
                    val fne = FileNameExtractor(take)

                    if (fne.isMatched()) {
                        updateMetadata(wmd, fne, mode)
                        WavFileWriter().write(wf, take)

                        // Rename file if it was created prior to ver.8.5
                        if (fne.isVersion84()) {
                            var newName = take.parent + File.separator
                            newName += (wmd.language
                                    + "_" + wmd.version
                                    + "_b" + wmd.bookNumber
                                    + "_" + wmd.slug
                                    + "_c" + wmd.chapter
                                    + "_v" + wmd.startv
                                    + (if (mode == "chunk") "-" + wmd.endv else "")
                                    + "_t" + String.format("%02d", fne.getTake())
                                    + ".wav")

                            val newFile = File(newName)
                            take.renameTo(newFile)
                        }

                        counter++
                    }

                    println(take.name)
                }
            }
        }

        println("Conversion complete: $counter files have been affected.")
        return counter
    }

    fun getModeFromUser() {
        for (m in modes) {
            var modeSet = false
            while (!modeSet) {
                println("Select mode for \"" + m.projectName + "\". " +
                        if (m.mode.isNotEmpty()) "Current mode: " + m.mode else "")
                println("(1 - verse, 2 - chunk): ")
                val input = reader.nextInt()
                m.mode = if (input == 1) "verse" else if (input == 2) "chunk" else ""

                if (m.mode.isNotEmpty()) modeSet = true
            }
        }

        reader.close()
    }

    private fun createBackup() {
        backupCreated = false

        // Create Archive folder if needed
        if (!tra.exists()) {
            tra.mkdir()
        }

        // Create DateTime folder
        if (!dtDir.exists()) {
            dtDir.mkdir()
        }

        if (tr.exists()) {
            // Copy project folder to Archive folder
            try {
                tr.listFiles()?.let {
                    for (project in it) {
                        if (project.isDirectory) {
                            FileUtils.copyDirectoryToDirectory(project, dtDir)
                        } else {
                            FileUtils.copyFileToDirectory(project, dtDir)
                        }
                        backupCreated = true
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMetadata(wmd: Metadata, fne: FileNameExtractor, mode: String) {
        val bp = BookParser()

        if (wmd.language.isEmpty()) {
            wmd.language = fne.getLang()
        }
        if (wmd.anthology.isEmpty()) {
            val ant = bp.getAnthology(fne.getBook())
            wmd.anthology = ant
        }
        if (wmd.version.isEmpty()) {
            wmd.version = fne.getVersion()
        }
        if (wmd.slug.isEmpty()) {
            wmd.slug = fne.getBook()
        }
        if (wmd.bookNumber.isEmpty()) {
            var bn = fne.getBookNumber()
            if (bn <= 0) {
                bn = bp.getBookNumber(fne.getBook())
            }

            val bnStr = fne.unitIntToString(bn)
            wmd.bookNumber = bnStr
        }
        if (wmd.chapter.isEmpty()) {
            val cn = fne.getChapter()
            val cnStr = fne.unitIntToString(cn)
            wmd.chapter = cnStr
        }

        // Set mode every time
        wmd.mode = mode

        if (wmd.startv.isEmpty()) {
            val sv = fne.getStartVerse()
            val svStr = fne.unitIntToString(sv)
            wmd.startv = svStr
        }

        // Set endVerse every time
        var ev = fne.getEndVerse()
        if (ev == -1) {
            ev = if (mode == "chunk") {
                val ant = bp.getAnthology(fne.getBook())
                val path = "assets/chunks/" + ant + "/" + fne.getBook() + "/chunks.json"

                val cnStr = fne.unitIntToString(fne.getChapter())
                val svStr = fne.unitIntToString(fne.getStartVerse())
                val id = "$cnStr-$svStr"

                val chp = ChunksParser(path)
                chp.getLastVerse(id)
            } else {
                Integer.parseInt(wmd.startv)
            }
        }

        val evStr = fne.unitIntToString(ev)
        wmd.endv = evStr

        if (wmd.markers.isEmpty()) {
            val startv = wmd.startv.toInt()
            val endv = wmd.endv.toInt()
            val vList = mutableListOf<CuePoint>()

            for (v in startv..endv) {
                vList.add(CuePoint(0, v.toString(), v))
            }

            wmd.markers = vList
        }
    }

    private fun detectMode(file: File): String {
        var mode = ""

        if (!file.isDirectory && file.name != "chapter.wav") {
            val wf = WavFileReader().read(file)
            val wmd = wf.metadata

            if (wmd.mode.isNotEmpty()) {
                mode = wmd.mode
                return mode
            }
        }

        return mode
    }

    private fun getMode(projectName: String): Mode? {
        for (m in modes) {
            if (m.projectName == projectName) return m
        }

        return null
    }

    fun getModes(): List<Mode> {
        return this.modes
    }

    fun setModes(modes: MutableList<Mode>) {
        this.modes = modes
    }

    private fun getDateTimeStr(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val date = Date()
        val dt = dateFormat.format(date)
        return dt
    }

    fun setDateTimeDir() {
        val dtStr = getDateTimeStr()
        dtDir = File(tra.toString() + File.separator + dtStr)
    }
}