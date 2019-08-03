package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class HibernateToolTaskTest {	
	
	@Test
	public void testCreateMetadata() {
		HibernateToolTask htt = new HibernateToolTask();
		MetadataTask mdt = htt.createMetadata();
		assertNotNull(mdt);
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
		ExportCfgTask ect = htt.createExportCfg();
		assertNotNull(ect);
	}

}
