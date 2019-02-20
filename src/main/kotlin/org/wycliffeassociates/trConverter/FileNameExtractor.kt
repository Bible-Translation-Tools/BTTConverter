package org.wycliffeassociates.trConverter

import java.io.File
import java.util.regex.Pattern

class FileNameExtractor(file: String?) {
    private var mLang = ""
    private var mVersion = ""
    private var mBook = ""
    private var mChap: Int = 0
    private var mStartVerse: Int = 0
    private var mEndVerse: Int = 0
    private var mTake: Int = 0
    private var mBookNum: Int = 0
    private var mIsMatched = false
    private var mIsVersion84 = false

    constructor(file: File): this(file.name)

    constructor(lang: String, version: String, bookNum: String, book: String, chapter: String,
                startV: String, endV: String, take: String?) : this(null) {
        mLang = lang
        mVersion = version
        mBookNum = try {
            Integer.parseInt(bookNum)
        } catch (e: NumberFormatException) {
            -1
        }

        mBook = book

        mChap = try {
            Integer.parseInt(chapter)
        } catch (e: NumberFormatException) {
            -1
        }

        mStartVerse = try {
            Integer.parseInt(startV)
        } catch (e: NumberFormatException) {
            -1
        }

        mEndVerse = try {
            Integer.parseInt(endV)
        } catch (e: NumberFormatException) {
            -1
        }

        mTake = try {
            if (take != null) Integer.parseInt(take) else -1
        } catch (e: NumberFormatException) {
            -1
        }
    }

    init {
        file?.let {
            extractData(file)

            if (!mIsMatched) {
                mIsVersion84 = true
                extractData(file)
            }
        }
    }

    fun unitIntToString(unit: Int): String {
        return String.format("%02d", unit)
    }

    private fun extractData(file: String) {
        //includes the wav extention, could replace this with .*?
        //String FILENAME_PATTERN = "([a-zA-Z]{2,3}[-[\\d\\w]+]*)_([a-zA-Z]{3})_([1-3]*[a-zA-Z]+)_([0-9]{2})-([0-9]{2})(_([0-9]{2}))?.*";

        if (!mIsVersion84) {
            val UNDERSCORE = "_"
            val LANGUAGE = "([a-zA-Z]{2,3}[-[\\d\\w]+]*)"
            val PROJECT = "(([a-zA-Z]{3})_b([\\d]{2})_([1-3]*[a-zA-Z]+)|obs)"
            val CHAPTER = "c([\\d]{2,3})"
            val VERSE = "v([\\d]{2,3})(-([\\d]{2,3}))?"
            val TAKE = "(_t([\\d]{2}))?"
            val FILENAME_PATTERN = LANGUAGE + UNDERSCORE + PROJECT + UNDERSCORE + CHAPTER +
                    UNDERSCORE + VERSE + TAKE + ".*"
            val p = Pattern.compile(FILENAME_PATTERN)
            val m = p.matcher(file)
            val found = m.find()
            //m.group starts with the pattern, so the first group is at 1
            if (found) {
                mLang = m.group(1)
                mVersion = m.group(3)
                mBookNum = if (m.group(4) != null) Integer.parseInt(m.group(4)) else -1
                mBook = m.group(5)
                mChap = Integer.parseInt(m.group(6))
                mStartVerse = Integer.parseInt(m.group(7))
                mEndVerse = if (m.group(9) != null) Integer.parseInt(m.group(9)) else -1
                mTake = if (m.group(11) != null) Integer.parseInt(m.group(11)) else 0
                mIsMatched = true
            } else {
                mIsMatched = false
            }
        } else {
            val UNDERSCORE = "_"
            val DASH = "-"
            val LANGUAGE = "([a-zA-Z]{2,3}[-[\\d\\w]+]*)"
            val PROJECT = "(([a-zA-Z]{3})_([1-3]*[a-zA-Z]+))"
            val CHAPTER = "([\\d]{2,3})"
            val VERSE = "([\\d]{2,3})"
            val TAKE = "([\\d]{2,3})"
            val FILENAME_PATTERN = LANGUAGE + UNDERSCORE + PROJECT + UNDERSCORE + CHAPTER +
                    DASH + VERSE + UNDERSCORE + TAKE + ".*"

            val p = Pattern.compile(FILENAME_PATTERN)
            val m = p.matcher(file)
            val found = m.find()
            //m.group starts with the pattern, so the first group is at 1
            if (found) {
                mLang = m.group(1)
                mVersion = m.group(3)
                mBookNum = -1
                mBook = m.group(4)
                mChap = Integer.parseInt(m.group(5))
                mStartVerse = Integer.parseInt(m.group(6))
                mEndVerse = -1
                mTake = Integer.parseInt(m.group(7))
                mIsMatched = true
            } else {
                mIsMatched = false
            }
        }
    }

    fun getLang(): String {
        return mLang
    }

    fun getVersion(): String {
        return mVersion
    }

    fun getBook(): String {
        return mBook
    }

    fun getBookNumber(): Int {
        return mBookNum
    }

    fun getChapter(): Int {
        return mChap
    }

    fun getStartVerse(): Int {
        return mStartVerse
    }

    fun getEndVerse(): Int {
        return mEndVerse
    }

    fun getTake(): Int {
        return mTake
    }

    fun isMatched(): Boolean {
        return mIsMatched
    }

    fun isVersion84(): Boolean {
        return mIsVersion84
    }

    fun getNameWithoutTake(): String {
        val name: String
        var end = if (mEndVerse != -1 && mStartVerse != mEndVerse) String.format("-%02d", mEndVerse) else ""
        name = mLang + "_" + mVersion + "_b" + String.format("%02d", mBookNum) + "_" +
                mBook + "_c" + String.format("%02d", mChap) + "_v" + String.format("%02d", mStartVerse) + end
        return name
    }

    //Extracts the identifiable section of a filename for source audio
    fun getChapterAndVerseSection(name: String): String? {
        val CHAPTER = "c([\\d]{2,3})"
        val VERSE = "v([\\d]{2,3})(-([\\d]{2,3}))?"
        val chapterAndVerseSection = Pattern.compile("(" + CHAPTER + "_" + VERSE + ")")
        val matcher = chapterAndVerseSection.matcher(name)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}