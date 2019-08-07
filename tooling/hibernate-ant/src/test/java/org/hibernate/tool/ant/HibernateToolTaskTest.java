package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class HibernateToolTaskTest {	
	
	@Test
	public void testCreateMetadata() {
		HibernateToolTask htt = new HibernateToolTask();
		assertNull(htt.metadataTask);
		MetadataTask mdt = htt.createMetadata();
		assertSame(mdt, htt.metadataTask);
	}

	@Test
	public void testCreateExportDdl() {
		HibernateToolTask htt = new HibernateToolTask();
		ExportDdlTask edt = htt.createExportDdl();
		assertNotNull(edt);
	}

	@Test
	public void testCreateExportCfg() {
		HibernateToolTask htt = new HibernateToolTask();
		assertNull(htt.exportCfgTask);
		ExportCfgTask ect = htt.createExportCfg();
		assertSame(ect, htt.exportCfgTask);
	}

}
