/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultArtifactCollectorTest {

	private DefaultArtifactCollector collector;

	@BeforeEach
	public void setUp() {
		collector = new DefaultArtifactCollector();
	}

	@Test
	public void testAddFileAndGetCount() {
		File file = new File("test.java");
		collector.addFile(file, "java");
		assertEquals(1, collector.getFileCount("java"));
	}

	@Test
	public void testGetCountForUnknownType() {
		assertEquals(0, collector.getFileCount("unknown"));
	}

	@Test
	public void testGetFilesForUnknownType() {
		File[] files = collector.getFiles("unknown");
		assertEquals(0, files.length);
	}

	@Test
	public void testAddMultipleFiles() {
		collector.addFile(new File("a.java"), "java");
		collector.addFile(new File("b.java"), "java");
		collector.addFile(new File("c.xml"), "xml");
		assertEquals(2, collector.getFileCount("java"));
		assertEquals(1, collector.getFileCount("xml"));
	}

	@Test
	public void testGetFiles() {
		File f1 = new File("a.java");
		File f2 = new File("b.java");
		collector.addFile(f1, "java");
		collector.addFile(f2, "java");
		File[] files = collector.getFiles("java");
		assertEquals(2, files.length);
		assertEquals(f1, files[0]);
		assertEquals(f2, files[1]);
	}

	@Test
	public void testGetFileTypes() {
		collector.addFile(new File("a.java"), "java");
		collector.addFile(new File("b.xml"), "xml");
		collector.addFile(new File("c.cfg.xml"), "cfg.xml");
		Set<String> types = collector.getFileTypes();
		assertEquals(3, types.size());
		assertTrue(types.contains("java"));
		assertTrue(types.contains("xml"));
		assertTrue(types.contains("cfg.xml"));
	}

	@Test
	public void testGetFileTypesEmpty() {
		Set<String> types = collector.getFileTypes();
		assertTrue(types.isEmpty());
	}

	@Test
	public void testFormatFilesWithNoFiles() {
		// Should not throw when no files are registered
		collector.formatFiles();
	}

	@Test
	public void testFormatFilesWithXmlFiles(@TempDir File tempDir) throws Exception {
		File xmlFile = new File(tempDir, "test.xml");
		java.nio.file.Files.writeString(xmlFile.toPath(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><child>text</child></root>");
		collector.addFile(xmlFile, "xml");
		collector.formatFiles();
		String content = java.nio.file.Files.readString(xmlFile.toPath());
		assertTrue(content.contains("<root>"));
		assertTrue(content.contains("<child>"));
	}

	@Test
	public void testFormatFilesWithHbmXml(@TempDir File tempDir) throws Exception {
		File hbmFile = new File(tempDir, "test.hbm.xml");
		java.nio.file.Files.writeString(hbmFile.toPath(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><hibernate-mapping><class name=\"Foo\"/></hibernate-mapping>");
		collector.addFile(hbmFile, "hbm.xml");
		collector.formatFiles();
		String content = java.nio.file.Files.readString(hbmFile.toPath());
		assertTrue(content.contains("<hibernate-mapping>"));
	}

	@Test
	public void testFormatFilesWithCfgXml(@TempDir File tempDir) throws Exception {
		File cfgFile = new File(tempDir, "test.cfg.xml");
		java.nio.file.Files.writeString(cfgFile.toPath(),
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><hibernate-configuration><session-factory/></hibernate-configuration>");
		collector.addFile(cfgFile, "cfg.xml");
		collector.formatFiles();
		String content = java.nio.file.Files.readString(cfgFile.toPath());
		assertTrue(content.contains("<hibernate-configuration>"));
	}
}
