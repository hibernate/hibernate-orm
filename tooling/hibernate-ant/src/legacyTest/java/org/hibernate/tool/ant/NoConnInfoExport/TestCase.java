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

package org.hibernate.tool.ant.NoConnInfoExport;

import org.hibernate.tools.test.util.AntUtil;
import org.hibernate.tools.test.util.FileUtil;
import org.hibernate.tools.test.util.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
		destinationDir.mkdir();
		resourcesDir = new File(outputFolder, "resources");
		resourcesDir.mkdir();
	}

	// TODO HBX-3313: Verify why this does not work on Windows
	@Test
	@DisabledOnOs(OS.WINDOWS)
	public void testNoConnInfoExport() {

		String[] resources = new String[] {"build.xml", "hibernate.cfg.xml", "TopDown.hbm.xml", "hibernate.properties"};
		ResourceUtil.createResources(this, resources, resourcesDir);
		File buildFile = new File(resourcesDir, "build.xml");	
		
		AntUtil.Project project = AntUtil.createProject(buildFile);
		project.setProperty("destinationDir", destinationDir.getAbsolutePath());
		project.setProperty("resourcesDir", resourcesDir.getAbsolutePath());
		
		File noConnInfo = new File(destinationDir, "noConnInfo.sql");
		assertFalse(noConnInfo.exists());
		
		project.executeTarget("testNoConnInfoExport");
		
		assertTrue(noConnInfo.exists());
		assertTrue(FileUtil
				.findFirstString("create", noConnInfo)
				.contains("create table TopDown (id bigint not null, name varchar(255), primary key (id));"));
	}
	
}
