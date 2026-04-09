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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.export.common.AbstractExporter;
import org.hibernate.tool.reveng.internal.export.common.TemplateHelper;
import org.hibernate.tool.reveng.internal.export.hbm.HbmExporter;
import org.hibernate.tool.reveng.internal.export.java.Cfg2JavaTool;
import org.hibernate.tool.reveng.internal.export.java.EntityPOJOClass;
import org.hibernate.tool.reveng.internal.export.java.POJOClass;
import org.hibernate.tool.orm.jbt.internal.factory.ConfigurationWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.factory.HbmExporterWrapperFactory;
import org.hibernate.tool.orm.jbt.internal.util.ConfigurationMetadataDescriptor;
import org.hibernate.tool.orm.jbt.internal.util.DummyMetadataBuildingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HbmExporterWrapperTest {

	private HbmExporterWrapper hbmExporterWrapper = null; 
	private HbmExporter wrappedHbmExporter = null;

	private ConfigurationWrapper cfg = null;
	private File f = null;
	
	private boolean delegateHasExported = false;
	private boolean templateProcessed = false;

	@TempDir private File tempFolder;
	
	@BeforeEach
	public void beforeEach() {
		cfg = ConfigurationWrapperFactory.createNativeConfigurationWrapper();
		f = new File(tempFolder, "foo");
		hbmExporterWrapper = HbmExporterWrapperFactory.createHbmExporterWrapper(cfg, f);
		wrappedHbmExporter = (HbmExporter)hbmExporterWrapper.getWrappedObject();
	}
	
	@Test
	public void testConstruction() throws Exception {
		assertTrue(tempFolder.exists());
		assertFalse(f.exists());
		assertNotNull(wrappedHbmExporter);
		assertNotNull(hbmExporterWrapper);
		assertSame(f, wrappedHbmExporter.getProperties().get(ExporterConstants.OUTPUT_FILE_NAME));
		ConfigurationMetadataDescriptor descriptor = (ConfigurationMetadataDescriptor)wrappedHbmExporter
				.getProperties().get(ExporterConstants.METADATA_DESCRIPTOR);
		assertNotNull(descriptor);
		Field configurationField = ConfigurationMetadataDescriptor.class.getDeclaredField("configuration");
		configurationField.setAccessible(true);
		assertSame(cfg.getWrappedObject(), configurationField.get(descriptor));
	}
	
	@Test
	public void testStart() throws Exception {
		MetadataDescriptor descriptor = new TestMetadataDescriptor();
		Properties properties = wrappedHbmExporter.getProperties();
		properties.put(ExporterConstants.METADATA_DESCRIPTOR, descriptor);
		properties.put(ExporterConstants.DESTINATION_FOLDER, tempFolder);
		final File fooHbmXml = new File(tempFolder, "Foo.hbm.xml");
		// First without a 'delegate' exporter
		assertFalse(fooHbmXml.exists());
		hbmExporterWrapper.start();
		assertTrue(fooHbmXml.exists());
		assertTrue(fooHbmXml.delete());
		// Now set a 'delegate' and invoke 'start' again
		Object delegate = new Object() {			
			@SuppressWarnings("unused")
			public void exportPojo(Map<Object, Object> map, Object pojoClass, String qualifiedDeclarationName) {
				try {
					FileWriter fw = new FileWriter(fooHbmXml);
					fw.write("<someDummyXml/>");
					fw.close();
					delegateHasExported = true;
				} catch (Throwable t) {
					fail(t);
				}
			}
		};
		Field delegateField = wrappedHbmExporter.getClass().getDeclaredField("delegateExporter");
		delegateField.setAccessible(true);
		delegateField.set(wrappedHbmExporter, delegate);
		assertFalse(delegateHasExported);
		hbmExporterWrapper.start();
		assertTrue(delegateHasExported);
	}
	
	@Test
	public void testGetOutputDirectory() {
		assertNull(hbmExporterWrapper.getOutputDirectory());
		File file = new File("testGetOutputDirectory");
		wrappedHbmExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, file);
		assertSame(file, hbmExporterWrapper.getOutputDirectory());
	}
	
	@Test
	public void testSetOutputDirectory() {
		assertNull(wrappedHbmExporter.getProperties().get(ExporterConstants.DESTINATION_FOLDER));
		File file = new File("testSetOutputDirectory");
		hbmExporterWrapper.setOutputDirectory(file);
		assertSame(file, wrappedHbmExporter.getProperties().get(ExporterConstants.DESTINATION_FOLDER));
	}
	
	@Test
	public void testExportPOJO() throws Exception {
		// first without a delegate exporter
		Map<Object, Object> context = new HashMap<>();
		PersistentClass pc = new RootClass(DummyMetadataBuildingContext.INSTANCE);
		pc.setEntityName("foo");
		pc.setClassName("foo");
		POJOClass pojoClass = new EntityPOJOClass(pc, new Cfg2JavaTool());
		TemplateHelper templateHelper = new TemplateHelper() {
		    public void processTemplate(String templateName, Writer output, String rootContext) {
		    	templateProcessed = true;
		    }
		};
		templateHelper.init(null, new String[] {});
		Field templateHelperField = AbstractExporter.class.getDeclaredField("vh");
		templateHelperField.setAccessible(true);
		templateHelperField.set(wrappedHbmExporter, templateHelper);
		assertFalse(templateProcessed);
		assertFalse(delegateHasExported);
		hbmExporterWrapper.exportPOJO(context, pojoClass);
		assertTrue(templateProcessed);
		assertFalse(delegateHasExported);
		// now with a delegate exporter
		templateProcessed = false;
		Object delegate = new Object() {
			@SuppressWarnings("unused")
			private void exportPojo(Map<Object, Object> map, Object object, String string) {
				assertSame(map, context);
				assertSame(object, pojoClass);
				assertEquals(string, pojoClass.getQualifiedDeclarationName());
				delegateHasExported = true;
			}
		};
		Field delegateField = wrappedHbmExporter.getClass().getDeclaredField("delegateExporter");
		delegateField.setAccessible(true);
		delegateField.set(wrappedHbmExporter, delegate);
		assertFalse(templateProcessed);
		assertFalse(delegateHasExported);
		hbmExporterWrapper.exportPOJO(context, pojoClass);
		assertFalse(templateProcessed);
		assertTrue(delegateHasExported);
	}
	
	@Test
	public void testSetExportPOJODelegate() throws Exception {
		Object delegate = new Object() {			
			@SuppressWarnings("unused")
			public void exportPojo(Map<Object, Object> map, Object pojoClass, String qualifiedDeclarationName) { }
		};
		Field delegateField = wrappedHbmExporter.getClass().getDeclaredField("delegateExporter");
		delegateField.setAccessible(true);
		assertNull(delegateField.get(wrappedHbmExporter));
		hbmExporterWrapper.setExportPOJODelegate(delegate);
		assertSame(delegate, delegateField.get(wrappedHbmExporter));
	}

	private static class TestMetadataDescriptor implements MetadataDescriptor {
		@Override
		public Metadata createMetadata() {
			return (Metadata)Proxy.newProxyInstance(
					getClass().getClassLoader(), 
					new Class<?>[] { Metadata.class }, 
					new TestInvocationHandler());
		}
		@Override
		public Properties getProperties() {
			Properties properties = new Properties();
			properties.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
			return properties;
		}	
	}
	
	private static class TestInvocationHandler implements InvocationHandler {
		private ArrayList<PersistentClass> entities = new ArrayList<PersistentClass>();
		private ArrayList<Table> tables = new ArrayList<Table>();
		private TestInvocationHandler() {
			RootClass persistentClass = new RootClass(DummyMetadataBuildingContext.INSTANCE);
			Table table = new Table("JBoss Tools", "FOO");
			Column keyColumn = new Column("BAR");
			SimpleValue key = new BasicValue(DummyMetadataBuildingContext.INSTANCE);
			key.setTable(table);
			key.setTypeName("String");
			key.addColumn(keyColumn);
			persistentClass.setClassName("Foo");
			persistentClass.setEntityName("Foo");
			persistentClass.setJpaEntityName("Foo");
			persistentClass.setTable(table);
			persistentClass.setIdentifier(key);	
			entities.add(persistentClass);
			tables.add(table);
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("getEntityBindings")) {
				return entities;
			} else if (method.getName().equals("collectTableMappings")) {
				return tables;
			} else if (method.getName().equals("getImports")) {
				return Collections.emptyMap();
			}
			return null;
		}		
	}
		
}
