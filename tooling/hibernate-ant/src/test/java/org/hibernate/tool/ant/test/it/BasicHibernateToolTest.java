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
package org.hibernate.tool.ant.test.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.hibernate.tool.ant.fresh.HibernateToolTask;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BasicHibernateToolTest {

	@TempDir
	Path tempDir;
	
	@Test
	public void testHibernateToolTask() throws Exception {
		String buildXmlString = 
				"<project name='HibernateToolTaskTest'>                       " +
				"  <taskdef                                                   " +
                "      name='hibernatetool'                                   " +
				"      classname='org.hibernate.tool.ant.fresh.HibernateToolTask' />" +
		        "  <target name='testHibernateToolTask'>                      " +
				"    <hibernatetool/>                                         " +
		        "  </target>                                                  " +
		        "</project>                                                   " ;
		File buildXml = new File(tempDir.toFile(), "build.xml");
		Files.write(buildXml.toPath(), buildXmlString.getBytes());
		Project project = ProjectUtil.createProject(buildXml);
		Class<?> hibernateToolTaskDefinition = project.getTaskDefinitions().get("hibernatetool");
		assertEquals(hibernateToolTaskDefinition, HibernateToolTask.class);
		Target testHibernateToolTaskTarget = project.getTargets().get("testHibernateToolTask");
		Task[] tasks = testHibernateToolTaskTarget.getTasks();
		assertTrue(tasks.length == 1);
		Task hibernateToolTask = tasks[0];
		assertEquals("hibernatetool", hibernateToolTask.getTaskName());
	}

}
