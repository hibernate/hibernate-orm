/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2022-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.DefaultHandler;

public class NativeConfigurationTest {
	
	private static final String TEST_HBM_XML_STRING =
			"<hibernate-mapping package='org.hibernate.tool.orm.jbt.internal.util'>" +
			"  <class name='NativeConfigurationTest$Foo'>" + 
			"    <id name='id'/>" +
			"  </class>" +
			"</hibernate-mapping>";
	
	static class Foo {
		public String id;
	}
	
	private NativeConfiguration nativeConfiguration = null;
	
	@BeforeEach
	public void beforeEach() {
		nativeConfiguration = new NativeConfiguration();
		nativeConfiguration.setProperty(AvailableSettings.DIALECT, MockDialect.class.getName());
		nativeConfiguration.setProperty(AvailableSettings.CONNECTION_PROVIDER, MockConnectionProvider.class.getName());
	}
	
	@Test
	public void testSetEntityResolver() throws Exception {
		Field field = NativeConfiguration.class.getDeclaredField("entityResolver");
		field.setAccessible(true);
		assertNull(field.get(nativeConfiguration));
		EntityResolver entityResolver = new DefaultHandler();
		nativeConfiguration.setEntityResolver(entityResolver);
		assertNotNull(field.get(nativeConfiguration));
		assertSame(field.get(nativeConfiguration), entityResolver);
	}
	
	@Test
	public void testSetNamingStrategy() throws Exception {
		Field field = NativeConfiguration.class.getDeclaredField("namingStrategy");
		field.setAccessible(true);
		assertNull(field.get(nativeConfiguration));
		ImplicitNamingStrategy namingStrategy = new ImplicitNamingStrategyJpaCompliantImpl();
		nativeConfiguration.setNamingStrategy(namingStrategy);
		assertNotNull(field.get(nativeConfiguration));
		assertSame(field.get(nativeConfiguration), namingStrategy);
	}

	@Test
	public void testConfigureDocument() throws Exception {
		Document document = DocumentBuilderFactory
				.newInstance()
				.newDocumentBuilder()
				.newDocument();
		Element hibernateConfiguration = document.createElement("hibernate-configuration");
		document.appendChild(hibernateConfiguration);
		Element sessionFactory = document.createElement("session-factory");
		sessionFactory.setAttribute("name", "bar");
		hibernateConfiguration.appendChild(sessionFactory);
		Element mapping = document.createElement("mapping");
		mapping.setAttribute("resource", "Foo.hbm.xml");
		sessionFactory.appendChild(mapping);
		
		URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
		File hbmXmlFile = new File(new File(url.toURI()), "Foo.hbm.xml");
		hbmXmlFile.deleteOnExit();
		FileWriter fileWriter = new FileWriter(hbmXmlFile);
		fileWriter.write(TEST_HBM_XML_STRING);
		fileWriter.close();

		String fooClassName = 
				"org.hibernate.tool.orm.jbt.internal.util.NativeConfigurationTest$Foo";
		Metadata metadata = MetadataHelper.getMetadata(nativeConfiguration);
		assertNull(metadata.getEntityBinding(fooClassName));
		nativeConfiguration.configure(document);
		metadata = MetadataHelper.getMetadata(nativeConfiguration);
		assertNotNull(metadata.getEntityBinding(fooClassName));
	}
	
	@Test
	public void testBuildMappings() throws Exception {
		Field metadataField = NativeConfiguration.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		assertNull(metadataField.get(nativeConfiguration));
		nativeConfiguration.buildMappings();
		assertNotNull(metadataField.get(nativeConfiguration));
	}
	
	@Test
	public void testGetClassMappings() throws Exception {
		String fooHbmXmlFilePath = "org/hibernate/tool/orm/jbt/internal/util";
		String fooHbmXmlFileName = "NativeConfigurationTest$Foo.hbm.xml";
		String fooClassName = 
				"org.hibernate.tool.orm.jbt.internal.util.NativeConfigurationTest$Foo";
		URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
		File hbmXmlFileDir = new File(new File(url.toURI()),fooHbmXmlFilePath);
		hbmXmlFileDir.deleteOnExit();
		hbmXmlFileDir.mkdirs();
		File hbmXmlFile = new File(hbmXmlFileDir, fooHbmXmlFileName);
		hbmXmlFile.deleteOnExit();
		FileWriter fileWriter = new FileWriter(hbmXmlFile);
		fileWriter.write(TEST_HBM_XML_STRING);
		fileWriter.close();
		Field metadataField = NativeConfiguration.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		Iterator<PersistentClass> classesIterator = nativeConfiguration.getClassMappings();
		assertFalse(classesIterator.hasNext());
		metadataField.set(nativeConfiguration, null);
		nativeConfiguration.addClass(Foo.class);
		classesIterator = nativeConfiguration.getClassMappings();
		assertTrue(classesIterator.hasNext());
		PersistentClass pc = classesIterator.next();
		assertEquals(fooClassName, pc.getEntityName());
	}
	
