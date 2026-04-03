/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformHbmMojoTest {

	@TempDir
	private File tempDir;

	@Test
	public void testGetHbmFilesSingleFile() throws Exception {
		File hbmFile = new File(tempDir, "Person.hbm.xml");
		assertTrue(hbmFile.createNewFile());

		List<File> result = invokeGetHbmFiles(hbmFile);
		assertEquals(1, result.size());
		assertEquals(hbmFile, result.get(0));
	}

	@Test
	public void testGetHbmFilesNonHbmIgnored() throws Exception {
		File otherFile = new File(tempDir, "something.txt");
		assertTrue(otherFile.createNewFile());

		List<File> result = invokeGetHbmFiles(otherFile);
		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetHbmFilesDirectory() throws Exception {
		File subDir = new File(tempDir, "mappings");
		assertTrue(subDir.mkdir());
		File hbm1 = new File(subDir, "Person.hbm.xml");
		File hbm2 = new File(subDir, "Address.hbm.xml");
		File other = new File(subDir, "readme.txt");
		assertTrue(hbm1.createNewFile());
		assertTrue(hbm2.createNewFile());
		assertTrue(other.createNewFile());

		List<File> result = invokeGetHbmFiles(subDir);
		assertEquals(2, result.size());
	}

	@Test
	public void testGetHbmFilesNestedDirectories() throws Exception {
		File level1 = new File(tempDir, "level1");
		File level2 = new File(level1, "level2");
		assertTrue(level2.mkdirs());
		File hbm1 = new File(level1, "Foo.hbm.xml");
		File hbm2 = new File(level2, "Bar.hbm.xml");
		assertTrue(hbm1.createNewFile());
		assertTrue(hbm2.createNewFile());

		List<File> result = invokeGetHbmFiles(level1);
		assertEquals(2, result.size());
	}

	@Test
	public void testGetHbmFilesEmptyDirectory() throws Exception {
		File emptyDir = new File(tempDir, "empty");
		assertTrue(emptyDir.mkdir());

		List<File> result = invokeGetHbmFiles(emptyDir);
		assertTrue(result.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private List<File> invokeGetHbmFiles(File f) throws Exception {
		TransformHbmMojo mojo = new TransformHbmMojo();
		Method method = TransformHbmMojo.class.getDeclaredMethod("getHbmFiles", File.class);
		method.setAccessible(true);
		return (List<File>) method.invoke(mojo, f);
	}
}
