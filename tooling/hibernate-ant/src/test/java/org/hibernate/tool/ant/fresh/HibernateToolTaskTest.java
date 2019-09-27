package org.hibernate.tool.ant.fresh;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.ant.fresh.ExportCfgTask;
import org.hibernate.tool.ant.fresh.ExportDdlTask;
import org.hibernate.tool.ant.fresh.HibernateToolTask;
import org.hibernate.tool.ant.fresh.MetadataTask;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
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
	
	Object testObject = null;
	@Test
	public void testExecute() {
		HibernateToolTask htt = new HibernateToolTask();
		final MetadataDescriptor mdd = new MetadataDescriptor() {			
			@Override
			public Properties getProperties() {
				return null;
			}		
			@Override
			public Metadata createMetadata() {
				return null;
			}
		};
		MetadataTask mdt = new MetadataTask() {
			@Override
			public MetadataDescriptor createMetadataDescriptor() {
				return mdd;
			}
		};
		htt.metadataTask = mdt;
		ExportCfgTask ect = new ExportCfgTask(htt) {
			@Override 
			public void execute() {
				testObject = getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
			}
		};
		htt.exportCfgTask = ect;
		assertNull(testObject);
		htt.execute();
		assertSame(testObject, mdd);
	}

}
