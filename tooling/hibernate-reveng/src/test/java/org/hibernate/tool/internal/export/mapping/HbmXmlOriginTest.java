package org.hibernate.tool.internal.export.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;

import org.hibernate.boot.jaxb.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HbmXmlOriginTest {
	
	@TempDir
	private Path tempDir;
	
	@Test
	public void testHbmXmlOrigin() throws Exception {
		File hbmXmlFile = new File(tempDir.toFile(), "foo.hbm.xml");
		HbmXmlOrigin hxo = new HbmXmlOrigin(hbmXmlFile);
		assertNotNull(hxo);
		assertEquals(hbmXmlFile, hxo.getHbmXmlFile());
		assertEquals(hbmXmlFile.getAbsolutePath(), hxo.getName());
		assertEquals(SourceType.FILE, hxo.getType());
	}
	
}
