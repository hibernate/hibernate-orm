package org.hibernate.tool.gradle;

import org.gradle.api.Project;

public class Extension {
	
	public String sqlToRun = "";
	public String hibernateProperties = "hibernate.properties";
	public String outputFolder = "generated-sources";
	
	public Extension(Project project) {}
	
}