	@Test
	public void testGetNamingStrategy() throws Exception {
		Field field = NativeConfiguration.class.getDeclaredField("namingStrategy");
		field.setAccessible(true);
		assertNull(nativeConfiguration.getNamingStrategy());
		ImplicitNamingStrategy namingStrategy = new ImplicitNamingStrategyJpaCompliantImpl();
		field.set(nativeConfiguration, namingStrategy);
		assertNotNull(nativeConfiguration.getNamingStrategy());
		assertSame(nativeConfiguration.getNamingStrategy(), namingStrategy);
	}
	
	@Test
	public void testSetPreferBasicCompositeIds() {
		try {
			nativeConfiguration.setPreferBasicCompositeIds(false);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setPreferBasicCompositeIds' should not be called on instances of " + NativeConfiguration.class.getName());
		}
	}

	@Test
	public void testSetReverseEngineeringStrategy() {
		try {
			nativeConfiguration.setReverseEngineeringStrategy(new DefaultStrategy());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setReverseEngineeringStrategy' should not be called on instances of " + NativeConfiguration.class.getName());
		}
	}

	@Test
	public void testReadFromJDBC() {
		try {
			nativeConfiguration.readFromJDBC();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'readFromJDBC' should not be called on instances of " + NativeConfiguration.class.getName());
		}
	}

	@Test
	public void testGetEntityResolver() throws Exception {
		Field field = NativeConfiguration.class.getDeclaredField("entityResolver");
		field.setAccessible(true);
		assertNull(nativeConfiguration.getEntityResolver());
		EntityResolver entityResolver = new DefaultHandler();
		field.set(nativeConfiguration, entityResolver);
		assertNotNull(nativeConfiguration.getEntityResolver());
		assertSame(nativeConfiguration.getEntityResolver(), entityResolver);
	}
	
	@Test
	public void testGetClassMapping() throws Exception {
		String fooHbmXmlFilePath = "org/hibernate/tool/orm/jbt/internal/util";
		String fooHbmXmlFileName = "NativeConfigurationTest$Foo.hbm.xml";
		String fooClassName = 
				"org.hibernate.tool.orm.jbt.internal.util.NativeConfigurationTest$Foo";
		URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
		File hbmXmlFileDir = new File(new File(url.toURI()),fooHbmXmlFilePath);
		hbmXmlFileDir.deleteOnExit();
		hbmXmlFileDir.mkdirs();
		File hbmXmlFile = new File(hbmXmlFileDir, fooHbmXmlFileName);
		hbmXmlFile.deleteOnExit();
		FileWriter fileWriter = new FileWriter(hbmXmlFile);
		fileWriter.write(TEST_HBM_XML_STRING);
		fileWriter.close();
		Field metadataField = NativeConfiguration.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		assertNull(nativeConfiguration.getClassMapping(fooClassName));
		metadataField.set(nativeConfiguration, null);
		nativeConfiguration.addClass(Foo.class);
		assertNotNull(nativeConfiguration.getClassMapping(fooClassName));
	}
	
	@Test
	public void testGetTableMappings() throws Exception {
		String fooHbmXmlFilePath = "org/hibernate/tool/orm/jbt/internal/util";
		String fooHbmXmlFileName = "NativeConfigurationTest$Foo.hbm.xml";
		URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
		File hbmXmlFileDir = new File(new File(url.toURI()),fooHbmXmlFilePath);
		hbmXmlFileDir.deleteOnExit();
		hbmXmlFileDir.mkdirs();
		File hbmXmlFile = new File(hbmXmlFileDir, fooHbmXmlFileName);
		hbmXmlFile.deleteOnExit();
		FileWriter fileWriter = new FileWriter(hbmXmlFile);
		fileWriter.write(TEST_HBM_XML_STRING);
		fileWriter.close();
		Field metadataField = NativeConfiguration.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		Iterator<Table> tableIterator = nativeConfiguration.getTableMappings();
		assertFalse(tableIterator.hasNext());
		metadataField.set(nativeConfiguration, null);
		nativeConfiguration.addClass(Foo.class);
		tableIterator = nativeConfiguration.getTableMappings();
		assertTrue(tableIterator.hasNext());
		Table table = tableIterator.next();
		assertEquals("NativeConfigurationTest$Foo", table.getName());
	}
	
	@Test
	public void testBuildSessionFactory() throws Exception {
		SessionFactory sessionFactory = nativeConfiguration.buildSessionFactory();
		assertNotNull(sessionFactory);
		assertTrue(sessionFactory instanceof SessionFactory);
	}
	
}
