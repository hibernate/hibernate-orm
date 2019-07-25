package org.hibernate.tool.ant.test.util;

import java.io.File;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class ProjectUtil {

	public static Project createProject(File file) {
		Project project = new Project();
		project.init();
		ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
		projectHelper.parse(project, file);
		return project;
	}

}
