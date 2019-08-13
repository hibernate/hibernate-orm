package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.tools.ant.types.Environment.Variable;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExportCfgTaskTest {
	
	@TempDir 
	Path tempdir;
	
	@Test 
	public void testExportCfgTask() {
		HibernateToolTask htt = new HibernateToolTask();
		ExportCfgTask ect = new ExportCfgTask(htt);
		assertSame(htt, ect.parent);
	}
	
	@Test
	public void testExecute() throws Exception {
		ExportCfgTask ect = new ExportCfgTask(null);
		Properties properties = new Properties();
		properties.put("hibernate.dialect", "H2");
		MetadataDescriptor mdd = MetadataDescriptorFactory.createNativeDescriptor(
				null, 
				new File[] {}, 
				properties);
		ect.properties.put(ExporterConstants.METADATA_DESCRIPTOR, mdd);
		File destinationFolder = tempdir.toFile();
		ect.properties.put(ExporterConstants.DESTINATION_FOLDER, destinationFolder);
		File cfgFile = new File(destinationFolder, "hibernate.cfg.xml");
		assertFalse(cfgFile.exists());
		ect.execute();
		assertTrue(cfgFile.exists());
		String cfgXmlString = new String(Files.readAllBytes(cfgFile.toPath()));
		assertTrue(cfgXmlString.contains("hibernate.dialect"));
	}
	
	@Test
	public void testSetDestinationFolder() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertNull(ect.properties.get(ExporterConstants.DESTINATION_FOLDER));
		File file = new File("/");
		ect.setDestinationFolder(file);
		assertSame(file, ect.properties.get(ExporterConstants.DESTINATION_FOLDER));
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
	
	@Test
	public void testSetMetadataDescriptor() {
		ExportCfgTask ect = new ExportCfgTask(null);
		MetadataDescriptor mdd = MetadataDescriptorFactory.createNativeDescriptor(null, null, null);
		assertNull(ect.properties.get(ExporterConstants.METADATA_DESCRIPTOR));
		ect.setMetadataDescriptor(mdd);
		assertSame(mdd, ect.properties.get(ExporterConstants.METADATA_DESCRIPTOR));
	}
	
	@Test
	public void testGetProperties() {
		ExportCfgTask ect = new ExportCfgTask(null);
		assertSame(ect.properties, ect.getProperties());
	}

}
