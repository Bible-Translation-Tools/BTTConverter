package bible.translationtools.converterlib;

import picocli.CommandLine;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Launcher implements Runnable {
    private final Logger logger = Logger.getLogger(Launcher.class.getName());

    @CommandLine.Option(names = "-t", description = "If you want to change the language of the project files and/or resource type")
    private boolean isTransformer;

    @CommandLine.Option(names = "-d", description = "A destination directory with audio projects")
    private String destination;

    @CommandLine.Option(names = "-pl", description = "Source project language directory name")
    private String projectLang;

    @CommandLine.Option(names = "-pv", description = "Source project version directory name")
    private String projectVersion;

    @CommandLine.Option(names = "-pb", description = "Source project book directory name")
    private String projectBook;

    @CommandLine.Option(names = "-v", description = "Resource type (version) code to change to")
    private String version;

    @CommandLine.Option(names = "-lc", description = "Language code to change to")
    private String languageCode;

    @CommandLine.Option(names = "-ln", description = "Original language name. Optional")
    private String languageName;

    @CommandLine.Option(names = "-m", description = "Mode (verse or chunk). Optional")
    private Mode mode;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help")
    private boolean helpRequested = false;

    private void execute() {
        try {
            if (destination == null) throw new IllegalArgumentException("You must specify destination directory");

            if(isTransformer) {
                IExecutor transformer = new Transformer(destination,
                        projectLang,
                        projectVersion,
                        projectBook,
                        languageCode,
                        languageName,
                        version);
                transformer.execute();
            } else {
                IConverter converter = new Converter(destination);
                converter.analyze();
                converter.setMode(mode);
                converter.execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    @Override
    public void run() {
        execute();
    }
}
