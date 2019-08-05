package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.File;

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

}
