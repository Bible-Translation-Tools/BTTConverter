package bible.translationtools.converterlib;

public enum Mode {
    CHUNK("chunk"),
    VERSE("verse");

    public String value;

    Mode(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }
}
