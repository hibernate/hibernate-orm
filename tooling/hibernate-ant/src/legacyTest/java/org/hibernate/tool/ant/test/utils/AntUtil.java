/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant.test.utils;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.ProjectHelper;

import java.io.File;

public class AntUtil {

	public static class Project extends org.apache.tools.ant.Project {
		StringBuffer logBuffer;
		public void executeTarget(String targetName) {
				logBuffer = new StringBuffer();
				addBuildListener(new BuildListener(this));
				super.executeTarget(targetName);
		}
	}

	public static Project createProject(File buildFile) {
		Project project = new Project();
		project.init();
		ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
		projectHelper.parse(project, buildFile);
		return project;
	}

	public static String getLog(Project project) {
		return project.logBuffer.toString();
	}

	private static class BuildListener implements org.apache.tools.ant.BuildListener {
		private final Project project;
		public BuildListener(Project project) {
			this.project = project;
		}
		public void buildStarted(BuildEvent event) {}
		public void buildFinished(BuildEvent event) {}
		public void targetStarted(BuildEvent event) {}
		public void targetFinished(BuildEvent event) {}
		public void taskStarted(BuildEvent event) {}
		public void taskFinished(BuildEvent event) {}

		public void messageLogged(BuildEvent event) {
			if ( event.getPriority() > Project.MSG_DEBUG) {
				return;
			}
			project.logBuffer.append(event.getMessage() + "\n");
		}
	}

}
