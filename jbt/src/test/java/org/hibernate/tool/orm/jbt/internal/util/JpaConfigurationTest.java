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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.helpers.DefaultHandler;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class JpaConfigurationTest {
	
	private static final String PERSISTENCE_XML = 
			"<persistence version='2.2'" +
	        "  xmlns='http://xmlns.jcp.org/xml/ns/persistence'" +
		    "  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" +
	        "  xsi:schemaLocation='http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd'>" +
	        "  <persistence-unit name='foobar'>" +
	        "    <class>"+ FooBar.class.getName()  +"</class>" +
	        "    <properties>" +
	        "      <property name='" + AvailableSettings.DIALECT + "' value='" + MockDialect.class.getName() + "'/>" +
	        "      <property name='" + AvailableSettings.CONNECTION_PROVIDER + "' value='" + MockConnectionProvider.class.getName() + "'/>" +
	        "      <property name='foo' value='bar'/>" +
	        "    </properties>" +
	        "  </persistence-unit>" +
			"</persistence>";
	
	private ClassLoader original = null;
	
	@TempDir
	public File tempRoot;
	
	@BeforeEach
	public void beforeEach() throws Exception {
		tempRoot = Files.createTempDirectory("temp").toFile();
		File metaInf = new File(tempRoot, "META-INF");
		metaInf.mkdirs();
		File persistenceXml = new File(metaInf, "persistence.xml");
		persistenceXml.createNewFile();
		FileWriter fileWriter = new FileWriter(persistenceXml);
		fileWriter.write(PERSISTENCE_XML);
		fileWriter.close();
		original = Thread.currentThread().getContextClassLoader();
		ClassLoader urlCl = URLClassLoader.newInstance(
				new URL[] { new URL(tempRoot.toURI().toURL().toString())} , 
				original);
		Thread.currentThread().setContextClassLoader(urlCl);
	}
	
	@AfterEach
	public void afterEach() {
		Thread.currentThread().setContextClassLoader(original);
	}
	
	@Test
	public void testConstruction() {
		Properties properties = new Properties();
		properties.put("foo", "bar");
		JpaConfiguration jpaConfiguration = new JpaConfiguration("barfoo", properties);
		assertNotNull(jpaConfiguration);
		assertEquals("barfoo", jpaConfiguration.persistenceUnit);
		assertEquals("bar", jpaConfiguration.getProperties().get("foo"));
	}
	
	@Test
	public void testGetMetadata() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		assertNull(jpaConfiguration.metadata);
		Metadata metadata = jpaConfiguration.getMetadata();
		assertNotNull(metadata.getEntityBinding(FooBar.class.getName()));
		assertSame(metadata, jpaConfiguration.metadata);
	}
	
	@Test
	public void testBuildSessionFactory() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		assertNull(jpaConfiguration.sessionFactory);
		SessionFactory sessionFactory = jpaConfiguration.buildSessionFactory();
		assertNotNull(sessionFactory);
		assertSame(sessionFactory, jpaConfiguration.sessionFactory);
	}
	
	@Test
	public void testSetProperties() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		jpaConfiguration.metadata = (Metadata)createDummy(Metadata.class);
		jpaConfiguration.sessionFactory = (SessionFactory)createDummy(SessionFactoryImplementor.class);
		assertNull(jpaConfiguration.getProperty("foo"));
		Properties properties = new Properties();
		properties.put("foo", "bar");
		Object result = jpaConfiguration.setProperties(properties);
		assertSame(result, jpaConfiguration);
		assertNull(jpaConfiguration.metadata);
		assertNull(jpaConfiguration.sessionFactory);
		assertEquals("bar", jpaConfiguration.getProperty("foo"));
	}
	
	@Test
	public void testAddProperties() {
		Properties properties = new Properties();
		properties.put("foo", "bar");
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", properties);
		jpaConfiguration.metadata = (Metadata)createDummy(Metadata.class);
		jpaConfiguration.sessionFactory = (SessionFactory)createDummy(SessionFactoryImplementor.class);
		assertEquals("bar", jpaConfiguration.getProperty("foo"));
		assertNull(jpaConfiguration.getProperty("bar"));
		properties = new Properties();
		properties.put("bar", "foo");
		Object result = jpaConfiguration.addProperties(properties);
		assertSame(result, jpaConfiguration);
		assertNull(jpaConfiguration.metadata);
		assertNull(jpaConfiguration.sessionFactory);
		assertEquals("foo", jpaConfiguration.getProperty("bar"));
	}
	
	@Test
	public void testGetPersistenceUnit() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("barfoo", null);
		assertNotEquals("foobar", jpaConfiguration.getPersistenceUnit());
		jpaConfiguration.persistenceUnit = "foobar";
		assertEquals("foobar", jpaConfiguration.getPersistenceUnit());
	}
	
	@Test
	public void testInitialize() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		assertNull(jpaConfiguration.metadata);
		assertNull(jpaConfiguration.sessionFactory);
		assertNull(jpaConfiguration.getProperties().get("foo"));
		jpaConfiguration.initialize();
		assertNotNull(jpaConfiguration.metadata);
		assertNotNull(jpaConfiguration.metadata.getEntityBinding(FooBar.class.getName()));
		assertNotNull(jpaConfiguration.sessionFactory);
		assertEquals("bar", ((SessionFactoryImplementor)jpaConfiguration.sessionFactory).getProperties().get("foo"));
		assertEquals("bar", jpaConfiguration.getProperties().get("foo"));
	}
	
	@Test
	public void testGetClassMappings() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		Iterator<PersistentClass> classMappings = jpaConfiguration.getClassMappings();
		assertNotNull(classMappings);
		assertTrue(classMappings.hasNext());
		PersistentClass pc = classMappings.next();
		assertSame(pc.getMappedClass(), FooBar.class);
	}
	
	@Test
	public void testAddFile() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.addFile(new File("Foo"));
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addFile' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testAddClass() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.addClass(Object.class);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addClass' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testSetEntityResolver() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.setEntityResolver(new DefaultHandler());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setEntityResolver' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testSetNamingStrategy() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.setNamingStrategy(new ImplicitNamingStrategyJpaCompliantImpl());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setNamingStrategy' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testConfigure() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		try {
			jpaConfiguration.configure(new File(""));
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'configure' should not be called on instances of " + JpaConfiguration.class.getName());
		}
		try {
			jpaConfiguration.configure();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'configure' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}
	
	@Test
	public void testBuildMappings() throws Exception {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		Field metadataField = JpaConfiguration.class.getDeclaredField("metadata");
		metadataField.setAccessible(true);
		assertNull(metadataField.get(jpaConfiguration));
		jpaConfiguration.buildMappings();
		assertNotNull(metadataField.get(jpaConfiguration));
	}
	
	@Test
	public void testSetPreferBasicCompositeIds() {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		try {
			jpaConfiguration.setPreferBasicCompositeIds(false);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setPreferBasicCompositeIds' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testSetReverseEngineeringStrategy() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.setReverseEngineeringStrategy(new DefaultStrategy());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setReverseEngineeringStrategy' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testReadFromJDBC() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.readFromJDBC();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'readFromJDBC' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testGetNamingStrategy() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.getNamingStrategy();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'getNamingStrategy' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testGetClassMapping() throws Exception {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		assertNull(jpaConfiguration.getClassMapping("Bar"));
		assertNotNull(jpaConfiguration.getClassMapping(FooBar.class.getName()));
	}
	
	@Test
	public void testGetGetEntityResolver() {
		try {
			JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
			jpaConfiguration.getEntityResolver();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'getEntityResolver' should not be called on instances of " + JpaConfiguration.class.getName());
		}
	}

	@Test
	public void testGetTableMappings() throws Exception {
		JpaConfiguration jpaConfiguration = new JpaConfiguration("foobar", null);
		Iterator<Table> tableMappings = jpaConfiguration.getTableMappings();
		assertNotNull(tableMappings);
		assertTrue(tableMappings.hasNext());
		Table table = tableMappings.next();
		assertEquals(table.getName(), "JpaConfigurationTest$FooBar");
		assertFalse(tableMappings.hasNext());
	}
	
	@Entity public class FooBar {
		@Id public int id;
	}

	private Object createDummy(Class<?> interf) {
		return Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class[] { interf },
				new InvocationHandler() {					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});

	}
}
