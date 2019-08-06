package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.tools.ant.types.FileSet;
import org.junit.jupiter.api.Test;

public class MetadataTaskTest {
	
	@Test
	public void testSetPropertyFile() {
		MetadataTask mdt = new MetadataTask();
		assertNull(mdt.propertyFile);
		File f = new File(".");
		mdt.setPropertyFile(f);
		assertSame(f, mdt.propertyFile);
	}
	
	@Test
	public void testSetConfigFile() {
		MetadataTask mdt = new MetadataTask();
		assertNull(mdt.configFile);
		File f = new File(".");
		mdt.setConfigFile(f);
		assertSame(f, mdt.configFile);
	}
	
	@Test
	public void testAddConfiguredFileSet() {
		MetadataTask mdt = new MetadataTask();
		assertTrue(mdt.fileSets.isEmpty());
		FileSet fs = new FileSet();
		mdt.addConfiguredFileSet(fs);
		assertEquals(1, mdt.fileSets.size());
		assertSame(fs, mdt.fileSets.get(0));
	}

}
