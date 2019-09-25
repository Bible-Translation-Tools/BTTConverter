package bible.translationtools.converterlib;

public class Project {
    public String mode;
    public String language;
    public String version;
    public String book;
    public boolean pending;

    public Project(String mode, String language, String version, String book, boolean pending) {
        this.mode = mode;
        this.language = language;
        this.version = version;
        this.book = book;
        this.pending = pending;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s",
                this.language, this.version, this.book);
    }
}
