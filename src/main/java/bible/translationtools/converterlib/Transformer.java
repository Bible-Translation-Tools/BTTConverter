package bible.translationtools.converterlib;

import bible.translationtools.recorderapp.wav.WavFile;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import bible.translationtools.recorderapp.wav.WavMetadata;

import javax.rmi.CORBA.Util;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * This class is to change language and version of the takes
 * Parameters to specify:
 * -t (no value needed. It means that app will transform takes)
 * -d path (Path to a directory with takes to transform)
 * -pl Source project language code (language code: en, ru, es, etc...)
 * -pv Source project version (version slug: ulb, udb, reg, v4)
 * -pb Source project book (book slug: gen, mrk, jas, etc...)
 * -lc language code (language slug to change to: en, ru, es, etc...)
 * -ln language name (original language name: English, Русский, Español, etc...)
 * -v version (version to change to: ulb, udb, reg, v4)
 */
public class Transformer implements ITransformer {

    String langDir;
    String versionDir;
    String bookDir;

    String langSlug;
    String langName;
    String version;

    String originalLang;
    String originalVersion;

    String rootPath;
    String archivePath;
    File rootDir;
    File archiveDir;
    File projectDir;
    File dateTimeDir;
    File projectArchiveDir;
    boolean backupCreated;

    /**
     * Constructor
     * @param rootPath root directory path
     * @param langDir language directory
     * @param versionDir version directory
     * @param bookDir book directory | if not provided, all the books will be transformed
     * @param langSlug Project language code to change to
     * @param langName Language original name
     * @param version Project version to change to
     * @throws Exception
     */
    public Transformer(String rootPath, String langDir, String versionDir, String bookDir,
                       String langSlug, String langName, String version) throws Exception {
        this.rootPath = rootPath;
        this.archivePath = this.rootPath + "Archive";

        if(langDir == null || versionDir == null) {
            throw new InvalidParameterException("Please specify the project language and version, using parameters -pl and -pv");
        }

        this.langDir = langDir;
        this.langSlug = langSlug;
        this.langName = langName;
        this.version = version;
        this.versionDir = versionDir;
        this.bookDir = bookDir;

        this.rootDir = new File(this.rootPath);
        this.archiveDir = new File(this.archivePath);
        this.projectDir = new File(Utils.strJoin(new String[] {
                this.rootDir.getAbsolutePath(),
                this.langDir,
                this.versionDir,
                (this.bookDir != null ? this.bookDir : "")
        }, File.separator));

        this.setDateTimeDir();
        this.setProjectArchiveDir();
    }

    @Override
    public Integer execute() {
        if(!this.rootDir.exists()) return -1;

        if(this.langSlug == null && this.version == null) {
            System.out.println("Nothing has been pending.");
            return 0;
        }

        this.createBackup();

        if(!this.backupCreated) return -1;

        this.updateManifest();
        int counter = this.updateTakeFiles();

        this.renameFolders();
        System.out.println("Transformation complete: " + counter + " files have been affected.");
        return counter;
    }

    @Override
    public void setDateTimeDir() {
        String dt = Utils.getDateTimeStr();
        this.dateTimeDir = new File(this.archiveDir + File.separator + dt);
    }

