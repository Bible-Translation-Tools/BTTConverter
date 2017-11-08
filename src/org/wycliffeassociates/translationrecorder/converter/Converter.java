package org.wycliffeassociates.translationrecorder.converter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.wycliffeassociates.translationrecorder.wav.WavMetadata;
import org.wycliffeassociates.translationrecorder.wav.WavFile;
import wycliffeassociates.recordingapp.FilesPage.FileNameExtractor;


public class Converter {

    public static void main(String[] args) {
        String rootFolder = ".";
        String trFolder = "TranslationRecorder";
        String trArchiveFolder = "TranslationRecorderArchive";
        File tr = null;
        File tra = null;

        if(args.length > 0)
        {
            rootFolder = args[0];
        }

        tr = new File(rootFolder + File.separator + trFolder);
        tra = new File(rootFolder + File.separator + trArchiveFolder);

        // Create Archive folder if needed
        if(!tra.exists())
        {
            tra.mkdir();
        }

        File[] projects = tr.listFiles();

        // Copy project folder to Archive folder
        for(File project: projects) {
            try{
                FileUtils.copyDirectoryToDirectory(project, tra);
            } catch(Exception e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        }

        // If copy operation is successfull, delete projects from tr folder
        /*for(File project: projects) {
            try{
                FileUtils.deleteDirectory(project);
            } catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }*/

        // Iterate through projects
        File[] langs = tr.listFiles();
        for(File lang: langs) {
            File[] versions = lang.listFiles();
            for(File version: versions)
            {
                File[] books = version.listFiles();
                for(File book: books)
                {
                    File[] chapters = book.listFiles();
                    for(File chapter: chapters)
                    {
                        File[] takes = chapter.listFiles();

                        String mode = DetectMode(takes);

                        for(File take: takes)
                        {
                            if(take.isDirectory()) continue;
                            if(take.getName().equals("chapter.wav")) continue;

                            WavFile wf = new WavFile(take);
                            WavMetadata wmd = wf.getMetadata();
                            FileNameExtractor fne = new FileNameExtractor(take);

                            if(fne.matched())
                            {
                                UpdateMetadata(wmd, fne, mode);
                                wf.commit();

                                // Rename file if it was created prior to ver.8.5
                                if(fne.version84())
                                {
                                    String newName = take.getParent() + File.separator;
                                    newName += wmd.getLanguage()
                                        + "_" + wmd.getVersion()
                                        + "_b" + wmd.getBookNumber()
                                        + "_" + wmd.getSlug()
                                        + "_c" + wmd.getChapter()
                                        + "_v" + wmd.getStartVerse()
                                        + (mode.equals("chunk") ? "-" + wmd.getEndVerse() : "")
                                        + "_t" + String.format("%02d", fne.getTake())
                                        + ".wav";

                                    File newFile = new File(newName);
                                    take.renameTo(newFile);
                                }
                            }

                            System.out.println(take.getName());
                        }
                    }
                }
            }
        }
    }


    private static void UpdateMetadata(WavMetadata wmd, FileNameExtractor fne, String mode)
    {
        BookParser bp = new BookParser();

        if(wmd.getLanguage().isEmpty())
        {
            wmd.setLanguage(fne.getLang());
        }
        if(wmd.getAnthology().isEmpty())
        {
            String ant = bp.GetAnthology(fne.getBook());
            wmd.setAnthology(ant);
        }
        if(wmd.getVersion().isEmpty())
        {
            wmd.setVersion(fne.getSource());
        }
        if(wmd.getSlug().isEmpty())
        {
            wmd.setSlug(fne.getBook());
        }
        if(wmd.getBookNumber().isEmpty())
        {
            int bn = fne.getBookNumber();
            if(bn <= 0)
            {
                bn = bp.GetBookNumber(fne.getBook());
            }

            String bnStr = FileNameExtractor.chapterIntToString(fne.getBook(), bn);
            wmd.setBookNumber(bnStr);
        }
        if(wmd.getChapter().isEmpty())
        {
            int cn = fne.getChapter();
            String cnStr = FileNameExtractor.chapterIntToString(fne.getBook(), cn);
            wmd.setChapter(cnStr);
        }
        if(wmd.getModeSlug().isEmpty())
        {
            wmd.setModeSlug(mode);
        }
        if(wmd.getStartVerse().isEmpty())
        {
            int sv = fne.getStartVerse();
            String svStr = FileNameExtractor.unitIntToString(sv);
            wmd.setStartVerse(svStr);
        }
        if(wmd.getEndVerse().isEmpty())
        {
            int ev = fne.getEndVerse();
            if(ev == -1)
            {
                if(mode.equals("chunk"))
                {
                    String ant  = bp.GetAnthology(fne.getBook());
                    String path = "assets/chunks/" + ant + "/" + fne.getBook() + "/chunks.json";

                    String cnStr = FileNameExtractor.chapterIntToString(fne.getBook(), fne.getChapter());
                    String svStr = FileNameExtractor.unitIntToString(fne.getStartVerse());
                    String id = cnStr + "-" + svStr;

                    ChunksParser chp = new ChunksParser(path);
                    ev = chp.GetLastVerse(id);
                }
                else
                {
                    ev = Integer.parseInt(wmd.getStartVerse());
                }
            }

            String evStr = FileNameExtractor.unitIntToString(ev);
            wmd.setEndVerse(evStr);
        }
    }

    private static String DetectMode(File[] files)
    {
        String mode = "verse";
        ArrayList<Integer> verses = new ArrayList<Integer>();

        for(File file: files)
        {
            if(file.isDirectory()) continue;
            if(file.getName().equals("chapter.wav")) continue;

            WavFile wf = new WavFile(file);
            WavMetadata wmd = wf.getMetadata();
            FileNameExtractor fne = new FileNameExtractor(file);

            if(!wmd.getModeSlug().isEmpty())
            {
                mode = wmd.getModeSlug();
                return mode;
            }

            verses.add(fne.getStartVerse());
        }

        Collections.sort(verses);

        int lastVerse = 0;
        for(int verse: verses)
        {
            if((verse - lastVerse) > 1)
            {
                return "chunk";
            }
            lastVerse = verse;
        }

        return mode;
    }
}
