/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tools.test.util;

import java.io.File;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.ProjectHelper;

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
        private Project project;
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
            if (event.getPriority() > Project.MSG_DEBUG) {
                return;
            }
            project.logBuffer.append(event.getMessage() + "\n");
        }
    }

}
