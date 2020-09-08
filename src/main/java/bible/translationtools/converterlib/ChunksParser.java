package bible.translationtools.converterlib;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class ChunksParser {

    private JSONArray arrayOfChunks = new JSONArray();

    public ChunksParser(String file)
    {
        try {
            InputStream fis = getClass().getClassLoader().getResourceAsStream(file);
            String json = ReadInputStream(fis);
            arrayOfChunks = new JSONArray(json);
        }
        catch(Exception e)
        {
            System.out.printf(e.getMessage());
        }
    }

    private String ReadInputStream(InputStream fis)
    {
        try (Scanner scanner = new Scanner(fis)) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    public int GetLastVerse(String id)
    {
        int chkLastVs = -1;

        for (int i = 0; i < arrayOfChunks.length(); i++) {
            try {
                JSONObject jsonChunk = arrayOfChunks.getJSONObject(i);
                String chkId = jsonChunk.getString("id");

                if(id.equals(chkId))
                {
                    String chkLastVsStr = jsonChunk.getString("lastvs");
                    chkLastVs = Integer.parseInt(chkLastVsStr);
                }
            }
            catch(JSONException e)
            {
                System.out.printf(e.getMessage());
            }
        }

        return chkLastVs;
    }

    public int GetChapterLastVerse() {
        JSONObject lastChunk = arrayOfChunks.getJSONObject(arrayOfChunks.length() - 1);
        String lastVs = lastChunk.getString("lastvs");
        return Integer.parseInt(lastVs);
    }
}
