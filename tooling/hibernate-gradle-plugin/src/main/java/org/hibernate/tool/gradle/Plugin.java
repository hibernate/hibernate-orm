package org.hibernate.tool.gradle;

import java.util.Map;

import org.gradle.api.Project;
import org.hibernate.tool.gradle.task.AbstractTask;
import org.hibernate.tool.gradle.task.GenerateCfgTask;
import org.hibernate.tool.gradle.task.GenerateHbmTask;
import org.hibernate.tool.gradle.task.GenerateJavaTask;
import org.hibernate.tool.gradle.task.RunSqlTask;

public class Plugin implements org.gradle.api.Plugin<Project> {
	
	private static Map<String, Class<?>> PLUGIN_TASK_MAP = Map.of(
			"runSql", RunSqlTask.class,
			"generateJava", GenerateJavaTask.class,
			"generateCfg", GenerateCfgTask.class,
			"generateHbm", GenerateHbmTask.class
		);
	
    @SuppressWarnings("unchecked")
	public void apply(Project project) {
    	Extension extension =  project.getExtensions().create("hibernateTools", Extension.class, project);
    	for (String key : PLUGIN_TASK_MAP.keySet()) {
    		Class<?> taskClass = PLUGIN_TASK_MAP.get(key);
    		project.getTasks().register(key, (Class<AbstractTask>)taskClass);
    		AbstractTask task = (AbstractTask)project.getTasks().getByName(key);
    		task.doFirst(w -> task.initialize(extension));
    	}
    }
    
}
