package bible.translationtools.converterlib;

import picocli.CommandLine;

class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Launcher())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