    private void setProjectArchiveDir() {
        this.projectArchiveDir = new File(Utils.strJoin(new String[] {
                this.dateTimeDir.getAbsolutePath(),
                this.langDir,
                this.versionDir,
                (this.bookDir != null ? this.bookDir : "")
        }, File.separator));
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

        // Create Project archive folder
        if(!this.projectArchiveDir.exists())
        {
            this.projectArchiveDir.mkdir();
        }

        File[] project = this.projectDir.listFiles();

        // Copy contents of Root folder to Archive folder
        try {
            for(File child: project) {
                if(child.isDirectory())
                {
                    FileUtils.copyDirectoryToDirectory(child, this.projectArchiveDir);
                }
                else
                {
                    FileUtils.copyFileToDirectory(child, this.projectArchiveDir);
                }
                this.backupCreated = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject open_manifest() {
        try {
            File file = new File(this.projectDir + File.separator + "manifest.json");
            String content = FileUtils.readFileToString(file, "utf-8");
            return new JSONObject(content);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private void writeManifest(JSONObject manifest) {
        File outFile = new File(this.projectDir + File.separator + "manifest.json");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            manifest.write(bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateManifest() {
        JSONObject projectManifest = this.open_manifest();

        if(projectManifest != null) {
            JSONObject language = projectManifest.getJSONObject("language");
            JSONObject version = projectManifest.getJSONObject("version");
            JSONArray chapters = projectManifest.getJSONArray("manifest");

            if(this.langSlug != null) {
                language.put("slug", this.langSlug);
                language.put("name", this.langName);
                projectManifest.put("language", language);
            }
            if(this.version != null) {
                version.put("slug", this.version);
                version.put("name", Utils.getVersionName(this.version));
                projectManifest.put("version", version);
            }

            for (int i = 0; i < chapters.length(); i++) {
                JSONObject chapter = chapters.getJSONObject(i);
                JSONArray chunks = chapter.getJSONArray("chunks");
                for (int j = 0; j < chunks.length(); j++) {
                    JSONObject chunk = chunks.getJSONObject(j);
                    JSONArray takes = chunk.getJSONArray("takes");

                    for (int k = 0; k < takes.length(); k++) {
                        JSONObject take = takes.getJSONObject(k);
                        String takeName = take.get("name").toString();

                        String[] takeNameParts = takeName.split("_");
                        takeNameParts[0] = language.getString("slug");
                        takeNameParts[1] = version.getString("slug");

                        takeName = Utils.strJoin(takeNameParts, "_");
                        take.put("name", takeName);

                        try {
                            String takeLocation = take.get("location").toString();
                            String[] takeLocationParts = takeLocation.split("/");
                            String takeLocationName = takeLocationParts[takeLocationParts.length-1];
                            String[] takeLocationNameParts = takeLocationName.split("_");
                            takeLocationNameParts[0] = language.getString("slug");
                            takeLocationNameParts[1] = version.getString("slug");
                            takeLocationName = Utils.strJoin(takeLocationNameParts, "_");
                            takeLocationParts[takeLocationParts.length-1] = takeLocationName;
                            takeLocation = Utils.strJoin(takeLocationParts, "/");
                            take.put("location", takeLocation);
                        } catch (JSONException e) {
                            //System.out.println("location key not found in manifest. Skipping...");
                        }
                    }
                }
            }
            this.writeManifest(projectManifest);
        }
    }

    private Integer updateTakeFiles() {
        int counter = 0;
        Collection<File> takes = FileUtils.listFiles(this.projectDir, new String[]{"wav"}, true);
        for (File takeFile: takes) {
            WavFile wf = new WavFile(takeFile);
            WavMetadata wmd = wf.getMetadata();
            String parentDir = takeFile.getParent();

            this.originalLang = wmd.getLanguage();
            this.originalVersion = wmd.getVersion();

            if(this.langSlug != null) {
                wmd.setLanguage(this.langSlug);
            }
            if(this.version != null) {
                wmd.setVersion(this.version);
            }
            wf.commit();

            String[] takeNameParts = takeFile.getName().split("_");
            takeNameParts[0] = wmd.getLanguage();
            takeNameParts[1] = wmd.getVersion();

            String takeName = Utils.strJoin(takeNameParts, "_");
            takeFile.renameTo(new File(parentDir + File.separator + takeName));
            counter++;
        }
        return counter;
    }

    private void renameFolders() {
        try {
            File target = new File(Utils.strJoin(new String[] {
                    this.rootDir.getAbsolutePath(),
                    (this.langSlug != null ? this.langSlug : this.originalLang),
                    this.version != null ? this.version : this.originalVersion,
                    (this.bookDir != null ? this.bookDir : "")
            }, File.separator));

            FileUtils.copyDirectory(this.projectDir, target);
            Utils.deleteDirectory(this.projectDir);
            this.projectDir = target;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
