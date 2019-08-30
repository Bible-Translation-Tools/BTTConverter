package bible.translationtools.converterlib;

import java.util.List;

public interface IConverter extends IExecutor {
    Integer execute();
    void analyze();
    void getModeFromUser();
    List<Mode> getModes();
    void setModes(List<Mode> modes);
    void setDateTimeDir();
}
