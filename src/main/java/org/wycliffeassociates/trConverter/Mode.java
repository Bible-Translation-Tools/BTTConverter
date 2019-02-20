package org.wycliffeassociates.trConverter;

public class Mode {
    public String mode;
    public String projectName;

    public Mode(String mode, String projectName) {
        this.mode = mode;
        this.projectName = projectName;
    }

    @Override
    public String toString() {
        return this.projectName;
    }
}
