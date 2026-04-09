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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourceUtilTest {
	
	@TempDir
	public File outputFolder = new File("output");
	
	@Test
	public void testResolveResourceLocation() {
		InputStream is = ResourceUtil.resolveResourceLocation(getClass(), "/foo");
		assertNull(is);
		is = ResourceUtil.resolveResourceLocation(getClass(), "/ResourceUtilTest.resource");
		assertNotNull(is);
		is = ResourceUtil.resolveResourceLocation(getClass(), "HelloWorld.foo.bar");
		assertNull(is);
		is = ResourceUtil.resolveResourceLocation(getClass(), "HelloWorld.hbm.xml");
	}
	
	@Test
	public void testCreateResources() {
		String[] resources = new String[] { "HelloWorld.hbm.xml" };
		File helloWorldFile = new File(outputFolder, "HelloWorld.hbm.xml");
		assertFalse(helloWorldFile.exists());
		ResourceUtil.createResources(this, resources, outputFolder);
		assertTrue(helloWorldFile.exists());
		assertTrue(FileUtil
				.findFirstString("class", helloWorldFile)
				.contains("HelloWorld"));
	}

  @Test
	public void testFindResourceFile() {
    File resourceFile = ResourceUtil.resolveResourceFile(this.getClass(), "FileUtilTest.resource");
    assertTrue(resourceFile.exists());
  }
}
