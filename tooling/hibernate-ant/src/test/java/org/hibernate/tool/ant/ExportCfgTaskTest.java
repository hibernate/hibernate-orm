package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.tool.api.export.ExporterConstants;
import org.junit.jupiter.api.Test;

public class ExportCfgTaskTest {
	
	@Test 
	public void testExportCfgTask() {
		HibernateToolTask htt = new HibernateToolTask();
		ExportCfgTask ect = new ExportCfgTask(htt);
		assertSame(htt, ect.parent);
	}
	
	@Test
	public void testExecute() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertFalse(ect.executed);
		ect.execute();
		assertTrue(ect.executed);
	}
	
	@Test
	public void testSetDestinationFolder() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get(ExporterConstants.OUTPUT_FOLDER));
		File file = new File("/");
		ect.setDestinationFolder(file);
		assertSame(file, ect.properties.get(ExporterConstants.OUTPUT_FOLDER));
	}
	
	@Test
	public void testGetDestinationFolder() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.getDestinationFolder());
		File file = new File("/");
		ect.properties.put(ExporterConstants.OUTPUT_FOLDER, file);
		assertSame(file, ect.getDestinationFolder());
	}
	
	@Test
	public void testAddConfiguredProperty() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get("foo"));
		Variable v = new Variable();
		v.setKey("foo");
		v.setValue("bar");
		ect.addConfiguredProperty(v);
		assertEquals("bar", ect.properties.get("foo"));
	}
	

}
