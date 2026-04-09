/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.api.wrp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.cfg.Configuration;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.internal.export.cfg.CfgExporter;
import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.hibernate.tool.reveng.internal.export.common.GenericExporter;
import org.hibernate.tool.reveng.internal.export.ddl.DdlExporter;
import org.hibernate.tool.reveng.internal.export.query.QueryExporter;
import org.hibernate.tool.orm.jbt.internal.factory.ArtifactCollectorWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.ConfigurationWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.ExporterWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.util.ConfigurationMetadataDescriptor;
import org.hibernate.tool.orm.jbt.internal.util.DummyMetadataDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExporterWrapperTest {

	private ExporterWrapper exporterWrapper = null;
	private Exporter wrappedExporter = null;
	
	@BeforeEach
	public void beforeEach() {
		wrappedExporter = new TestExporter();
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(TestExporter.class.getName());
		wrappedExporter = (Exporter)exporterWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(exporterWrapper);
		assertNotNull(wrappedExporter);
	}

	@Test
	public void testSetConfiguration() throws Exception {
		Object metadataDescriptor = null;
		Properties properties = new Properties();
		ConfigurationWrapper configurationWrapper = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		Configuration configuration = (Configuration)configurationWrapper.getWrappedObject();
		configuration.setProperties(properties);
		Field field = ConfigurationMetadataDescriptor.class.getDeclaredField("configuration");
		field.setAccessible(true);
		// First use the TestExporter 
		metadataDescriptor = wrappedExporter.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(metadataDescriptor);
		assertTrue(metadataDescriptor instanceof ConfigurationMetadataDescriptor);
		assertNotSame(configuration, field.get(metadataDescriptor));
		exporterWrapper.setConfiguration(configurationWrapper);	
		metadataDescriptor = wrappedExporter.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(metadataDescriptor);
		assertTrue(metadataDescriptor instanceof ConfigurationMetadataDescriptor);
		assertSame(configuration, field.get(metadataDescriptor));
		// Now test with a CfgExporter
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(CfgExporter.class.getName());
		wrappedExporter = (Exporter)exporterWrapper.getWrappedObject();
		assertNotSame(properties, ((CfgExporter)exporterWrapper.getWrappedObject()).getCustomProperties());
		metadataDescriptor = wrappedExporter.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(metadataDescriptor);
		assertTrue(metadataDescriptor instanceof DummyMetadataDescriptor);
		exporterWrapper.setConfiguration(configurationWrapper);	
		assertSame(properties, ((CfgExporter)exporterWrapper.getWrappedObject()).getCustomProperties());
		metadataDescriptor = wrappedExporter.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(metadataDescriptor);
		assertTrue(metadataDescriptor instanceof ConfigurationMetadataDescriptor);
		assertSame(configuration, field.get(metadataDescriptor));
	}
	
	@Test
	public void testSetArtifactCollector() {
		ArtifactCollectorWrapper artifactCollectorWrapper = ArtifactCollectorWrapperFactory.createArtifactCollectorWrapper();
		Object wrappedArtifactCollector = artifactCollectorWrapper.getWrappedObject();
		assertNotSame(wrappedArtifactCollector, wrappedExporter.getProperties().get(ExporterConstants.ARTIFACT_COLLECTOR));
		exporterWrapper.setArtifactCollector(artifactCollectorWrapper);
		assertSame(wrappedArtifactCollector, wrappedExporter.getProperties().get(ExporterConstants.ARTIFACT_COLLECTOR));
	}
	
	@Test
	public void testSetOutputDirectory() {
		File file = new File("");
		assertNotSame(file, wrappedExporter.getProperties().get(ExporterConstants.DESTINATION_FOLDER));		
		exporterWrapper.setOutputDirectory(file);
		assertSame(file, wrappedExporter.getProperties().get(ExporterConstants.DESTINATION_FOLDER));		
	}
	
	@Test
	public void testSetTemplatePath() {
		String[] templatePath = new String[] {};
		assertNotSame(templatePath, wrappedExporter.getProperties().get(ExporterConstants.TEMPLATE_PATH));		
		exporterWrapper.setTemplatePath(templatePath);
		assertSame(templatePath, wrappedExporter.getProperties().get(ExporterConstants.TEMPLATE_PATH));		
	}
	
	@Test
	public void testStart() throws Exception {
		assertFalse(((TestExporter)exporterWrapper.getWrappedObject()).started);
		exporterWrapper.start();
		assertTrue(((TestExporter)exporterWrapper.getWrappedObject()).started);
	}
	
	@Test
	public void testGetProperties() throws Exception {
		Field propertiesField = AbstractExporter.class.getDeclaredField("properties");
		propertiesField.setAccessible(true);
		Properties properties = new Properties();
		assertNotNull(exporterWrapper.getProperties());
		assertNotSame(properties, exporterWrapper.getProperties());
		propertiesField.set(exporterWrapper.getWrappedObject(), properties);
		assertSame(properties, exporterWrapper.getProperties());
	}
	
	@Test
	public void testGetGenericExporter() {
		// TestExporter should not return a GenericExporterFacade instance
		assertNull(exporterWrapper.getGenericExporter());
		// try now with a GenericExporter
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(GenericExporter.class.getName());
		GenericExporterWrapper genericExporterWrapper = exporterWrapper.getGenericExporter();
		assertSame(exporterWrapper.getWrappedObject(), genericExporterWrapper.getWrappedObject());
	}
	
	@Test
	public void testGetHbm2DDlExporter() {
		// TestExporter should not return a GenericExporterFacade instance
		assertNull(exporterWrapper.getHbm2DDLExporter());
		// try now with a DdlExporter
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(DdlExporter.class.getName());
		DdlExporterWrapper ddlExporterWrapper = exporterWrapper.getHbm2DDLExporter();
		assertSame(exporterWrapper.getWrappedObject(), ddlExporterWrapper.getWrappedObject());
	}
	
	@Test
	public void testGetQueryExporter() {
		// TestExporter should not return a GenericExporterFacade instance
		assertNull(exporterWrapper.getQueryExporter());
		// try now with a QueryExporter
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(QueryExporter.class.getName());
		QueryExporterWrapper queryExporterWrapper = exporterWrapper.getQueryExporter();
		assertSame(exporterWrapper.getWrappedObject(), queryExporterWrapper.getWrappedObject());
	}
	
	@Test
	public void testSetCustomProperties() {
		Properties properties = new Properties();
		// 'setCustomProperties()' should not be called on other exporters than CfgExporter
		TestExporter wrappedTestExporter = (TestExporter)exporterWrapper.getWrappedObject();
		assertNull(wrappedTestExporter.props);
		exporterWrapper.setCustomProperties(properties);
		assertNull(wrappedTestExporter.props);
		// try now with CfgExporter 
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(CfgExporter.class.getName());
		CfgExporter wrappedCfgExporter = (CfgExporter)exporterWrapper.getWrappedObject();
		assertNotSame(properties, wrappedCfgExporter.getCustomProperties());
		exporterWrapper.setCustomProperties(properties);
		assertSame(properties, wrappedCfgExporter.getCustomProperties());
	}
	
	@Test
	public void testSetOutput() {
		StringWriter stringWriter = new StringWriter();
		// 'setOutput()' should not be called on other exporters than CfgExporter
		TestExporter wrappedTestExporter = (TestExporter)exporterWrapper.getWrappedObject();
		assertNull(wrappedTestExporter.output);
		exporterWrapper.setOutput(stringWriter);
		assertNull(wrappedTestExporter.output);
		// try now with CfgExporter 
		exporterWrapper = ExporterWrapperFactory.createExporterWrapper(CfgExporter.class.getName());
		CfgExporter wrappedCfgExporter = (CfgExporter)exporterWrapper.getWrappedObject();
		assertNotSame(stringWriter, wrappedCfgExporter.getOutput());
		exporterWrapper.setOutput(stringWriter);
		assertSame(stringWriter, wrappedCfgExporter.getOutput());
	}
	
	public static class TestExporter extends AbstractExporter {
		private boolean started = false;
		private Properties props = null;
		private StringWriter output = null;
		@Override protected void doStart() {}
		@Override public void start() { started = true; }
		public void setCustomProperties(Properties p) {
			props = p;
		}
		
	}
	
}
