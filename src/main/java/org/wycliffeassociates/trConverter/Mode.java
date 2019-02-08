package org.wycliffeassociates.trConverter;

public class Mode {
    public String mode;
    public String projectName;
    public String bookPath;

    public Mode(String mode, String projectName, String bookPath) {
        this.mode = mode;
        this.projectName = projectName;
        this.bookPath = bookPath;
    }

    @Override
    public String toString() {
        return this.projectName;
    }
}
