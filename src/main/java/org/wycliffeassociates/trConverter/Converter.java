package org.wycliffeassociates.trConverter;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import org.wycliffeassociates.translationrecorder.wav.WavMetadata;
import org.wycliffeassociates.translationrecorder.wav.WavFile;
import org.wycliffeassociates.recordingapp.FilesPage.FileNameExtractor;


public class Converter {

    private List<Mode> modes = new ArrayList<>();

    private Scanner reader = new Scanner(System.in);
    private String rootFolder = ".";
    private String trFolder = "TranslationRecorder";
    private String trArchiveFolder = "TranslationRecorderArchive";
    private File tr;
    private File tra;
    private File datetime;
    boolean isCli = true;

    public Converter(String[] args) throws Exception {
        if(args.length > 0)
        {
            rootFolder = args[0];
        }

        if(args.length > 1)
        {
            isCli = args[1] == "c";
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        String dt = dateFormat.format(date);

        tr = new File(rootFolder + File.separator + trFolder);
        tra = new File(rootFolder + File.separator + trArchiveFolder);
        datetime = new File(tra + File.separator + dt);

        // Create Archive folder if needed
        if(!tra.exists())
        {
            tra.mkdir();
        }

        // Create DateTime folder
        if(!datetime.exists())
        {
            datetime.mkdir();
        }

        if(tr.exists())
        {
            File[] projects = tr.listFiles();

            // Copy project folder to Archive folder
            for(File project: projects) {
                if(project.isDirectory())
                {
                    FileUtils.copyDirectoryToDirectory(project, datetime);
                }
                else
                {
                    FileUtils.copyFileToDirectory(project, datetime);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Converter converter = new Converter(args);
            converter.analize();
            converter.getModeFromUser();
            converter.convert();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void analize()
    {
        if(!tr.exists()) return;

        // Iterate through projects
        File[] langs = tr.listFiles();
        for(File lang: langs) {
            if(!lang.isDirectory()) continue;
            File[] versions = lang.listFiles();
            for(File version: versions)
            {
                if(!version.isDirectory()) continue;
                File[] books = version.listFiles();
                for(File book: books)
                {
                    if(!book.isDirectory()) continue;
                    File[] chapters = book.listFiles();
                    for(File chapter: chapters)
                    {
                        if(!chapter.isDirectory()) continue;
                        File[] takes = chapter.listFiles();
                        List<File> filteredTakes = new ArrayList<File>();

                        for (File take: takes) {
                            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                            !take.getName().equals("chapter.wav")) {
                                filteredTakes.add(take);
                            }
                        }

                        if (filteredTakes.size() <= 0) continue;

                        if (getMode(book.getPath()) == null) {
                            String projectName = String.format("%s | %s | %s",
                                    lang.getName(), version.getName(), book.getName());
                            modes.add(new Mode(DetectMode(takes), projectName, book.getPath()));
                        }
                    }
                }
            }
        }
    }

    private void getModeFromUser() {
        for(Mode m: modes) {
            while (m.mode.isEmpty()) {
                System.out.println("Select mode for \"" + m.projectName + "\" (1 - verse, 2 - chunk): ");
                int input = reader.nextInt();
                m.mode = input == 1 ? "verse" : (input == 2 ? "chunk" : "");
            }
        }

        reader.close();
    }

    public String convert()
    {
        int counter = 0;

        // Iterate through projects
        File[] langs = tr.listFiles();
        for(File lang: langs) {
            if(!lang.isDirectory()) continue;
            File[] versions = lang.listFiles();
            for(File version: versions)
            {
                if(!version.isDirectory()) continue;
                File[] books = version.listFiles();
                for(File book: books)
                {
                    if(!book.isDirectory()) continue;

                    String mode = getMode(book.getPath()).mode;

                    File[] chapters = book.listFiles();
                    for(File chapter: chapters)
                    {
                        if(!chapter.isDirectory()) continue;
                        File[] takes = chapter.listFiles();
                        List<File> filteredTakes = new ArrayList<File>();

                        for (File take: takes) {
                            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                                    !take.getName().equals("chapter.wav")) {
                                filteredTakes.add(take);
                            }
                        }

                        if (filteredTakes.size() <= 0) continue;

                        for(File take: filteredTakes)
                        {
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

                                counter++;
                            }

                            System.out.println(take.getName());
                        }
                    }
                }
            }
        }

        System.out.println("Conversion complete: " + counter + " files have been affected.");
        return "Conversion complete: " + counter + " files have been affected.";
    }

    private void UpdateMetadata(WavMetadata wmd, FileNameExtractor fne, String mode)
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

            String bnStr = FileNameExtractor.unitIntToString(bn);
            wmd.setBookNumber(bnStr);
        }
        if(wmd.getChapter().isEmpty())
        {
            int cn = fne.getChapter();
            String cnStr = FileNameExtractor.unitIntToString(cn);
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

                    String cnStr = FileNameExtractor.unitIntToString(fne.getChapter());
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

    private String DetectMode(File[] files)
    {
        String mode = "";

        for(File file: files)
        {
            if(file.isDirectory()) continue;
            if(file.getName().equals("chapter.wav")) continue;

            WavFile wf = new WavFile(file);
            WavMetadata wmd = wf.getMetadata();

            if(!wmd.getModeSlug().isEmpty())
            {
                mode = wmd.getModeSlug();
                return mode;
            }
        }

        return mode;
    }

    private Mode getMode(String bookPath) {
        for (Mode m: modes) {
            if (m.bookPath.equals(bookPath)) return m;
        }

        return null;
    }

    public List<Mode> getModes() {
        return this.modes;
    }

    public void setModes(List<Mode> modes) {
        this.modes = modes;
    }
}
