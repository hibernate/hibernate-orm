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
package org.hibernate.tool.ant.AntHibernateTool;

import org.hibernate.tools.test.util.AntUtil;
import org.hibernate.tools.test.util.FileUtil;
import org.hibernate.tools.test.util.JdbcUtil;
import org.hibernate.tools.test.util.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class TestCase {
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File destinationDir = null;
	private File resourcesDir = null;
	
	@BeforeEach
	public void setUp() {
		destinationDir = new File(outputFolder, "destination");
		assertTrue(destinationDir.mkdir());
		resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testHbm2DDLLogic() throws Exception {

		String[] resources = { "Hbm2DDLLogic.xml", "TopDown.hbm.xml" };		
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "Hbm2DDLLogic.xml");		
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File topDown = new File(destinationDir, "org/hibernate/tool/hbm2x/ant/TopDown.java");
		File onlyDrop = new File(destinationDir, "onlydrop.sql");
		File onlyCreate = new File(destinationDir, "onlycreate.sql");
		File dropAndCreate = new File(destinationDir, "dropandcreate.sql");
		File update = new File(destinationDir, "update.sql");
		
		assertFalse(topDown.exists());
		assertFalse(onlyDrop.exists());	
		assertFalse(onlyCreate.exists());
		assertFalse(dropAndCreate.exists());
		assertFalse(update.exists());
		
		project.executeTarget("testHbm2DDLLogic");
		
		String log = AntUtil.getLog(project);
        assertFalse(log.contains("Exception"), log);
		
		assertTrue(topDown.exists());	
		
		assertTrue(onlyDrop.exists());		
		assertNull(FileUtil.findFirstString("create", onlyDrop));
		assertNotNull(FileUtil.findFirstString("drop", onlyDrop));	
		
		assertTrue(onlyCreate.exists());
		assertNull(FileUtil.findFirstString("drop", onlyCreate));
		assertNotNull(FileUtil.findFirstString("create", onlyCreate));
		assertNotNull(FileUtil.findFirstString("---", onlyCreate));
		
		assertTrue(dropAndCreate.exists());
		assertNotNull(FileUtil.findFirstString("drop", dropAndCreate));
		assertNotNull(FileUtil.findFirstString("create", dropAndCreate));
		assertNotNull(FileUtil.findFirstString("---", dropAndCreate));

		assertTrue(update.exists());
		assertNotNull(FileUtil.findFirstString("create", update));
		assertNotNull(FileUtil.findFirstString("---", update));

	}

	@Test
	public void testHbm2DDLUpdateExecution() {

		String[] resources = { "Hbm2DDLUpdateExecution.xml", "TopDown.hbm.xml" };
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "Hbm2DDLUpdateExecution.xml");
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File topDown = new File(destinationDir, "org/hibernate/tool/hbm2x/ant/TopDown.java");
		File onlyDrop = new File(destinationDir, "onlydrop.sql");
		File update1 = new File(destinationDir, "update1.sql");
		File update2 = new File(destinationDir, "update2.sql");
				
		assertFalse(topDown.exists());
		assertFalse(onlyDrop.exists());
		assertFalse(update1.exists());
		assertFalse(update2.exists());
		
		project.executeTarget("testantcfgUpdateExecuted");
		
		String log = AntUtil.getLog(project);
        assertFalse(log.contains("Exception"), log);
					
		assertTrue(topDown.exists());
		assertTrue(onlyDrop.exists());
		assertTrue(update1.exists());
		assertNotNull(FileUtil.findFirstString("create", update1));
		assertTrue(update2.exists());
		assertEquals(0, update2.length());

	}

	@Test
	public void testHbm2DDLExportExecution() throws Exception {

		String[] resources = { "Hbm2DDLExportExecution.xml", "TopDown.hbm.xml" };	
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "Hbm2DDLExportExecution.xml");		
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());

		File export = new File(destinationDir, "export.sql");
		File update = new File(destinationDir, "update.sql");
		File onlydrop = new File(destinationDir, "onlydrop.sql");
		
		assertFalse(export.exists());
		assertFalse(update.exists());
		assertFalse(onlydrop.exists());

		project.executeTarget("testantcfgExportExecuted");
		
		String log = AntUtil.getLog(project);
        assertFalse(log.contains("Exception"), log);
		
		assertTrue(export.exists());
		assertTrue(update.exists());
		assertNotNull(FileUtil.findFirstString("create", export));
		// if export is executed, update should be empty
		assertEquals(0, update.length());
	}

}
