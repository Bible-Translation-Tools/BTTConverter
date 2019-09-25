package bible.translationtools.converterlib;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public Utils() {}

    public static String strJoin(String[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = aArr.length; i < il; i++) {
            if (i > 0)
                sbStr.append(sSep);
            sbStr.append(aArr[i]);
        }
        return sbStr.toString();
    }

    public static void deleteDirectory(File path)
    {
        if (path == null)
            return;
        if (path.exists())
        {
            for(File f : path.listFiles())
            {
                if(f.isDirectory())
                {
                    deleteDirectory(f);
                    f.delete();
                }
                else
                {
                    f.delete();
                }
            }
            path.delete();
        }
    }

    public static String getDateTimeStr() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getVersionName(String version) {
        if(version.equals("ulb")) {
            return "unlocked literal bible";
        } else if(version.equals("udb")) {
            return "unlocked dynamic bible";
        } else {
            return "Regular";
        }
    }
}
