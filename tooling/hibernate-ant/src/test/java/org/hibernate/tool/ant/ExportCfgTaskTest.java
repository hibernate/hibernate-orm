package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ExportCfgTaskTest {
	
	@Test void testExportCfgTask() {
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
		assertNull(ect.destinationFolder);
		File file = new File("/");
		ect.setDestinationFolder(file);
		assertSame(file, ect.destinationFolder);
	}

}
