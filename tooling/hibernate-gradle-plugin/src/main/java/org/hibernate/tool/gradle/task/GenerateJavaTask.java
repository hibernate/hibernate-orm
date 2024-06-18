package org.hibernate.tool.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class GenerateJavaTask extends DefaultTask {
	
	@TaskAction
	public void generate() {
		getLogger().lifecycle("Starting Task 'GenerateJavaTask'");
		System.out.println("I am now generating Java files");
		getLogger().lifecycle("Ending Task 'GenerateJavaTask");
	}

}
