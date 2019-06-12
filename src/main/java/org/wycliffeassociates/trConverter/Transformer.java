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
import java.util.Collection;

/**
 * This class is to change language and version of the takes
 * Parameters to specify:
 * -t (no value needed. It means that app will transform takes)
 * -d path (Path to a directory with takes to transform)
 * -v version (version to change to: ulb | udb | reg)
 * -lc language code (language slug: en, ru, es, etc...)
 * -ln language name (original language name: Español Latin America)
 */
public class Transformer implements IExecutor {

    String targetDirPath;
    String ver;
    String langSlug;
    String langName;
    File targetDir;

    /**
     * Constructor
     * @param dirPath path to directory
     * @param languageSlug Language code
     * @param languageName Language original name
     * @param version Project version
     * @throws Exception
     */
    public Transformer(String dirPath, String languageSlug, String languageName, String version) throws Exception {
        targetDirPath = dirPath;
        targetDir = new File(targetDirPath);
        langSlug = languageSlug != null ? languageSlug : "en";
        langName = languageName != null ? languageName : "English";
        ver = version != null ? version : "ulb";
    }

    @Override
    public Integer execute() {
        JSONObject projectManifest = open_manifest();

        if(projectManifest != null) {
            JSONObject language = projectManifest.getJSONObject("language");
            JSONObject version = projectManifest.getJSONObject("version");
            JSONArray chapters = projectManifest.getJSONArray("manifest");

            language.put("slug", langSlug);
            language.put("name", langName);
            version.put("slug", ver);
            version.put("name", getVersionName(ver));

            projectManifest.put("language", language);
            projectManifest.put("version", version);

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
                        takeNameParts[0] = langSlug;
                        takeNameParts[1] = ver;

                        takeName = strJoin(takeNameParts, "_");
                        take.put("name", takeName);

                        try {
                            String takeLocation = take.get("location").toString();
                            String[] takeLocationParts = takeLocation.split("/");
                            String takeLocationName = takeLocationParts[takeLocationParts.length-1];
                            String[] takeLocationNameParts = takeLocationName.split("_");
                            takeLocationNameParts[0] = langSlug;
                            takeLocationNameParts[1] = ver;
                            takeLocationName = strJoin(takeLocationNameParts, "_");
                            takeLocationParts[takeLocationParts.length-1] = takeLocationName;
                            takeLocation = strJoin(takeLocationParts, "/");
                            take.put("location", takeLocation);
                        } catch (JSONException e) {
                            //System.out.println("location key not found in manifest. Skipping...");
                        }
                    }
                }
            }
            writeManifest(projectManifest);
        }

        int counter = updateTakeFiles();
        System.out.println("Transformation complete: " + counter + " files have been affected.");
        return counter;
    }

    private JSONObject open_manifest() {
        try {
            File file = new File(targetDirPath + File.separator + "manifest.json");
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
        File outFile = new File(targetDirPath + File.separator + "manifest.json");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            manifest.write(bw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer updateTakeFiles() {
        int counter = 0;
        Collection<File> takes = FileUtils.listFiles(targetDir, new String[]{"wav"}, true);
        for (File takeFile: takes) {
            WavFile wf = new WavFile(takeFile);
            WavMetadata wmd = wf.getMetadata();
            String parentDir = takeFile.getParent();

            wmd.setLanguage(langSlug);
            wmd.setVersion(ver);
            wf.commit();

            String[] takeNameParts = takeFile.getName().split("_");
            takeNameParts[0] = langSlug;
            takeNameParts[1] = ver;

            String takeName = strJoin(takeNameParts, "_");
            takeFile.renameTo(new File(parentDir + File.separator + takeName));
            counter++;
        }
        return counter;
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
}
