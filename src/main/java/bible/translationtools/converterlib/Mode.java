package bible.translationtools.converterlib;

public class Mode {
    public String mode;
    public String language;
    public String version;
    public String book;

    public Mode(String mode, String language, String version, String book) {
        this.mode = mode;
        this.language = language;
        this.version = version;
        this.book = book;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s",
                this.language, this.version, this.book);
    }
}
