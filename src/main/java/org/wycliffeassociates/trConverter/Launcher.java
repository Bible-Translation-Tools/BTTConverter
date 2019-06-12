package org.wycliffeassociates.trConverter;

import java.util.HashMap;
import java.util.Map;

public class Launcher {
    public static void main(String[] args) {
        final Map<String, String> params = parseParams(args);

        boolean isTransformer = params.get("t") != null ? true : false;
        String rootFolder = params.get("d") != null ? params.get("d") : ".";
        String version = params.get("v") != null ? params.get("v") : "ulb";
        String langSlug = params.get("lc") != null ? params.get("lc") : "en";
        String langName = params.get("ln") != null ? params.get("ln") : "English";

        try {
            if(isTransformer) {
                IExecutor transformer = new Transformer(rootFolder,
                        langSlug,
                        langName,
                        version);
                transformer.execute();
            } else {
                IConverter converter = new Converter(rootFolder);
                converter.analyze();
                converter.getModeFromUser();
                converter.execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseParams(String[] args) {
        final Map<String, String> params = new HashMap<>();

        String param = null;
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return new HashMap<>();
                }

                param = a.substring(1);
                params.put(param, "");
            }
            else if (params.get(param) == "") {
                params.put(param, a);
            }
            else {
                System.err.println("Illegal parameter usage");
                return new HashMap<>();
            }
        }

        return params;
    }
}
