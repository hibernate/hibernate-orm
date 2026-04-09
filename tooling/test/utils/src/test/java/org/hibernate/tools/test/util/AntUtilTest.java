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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Echo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AntUtilTest {
	
	private static String BUILD_XML = 
			"<?xml version='1.0' encoding='UTF-8'?> \n" + 
			"<!DOCTYPE project>                     \n" + 
			"<project name='AntUtilTest'/>          \n";
	
	@TempDir
	public File outputFolder = new File("output");
		
	@Test
	public void testCreateProject() throws Exception {
		File buildFile = new File(outputFolder, "build.xml");
		Files.copy(new ByteArrayInputStream(BUILD_XML.getBytes()), buildFile.toPath());
		AntUtil.Project project = AntUtil.createProject(buildFile);
		assertEquals("AntUtilTest", project.getName());
	}
	
	@Test
	public void testGetLog() {
		AntUtil.Project project = new AntUtil.Project();
		assertNull(project.logBuffer);
		Target target = new Target();
		target.setName("foobar");
		Echo echo = new Echo();
		echo.setProject(project);
		echo.setMessage("Executing foobar");
		target.addTask(echo);
		project.addTarget(target);
		project.executeTarget("foobar");
		assertTrue(AntUtil.getLog(project).contains("Executing foobar"));
	}

}
