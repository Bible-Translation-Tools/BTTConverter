package bible.translationtools.converterlib;

import bible.translationtools.recorderapp.filespage.FileNameExtractor;
import bible.translationtools.recorderapp.wav.WavCue;
import bible.translationtools.recorderapp.wav.WavFile;
import bible.translationtools.recorderapp.wav.WavMetadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

public class FileConverter implements IConverter {

    private final Logger logger = Logger.getLogger(Launcher.class.getName());
    Scanner reader = new Scanner(System.in);

    File srcFile;
    Project project;

    public FileConverter(String sourcePath) {
        if (sourcePath == null) throw new IllegalArgumentException("You must specify source file");

        this.srcFile = new File(sourcePath);
        if (!this.srcFile.exists()) {
            throw new IllegalArgumentException("Source file doesn't exist.");
        }
    }

    @Override
    public Integer execute() {
        int counter = 0;

        if (project == null) throw new IllegalArgumentException("Project is not defined");

        if(project.shouldFix || project.shouldUpdate) {
            if((FilenameUtils.getExtension(this.srcFile.getName()).equals("wav") ||
                    FilenameUtils.getExtension(this.srcFile.getName()).equals("WAV"))) {

                String mode = project.mode;

                WavFile wf = new WavFile(this.srcFile);
                WavMetadata wmd = wf.getMetadata();
                FileNameExtractor fne = new FileNameExtractor(this.srcFile);

                if(fne.matched())
                {
                    this.updateMetadata(wmd, fne, mode);
                    wf.commit();

                    // Rename file if it was created prior to version.8.5
                    if(fne.version84())
                    {
                        String newName = this.srcFile.getParent() + File.separator;
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
                        this.srcFile.renameTo(newFile);
                    }

                    counter++;
                }

                System.out.println(this.srcFile.getName());
            }

            project.shouldFix = false;
            project.shouldUpdate = false;
        }

        System.out.println("Conversion complete: " + counter + " file(s) have been affected.");
        return counter;
    }

    @Override
    public void analyze() {
        if((FilenameUtils.getExtension(this.srcFile.getName()).equals("wav") ||
                FilenameUtils.getExtension(this.srcFile.getName()).equals("WAV"))) {

            String[] parts = this.srcFile.getName().split("_");
            String lang = parts.length > 0 ? parts[0] : "";
            String version = parts.length > 1 ? parts[1] : "";
            String book = parts.length > 2
                    ? (parts[2].startsWith("b") && parts.length > 3
                    ? parts[3] : parts[2]) : "";

            if (!lang.isEmpty() && !version.isEmpty() && !book.isEmpty()) {
                String mode = this.detectMode(this.srcFile);
                boolean shouldFix = this.hasBadMetadata(this.srcFile);
                this.project = new Project(
                        mode,
                        lang,
                        version,
                        book,
                        shouldFix,
                        false
                );
            }
        }
    }

    @Override
    public void setMode(Mode mode) {
        if (project == null) throw new IllegalArgumentException("Project is not defined");

        boolean modeSet = false;
        while (!modeSet) {
            String previousMode = project.mode;

            if (mode == null) {
                System.out.println("Select mode for \"" + project + "\". " +
                        (!project.mode.isEmpty() ? "Current mode: " + project.mode : ""));
                System.out.println("(1 - verse, 2 - chunk): ");

                int input = this.reader.nextInt();
                project.mode = input == 1 ? "verse" : (input == 2 ? "chunk" : "");
            } else {
                project.mode = mode.toString();
            }

            if(!project.mode.equals(previousMode)) {
                project.shouldUpdate = true;
            }

            if(!project.mode.isEmpty()) modeSet = true;
        }
    }

    @Override
    public List<Project> getProjects() {
        return null;
    }

    @Override
    public void setProjects(List<Project> projects) {
    }

    @Override
    public void setDateTimeDir() {
    }

    private String detectMode(File file)
    {
        String mode = "";

        if(!file.isDirectory()) {
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

    private boolean hasBadMetadata(File file) {
        if(!file.isDirectory()) {
            WavFile wf = new WavFile(file);
            WavMetadata wmd = wf.getMetadata();

            if(wmd.getLanguage().isEmpty()) {
                return true;
            }

            if(wmd.getAnthology().isEmpty()) {
                return true;
            }

            if(wmd.getVersion().isEmpty()) {
                return true;
            }

            if(wmd.getSlug().isEmpty()) {
                return true;
            }

            if(wmd.getBookNumber().isEmpty()) {
                return true;
            }

            if(wmd.getModeSlug().isEmpty()) {
                return true;
            }

            if(wmd.getChapter().isEmpty()) {
                return true;
            }

            if(wmd.getStartVerse().isEmpty()) {
                return true;
            }

            if(wmd.getEndVerse().isEmpty()) {
                return true;
            }

            if(wmd.getCuePoints().isEmpty()) {
                return true;
            }
        } else {
            return true;
        }

        return false;
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
            if (sv == -1) {
                sv = 1;
            }
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

                ChunksParser chp = new ChunksParser(path);

                if (fne.getStartVerse() == -1) {
                    ev = chp.GetChapterLastVerse();
                } else {
                    String cnStr = FileNameExtractor.unitIntToString(fne.getChapter());
                    String svStr = FileNameExtractor.unitIntToString(fne.getStartVerse());
                    String id = cnStr + "-" + svStr;
                    ev = chp.GetLastVerse(id);
                }
            }
            else
            {
                ev = Integer.parseInt(wmd.getStartVerse());
            }
        }

        String evStr = FileNameExtractor.unitIntToString(ev);
        wmd.setEndVerse(evStr);

        if(wmd.getCuePoints().isEmpty()) {
            int startv = Integer.parseInt(wmd.getStartVerse());
            wmd.addCue(new WavCue(String.valueOf(startv), 0));
        }
    }
}
