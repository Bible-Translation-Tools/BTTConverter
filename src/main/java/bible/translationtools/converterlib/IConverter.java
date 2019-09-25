package bible.translationtools.converterlib;

import java.util.List;

public interface IConverter extends IExecutor {
    Integer execute();
    void analyze();
    void getModeFromUser();
    List<Project> getProjects();
    void setProjects(List<Project> projects);
    void setDateTimeDir();
}
