/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.hbm2x.Hbm2CfgTest;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.api.export.*;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.test.utils.ConnectionProvider;
import org.hibernate.tool.test.utils.FileUtil;
import org.hibernate.tool.test.utils.HibernateUtil;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	public static class FakeTransactionManagerLookup implements TransactionCoordinatorBuilder {
		@Override
		public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
			return null;
		}
		@Override
		public boolean isJta() {
			return false;
		}
		@Override
		public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
			return PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT;
		}		
	}
	
	private static final String[] HBM_XML_FILES = new String[] {
			"HelloWorld.hbm.xml"
	};
	
	@TempDir
	public File outputFolder = new File("output");
	
	private File srcDir = null;

    private Exporter cfgexporter;

	@BeforeEach
	public void setUp() throws Exception {
		srcDir = new File(outputFolder, "src");
		assertTrue(srcDir.mkdir());
        File resourcesDir = new File(outputFolder, "resources");
		assertTrue(resourcesDir.mkdir());
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, resourcesDir);
		cfgexporter = ExporterFactory.createExporter(ExporterType.CFG);
		cfgexporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		cfgexporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
		cfgexporter.start();
	}
	
	@Test
	public void testMagicPropertyHandling() {
	   Exporter exporter = ExporterFactory.createExporter(ExporterType.CFG);
	   Properties properties = exporter.getProperties();
	   properties.put( "hibernate.basic", "aValue" );
	   properties.put(AvailableSettings.SESSION_FACTORY_NAME, "shouldNotShowUp");
	   properties.put(AvailableSettings.HBM2DDL_AUTO, "false");
	   properties.put( "hibernate.temp.use_jdbc_metadata_defaults", "false");	   
	   properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
	   properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
	   exporter.getProperties().put(
			   ExporterConstants.METADATA_DESCRIPTOR, 
			   MetadataDescriptorFactory.createNativeDescriptor(null, null, properties));
	   exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
	   exporter.start();
	   File file = new File(srcDir, "hibernate.cfg.xml");
	   assertNull(
			   FileUtil.findFirstString(
					   Environment.SESSION_FACTORY_NAME, file ));
	   assertNotNull(
			   FileUtil.findFirstString( "hibernate.basic\">aValue<", file ));
	   assertNull(
			   FileUtil.findFirstString( Environment.HBM2DDL_AUTO, file ));
	   assertNull(
			   FileUtil.findFirstString("hibernate.temp.use_jdbc_metadata_defaults", file ));
	   exporter = ExporterFactory.createExporter(ExporterType.CFG);
	   properties = exporter.getProperties();
	   properties.put( Environment.HBM2DDL_AUTO, "validator");   
	   properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
	   properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
	   exporter.getProperties().put(
			   ExporterConstants.METADATA_DESCRIPTOR, 
			   MetadataDescriptorFactory.createNativeDescriptor(null, null, properties));
	   exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
	   exporter.start();
	   assertNotNull(
			   FileUtil.findFirstString( Environment.HBM2DDL_AUTO, file ));
	   exporter = ExporterFactory.createExporter(ExporterType.CFG);
	   properties = exporter.getProperties();
	   properties.put( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, FakeTransactionManagerLookup.class.getName()); // Hack for seam-gen console configurations
	   properties.put(AvailableSettings.DIALECT, HibernateUtil.Dialect.class.getName());
	   properties.put(AvailableSettings.CONNECTION_PROVIDER, ConnectionProvider.class.getName());
	   exporter.getProperties().put(
			   ExporterConstants.METADATA_DESCRIPTOR, 
			   MetadataDescriptorFactory.createNativeDescriptor(null, null, properties));
	   exporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, srcDir);
	   exporter.start();
	   assertNotNull(
			   FileUtil.findFirstString( AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, file ));
	}
		
	@Test
	public void testFileExistence() {
		JUnitUtil.assertIsNonEmptyFile(new File(srcDir, "hibernate.cfg.xml") );
	}

	@Test
	public void testArtifactCollection() {
		ArtifactCollector ac = (ArtifactCollector)cfgexporter.getProperties().get(ExporterConstants.ARTIFACT_COLLECTOR);
		assertEquals(1, ac.getFileCount("cfg.xml"));
	}
	
	@Test
	public void testNoVelocityLeftOvers() {
       assertNull(FileUtil.findFirstString("${",new File(srcDir, "hibernate.cfg.xml")));
	}

}
