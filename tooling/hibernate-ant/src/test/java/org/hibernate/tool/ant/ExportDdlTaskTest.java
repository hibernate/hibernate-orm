package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ExportDdlTaskTest {
	
	@Test
	public void testExecute() {
		ExportCfgTask ect = new ExportCfgTask();
		assertFalse(ect.executed);
		ect.execute();
		assertTrue(ect.executed);
	}

}
