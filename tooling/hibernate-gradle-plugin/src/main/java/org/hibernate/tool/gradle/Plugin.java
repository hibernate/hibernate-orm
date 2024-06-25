package org.hibernate.tool.gradle;

import org.gradle.api.Project;
import org.hibernate.tool.gradle.task.GenerateJavaTask;
import org.hibernate.tool.gradle.task.RunSqlTask;

public class Plugin implements org.gradle.api.Plugin<Project> {
	
    public void apply(Project project) {
    	Extension extension =  project.getExtensions().create("hibernateTools", Extension.class, project);
    	project.getTasks().register("generateJava", GenerateJavaTask.class);
    	project.getTasks().register("runSql", RunSqlTask.class);
    	RunSqlTask runSqlTask = (RunSqlTask)project.getTasks().getByName("runSql");
    	runSqlTask.doFirst(w -> runSqlTask.initialize(extension));
    }
    
}
