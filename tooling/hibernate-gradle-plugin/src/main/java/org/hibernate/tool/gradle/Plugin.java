package org.hibernate.tool.gradle;

import org.gradle.api.Project;
import org.hibernate.tool.gradle.task.GenerateJavaTask;

public class Plugin implements org.gradle.api.Plugin<Project> {
    public void apply(Project project) {
    	project.getTasks().register("hbm2java", GenerateJavaTask.class);
    }
}
