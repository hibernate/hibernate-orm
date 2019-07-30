package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ExportDdlTaskTest {
	
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

}
