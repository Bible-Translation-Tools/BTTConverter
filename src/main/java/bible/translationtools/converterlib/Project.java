package bible.translationtools.converterlib;

public class Project {
    public String mode;
    public String language;
    public String version;
    public String book;
    public boolean shouldFix;
    public boolean shouldUpdate;

    public Project(
            String mode,
            String language,
            String version,
            String book,
            boolean shouldFix,
            boolean shouldUpdate
    ) {
        this.mode = mode;
        this.language = language;
        this.version = version;
        this.book = book;
        this.shouldFix = shouldFix;
        this.shouldUpdate = shouldUpdate;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s",
                this.language, this.version, this.book);
    }
}
