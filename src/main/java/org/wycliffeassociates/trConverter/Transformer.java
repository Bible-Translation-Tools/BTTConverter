package org.wycliffeassociates.trConverter;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wycliffeassociates.translationrecorder.wav.WavFile;
import org.wycliffeassociates.translationrecorder.wav.WavMetadata;

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
 * -v version (version to change to: ulb | udb | reg)
 * -lc language code (language slug: en, ru, es, etc...)
 * -ln language name (original language name: Español Latin America)
 */
public class Transformer implements ITransformer {

    String projectPath;
    String version;
    String langSlug;
    String langName;

    String originalVersion;

    String rootPath;
    String archivePath;
    File rootDir;
    File archiveDir;
    File projectDir;
    File dateTimeDir;
    boolean backupCreated;

    /**
     * Constructor
     * @param rootPath root directory path
     * @param projectPath project directory
     * @param langSlug Language code
     * @param langName Language original name
     * @param version Project version
     * @throws Exception
     */
    public Transformer(String rootPath, String projectPath,
                       String langSlug, String langName, String version) throws Exception {
        this.rootPath = rootPath;
        this.archivePath = this.rootPath + "Archive";

        if(projectPath == null) {
            throw new InvalidParameterException("Please specify the project directory, using parameter -p");
        }

        this.projectPath = projectPath;
        this.langSlug = langSlug;
        this.langName = langName;
        this.version = version;

        this.rootDir = new File(this.rootPath);
        this.archiveDir = new File(this.archivePath);
        this.projectDir = new File(this.rootDir + File.separator + this.projectPath);

        this.setDateTimeDir();
    }

    @Override
    public Integer execute() {
        if(!this.rootDir.exists()) return -1;

        this.createBackup();

        if(!this.backupCreated) return -1;

        this.updateManifest();
        int counter = this.updateTakeFiles();

        this.renameParents();
        System.out.println("Transformation complete: " + counter + " files have been affected.");
        return counter;
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

        File[] projects = this.rootDir.listFiles();

        // Copy contents of Root folder to Archive folder
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
                version.put("name", this.getVersionName(this.version));
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

                        takeName = this.strJoin(takeNameParts, "_");
                        take.put("name", takeName);

                        try {
                            String takeLocation = take.get("location").toString();
                            String[] takeLocationParts = takeLocation.split("/");
                            String takeLocationName = takeLocationParts[takeLocationParts.length-1];
                            String[] takeLocationNameParts = takeLocationName.split("_");
                            takeLocationNameParts[0] = language.getString("slug");
                            takeLocationNameParts[1] = version.getString("slug");
                            takeLocationName = this.strJoin(takeLocationNameParts, "_");
                            takeLocationParts[takeLocationParts.length-1] = takeLocationName;
                            takeLocation = this.strJoin(takeLocationParts, "/");
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

            String takeName = this.strJoin(takeNameParts, "_");
            takeFile.renameTo(new File(parentDir + File.separator + takeName));
            counter++;
        }
        return counter;
    }

    private void renameParents() {
        if(this.langSlug != null) {
            try {
                File target = new File(this.rootDir + File.separator + this.langSlug);
                FileUtils.moveDirectory(this.projectDir, target);
                this.projectDir = target;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(this.version != null) {
            try {
                File source = new File(this.projectDir + File.separator + this.originalVersion);
                File target = new File(this.projectDir + File.separator + this.version);
                FileUtils.moveDirectory(source, target);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getVersionName(String version) {
        if(version.equals("ulb")) {
            return "unlocked literal bible";
        } else if(version.equals("udb")) {
            return "unlocked dynamic bible";
        } else {
            return "Regular";
        }
    }

    private static String strJoin(String[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = aArr.length; i < il; i++) {
            if (i > 0)
                sbStr.append(sSep);
            sbStr.append(aArr[i]);
        }
        return sbStr.toString();
    }

    private String getDateTimeStr() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
