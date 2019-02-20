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


public class Converter {

    private List<Mode> modes = new ArrayList<>();

    private Scanner reader = new Scanner(System.in);
    private String rootFolder = ".";
    private String trFolder = "TranslationRecorder";
    private String trArchiveFolder = "TranslationRecorderArchive";
    private File trDir;
    private File traDir;
    private File dateTimeDir;
    private boolean backupCreated;
    private boolean isCli = true;

    public Converter(String[] args) throws Exception {
        if(args.length > 0)
        {
            rootFolder = args[0];
        }

        if(args.length > 1)
        {
            isCli = args[1] == "c";
        }

        trDir = new File(rootFolder + File.separator + trFolder);
        traDir = new File(rootFolder + File.separator + trArchiveFolder);

        setDateTimeDir();
    }

    public static void main(String[] args) {
        try {
            Converter converter = new Converter(args);
            converter.analyze();
            converter.getModeFromUser();
            converter.convert();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyze()
    {
        if(!trDir.exists()) return;

        Collection<File> takes = FileUtils.listFiles(trDir, null, true);
        for (File take: takes) {
            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                    !take.getName().equals("chapter.wav")) {

                String[] parts = take.getName().split("_");
                String lang = parts.length > 0 ? parts[0] : "";
                String version = parts.length > 1 ? parts[1] : "";
                String book = parts.length > 3 ? parts[3] : "";

                if (!lang.isEmpty() && !version.isEmpty() && !book.isEmpty()) {
                    String projectName = String.format("%s | %s | %s",
                            lang, version, book);

                    if (getMode(projectName) == null) {
                        modes.add(new Mode(detectMode(take), projectName));
                    }
                }
            }
        }
    }

    public Integer convert() {
        createBackup();

        if(!backupCreated) return -1;

        int counter = 0;

        Collection<File> takes = FileUtils.listFiles(trDir, null, true);
        for (File take: takes) {
            if((FilenameUtils.getExtension(take.getName()).equals("wav") ||
                    FilenameUtils.getExtension(take.getName()).equals("WAV")) &&
                    !take.getName().equals("chapter.wav")) {

                String[] parts = take.getName().split("_");
                String lang = parts.length > 0 ? parts[0] : "";
                String version = parts.length > 1 ? parts[1] : "";
                String book = parts.length > 3 ? parts[3] : "";

                if (!lang.isEmpty() && !version.isEmpty() && !book.isEmpty()) {
                    String projectName = String.format("%s | %s | %s",
                            lang, version, book);

                    Mode bookMode = getMode(projectName);
                    if(bookMode == null) continue;

                    String mode = bookMode.mode;

                    WavFile wf = new WavFile(take);
                    WavMetadata wmd = wf.getMetadata();
                    FileNameExtractor fne = new FileNameExtractor(take);

                    if(fne.matched())
                    {
                        updateMetadata(wmd, fne, mode);
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

        System.out.println("Conversion complete: " + counter + " files have been affected.");
        return counter;
    }

    private void getModeFromUser() {
        for(Mode m: modes) {
            Boolean modeSet = false;
            while (!modeSet) {
                System.out.println("Select mode for \"" + m.projectName + "\". " +
                        (!m.mode.isEmpty() ? "Current mode: " + m.mode : ""));
                System.out.println("(1 - verse, 2 - chunk): ");
                int input = reader.nextInt();
                m.mode = input == 1 ? "verse" : (input == 2 ? "chunk" : "");

                if(!m.mode.isEmpty()) modeSet = true;
            }
        }

        reader.close();
    }

    private void createBackup() {
        backupCreated = false;

        // Create Archive folder if needed
        if(!traDir.exists())
        {
            traDir.mkdir();
        }

        // Create DateTime folder
        if(!dateTimeDir.exists())
        {
            dateTimeDir.mkdir();
        }

        if(trDir.exists())
        {
            File[] projects = trDir.listFiles();

            // Copy project folder to Archive folder
            try {
                for(File project: projects) {
                    if(project.isDirectory())
                    {
                        FileUtils.copyDirectoryToDirectory(project, dateTimeDir);
                    }
                    else
                    {
                        FileUtils.copyFileToDirectory(project, dateTimeDir);
                    }
                    backupCreated = true;
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
            wmd.addCue(new WavCue(wmd.getStartVerse(), 0));
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

    private Mode getMode(String projectName) {
        for (Mode m: modes) {
            if (m.projectName.equals(projectName)) return m;
        }

        return null;
    }

    public List<Mode> getModes() {
        return this.modes;
    }

    public void setModes(List<Mode> modes) {
        this.modes = modes;
    }

    public void setDateTimeDir() {
        String dt = getDateTimeStr();
        dateTimeDir = new File(traDir + File.separator + dt);
    }


}
