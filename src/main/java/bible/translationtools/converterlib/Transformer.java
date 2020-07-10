package bible.translationtools.converterlib;

import bible.translationtools.recorderapp.wav.WavFile;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import bible.translationtools.recorderapp.wav.WavMetadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;

/**
 * This class is to change language and version of the takes
 * Parameters to specify:
 * -t (no value needed. It means that app will transform takes)
 * -d path (Path to a directory with takes to transform)
 * -pl Source language (language slug: en, ru, es, etc...)
 * -pv Source version (version slug: ulb, udb, reg, v4)
 * -pb Source book (book slug: gen, mrk, jas, etc...)
 * -lc target language (language slug: en, ru, es, etc...)
 * -ln target language name (language name: English, Русский, Español, etc...)
 * -v target version (version slug: ulb, udb, reg, v4)
 */
public class Transformer implements ITransformer {

    String sourceLanguage;
    String sourceVersion;
    String sourceBook;

    String targetLanguage;
    String targetLanguageName;
    String targetVersion;

    String originalLanguage;
    String originalVersion;

    File rootDir;
    File archiveDir;
    File projectDir;
    File dateTimeDir;
    File projectArchiveDir;
    boolean backupCreated;

    /**
     * Constructor
     * @param rootPath root directory path
     * @param sourceLanguage Source language slug
     * @param sourceVersion Source version slug
     * @param sourceBook Source book slug | if null, all the books will be transformed
     * @param targetLanguage Target language slug
     * @param targetLanguageName Target language name | set it null if not used for BTT Exchanger
     * @param targetVersion Target version slug
     * @throws Exception
     */
    public Transformer(String rootPath, String sourceLanguage, String sourceVersion, String sourceBook,
                       String targetLanguage, String targetLanguageName, String targetVersion) throws Exception {
        rootPath = rootPath.replaceFirst("/$", ""); // remove trailing slash if exists

        if(sourceLanguage == null || sourceVersion == null) {
            throw new InvalidParameterException(
                    "Please specify the project language and version, using parameters -pl and -pv"
            );
        }

        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.targetLanguageName = targetLanguageName;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.sourceBook = sourceBook;

        this.rootDir = new File(rootPath);
        this.archiveDir = new File(rootPath + "Archive");
        this.projectDir = new File(Utils.strJoin(new String[] {
                this.rootDir.getAbsolutePath(),
                this.sourceLanguage,
                this.sourceVersion,
                (this.sourceBook != null ? this.sourceBook : "")
        }, File.separator));

        this.setDateTimeDir();
        this.setProjectArchiveDir();
    }

    @Override
    public Integer execute() {
        if(!this.rootDir.exists()) return -1;

        if(this.targetLanguage == null && this.targetVersion == null) {
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
                this.sourceLanguage,
                this.sourceVersion,
                (this.sourceBook != null ? this.sourceBook : "")
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

            if(this.targetLanguage != null) {
                language.put("slug", this.targetLanguage);
                language.put("name", this.targetLanguageName);
                projectManifest.put("language", language);
            }
            if(this.targetVersion != null) {
                version.put("slug", this.targetVersion);
                version.put("name", Utils.getVersionName(this.targetVersion));
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

            this.originalLanguage = wmd.getLanguage();
            this.originalVersion = wmd.getVersion();

            if(this.targetLanguage != null) {
                wmd.setLanguage(this.targetLanguage);
            }
            if(this.targetVersion != null) {
                wmd.setVersion(this.targetVersion);
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
                    (this.targetLanguage != null ? this.targetLanguage : this.originalLanguage),
                    this.targetVersion != null ? this.targetVersion : this.originalVersion,
                    (this.sourceBook != null ? this.sourceBook : "")
            }, File.separator));

            FileUtils.copyDirectory(this.projectDir, target);
            Utils.deleteDirectory(this.projectDir);
            this.projectDir = target;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
