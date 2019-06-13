package org.wycliffeassociates.trConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.wycliffeassociates.recordingapp.FilesPage.FileNameExtractor;
import org.wycliffeassociates.translationrecorder.wav.WavCue;
import org.wycliffeassociates.translationrecorder.wav.WavFile;
import org.wycliffeassociates.translationrecorder.wav.WavMetadata;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class is to convert takes from old version of translationRecorder
 * to new. Provide at least -t parameter with a directory
 * that contains TranslationRecorder directory as a child
 */
public class Converter implements IConverter {

    private List<Mode> modes = new ArrayList<>();

    Scanner reader = new Scanner(System.in);
    String rootPath;
    String archivePath;
    File rootDir;
    File archiveDir;
    File dateTimeDir;
    boolean backupCreated;

    public Converter(String rootPath) throws Exception {
        this.rootPath = rootPath;
        this.archivePath = rootPath + "Archive";

        this.rootDir = new File(this.rootPath);
        this.archiveDir = new File(this.archivePath);

        this.setDateTimeDir();
    }

    @Override
    public Integer execute() {
        this.createBackup();

        if(!this.backupCreated) return -1;

        int counter = 0;

        Collection<File> takes = FileUtils.listFiles(this.rootDir, null, true);
        for (File take: takes) {
            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                    !take.getName().equals("chapter.wav")) {

                String[] parts = take.getName().split("_");
                String lang = parts.length > 0 ? parts[0] : "";
                String version = parts.length > 1 ? parts[1] : "";
                String book = parts.length > 2
                        ? (parts[2].startsWith("b") && parts.length > 3
                        ? parts[3] : parts[2]) : "";

                if (!lang.isEmpty() && !version.isEmpty() && !book.isEmpty()) {
                    Mode bookMode = this.getMode(lang, version, book);
                    if(bookMode == null) continue;

                    String mode = bookMode.mode;

                    WavFile wf = new WavFile(take);
                    WavMetadata wmd = wf.getMetadata();
                    FileNameExtractor fne = new FileNameExtractor(take);

                    if(fne.matched())
                    {
                        this.updateMetadata(wmd, fne, mode);
                        wf.commit();

                        // Rename file if it was created prior to version.8.5
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

        System.out.println("Conversion complete: " + counter + " files have been affected.");
        return counter;
    }

    @Override
    public void analyze()
    {
        if(!this.rootDir.exists()) return;

        Collection<File> takes = FileUtils.listFiles(this.rootDir, null, true);
        for (File take: takes) {
            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                    !take.getName().equals("chapter.wav")) {

                String[] parts = take.getName().split("_");
                String lang = parts.length > 0 ? parts[0] : "";
                String version = parts.length > 1 ? parts[1] : "";
                String book = parts.length > 2
                        ? (parts[2].startsWith("b") && parts.length > 3
                        ? parts[3] : parts[2]) : "";

                if (!lang.isEmpty() && !version.isEmpty() && !book.isEmpty()) {
                    if (this.getMode(lang, version, book) == null) {
                        this.modes.add(new Mode(this.detectMode(take), lang, version, book));
                    }
                }
            }
        }
    }

    @Override
    public void getModeFromUser() {
        for(Mode m: this.modes) {
            Boolean modeSet = false;
            while (!modeSet) {
                System.out.println("Select mode for \"" + m + "\". " +
                        (!m.mode.isEmpty() ? "Current mode: " + m.mode : ""));
                System.out.println("(1 - verse, 2 - chunk): ");
                int input = this.reader.nextInt();
                m.mode = input == 1 ? "verse" : (input == 2 ? "chunk" : "");

                if(!m.mode.isEmpty()) modeSet = true;
            }
        }

        this.reader.close();
    }

    @Override
    public List<Mode> getModes() {
        return this.modes;
    }

    @Override
    public void setModes(List<Mode> modes) {
        this.modes = modes;
    }

    @Override
    public void setDateTimeDir() {
        String dt = this.getDateTimeStr();
        this.dateTimeDir = new File(this.archiveDir + File.separator + dt);
    }

    private void createBackup() {
        this.backupCreated = false;

        // Create Archive folder if needed
        if(!this.archiveDir.exists())
        {
            this.archiveDir.mkdir();
        }

        // Create DateTime folder
        if(!this.dateTimeDir.exists())
        {
            this.dateTimeDir.mkdir();
        }

        if(this.rootDir.exists())
        {
            File[] projects = rootDir.listFiles();

            // Copy project folder to Archive folder
            try {
                for(File project: projects) {
                    if(project.isDirectory())
                    {
                        FileUtils.copyDirectoryToDirectory(project, this.dateTimeDir);
                    }
                    else
                    {
                        FileUtils.copyFileToDirectory(project, this.dateTimeDir);
                    }
                    this.backupCreated = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateMetadata(WavMetadata wmd, FileNameExtractor fne, String mode)
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

        // Set mode every time
        wmd.setModeSlug(mode);

        if(wmd.getStartVerse().isEmpty())
        {
            int sv = fne.getStartVerse();
            String svStr = FileNameExtractor.unitIntToString(sv);
            wmd.setStartVerse(svStr);
        }

        // Set endVerse every time
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

        // Update verse markers if mode is "verse"
        if(mode == "verse" && wmd.getCuePoints().isEmpty()) {
            int startv = Integer.parseInt(wmd.getStartVerse());
            wmd.addCue(new WavCue(String.valueOf(startv), 0));
        }
    }

    private String detectMode(File file)
    {
        String mode = "";

        if(!file.isDirectory() && !file.getName().equals("chapter.wav")) {
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

    private String getDateTimeStr() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private Mode getMode(String language, String version, String book) {
        for (Mode m: this.modes) {
            if (m.language.equals(language) && m.version.equals(version) && m.book.equals(book)) {
                return m;
            }
        }

        return null;
    }
}
