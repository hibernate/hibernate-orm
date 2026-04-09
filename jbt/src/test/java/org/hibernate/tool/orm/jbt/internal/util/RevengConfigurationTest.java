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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;

import org.h2.Driver;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.helpers.DefaultHandler;

public class RevengConfigurationTest {

	private RevengConfiguration revengConfiguration = null;
	private Field propertyField = null;
	
	@BeforeAll
	public static void beforeAll() throws Exception {
		DriverManager.registerDriver(new Driver());		
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		revengConfiguration = new RevengConfiguration();
		propertyField = Configuration.class.getDeclaredField("properties");
		propertyField.setAccessible(true);	
	}
	
	@Test
	public void testInstance() {
		assertNotNull(revengConfiguration);
		assertTrue(revengConfiguration instanceof Configuration);
	}
	
	@Test
	public void testGetProperties() throws Exception {
		Properties properties = new Properties();
		assertNotNull(revengConfiguration.getProperties());
		assertNotSame(properties,  revengConfiguration.getProperties());
		propertyField.set(revengConfiguration, properties);
		assertSame(properties, revengConfiguration.getProperties());
	}
	
	@Test
	public void testSetProperties() throws Exception {
		Properties properties = new Properties();
		assertNotNull(propertyField.get(revengConfiguration));
		assertNotSame(properties,  propertyField.get(revengConfiguration));
		assertSame(revengConfiguration, revengConfiguration.setProperties(properties));
		assertSame(properties, propertyField.get(revengConfiguration));
	}
	
	@Test
	public void testGetProperty() throws Exception {
		assertNull(revengConfiguration.getProperty("foo"));
		((Properties)propertyField.get(revengConfiguration)).put("foo", "bar");
		assertEquals("bar", revengConfiguration.getProperty("foo"));
	}

	@Test
	public void testSetProperty() throws Exception {
		assertNull(((Properties)propertyField.get(revengConfiguration)).get("foo"));
		revengConfiguration.setProperty("foo", "bar");
		assertEquals("bar", ((Properties)propertyField.get(revengConfiguration)).get("foo"));
	}
	
	@Test
	public void testAddProperties() throws Exception {
		Properties properties = new Properties();
		properties.put("foo", "bar");
		assertNull(((Properties)propertyField.get(revengConfiguration)).get("foo"));
		revengConfiguration.addProperties(properties);
		assertEquals("bar", ((Properties)propertyField.get(revengConfiguration)).get("foo"));
	}
	
	@Test
	public void testGetReverseEngineeringStrategy() {
		RevengStrategy strategy = new DefaultStrategy();
		assertNull(revengConfiguration.getReverseEngineeringStrategy());
		revengConfiguration.revengStrategy = strategy;
		assertSame(strategy, revengConfiguration.getReverseEngineeringStrategy());
	}
	
	@Test
	public void testSetReverseEngineeringStrategy() {
		RevengStrategy strategy = new DefaultStrategy();
		assertNull(revengConfiguration.revengStrategy);
		revengConfiguration.setReverseEngineeringStrategy(strategy);
		assertSame(strategy, revengConfiguration.revengStrategy);
	}
	
	@Test
	public void testPreferBasicCompositeIds() throws Exception {
		assertTrue(revengConfiguration.preferBasicCompositeIds());
		((Properties)propertyField.get(revengConfiguration)).put(
				MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, false);		
		assertFalse(revengConfiguration.preferBasicCompositeIds());
	}
	
	@Test
	public void testSetPreferBasicCompositeIds() throws Exception {
		assertNull(
				((Properties)propertyField.get(revengConfiguration)).get(
						MetadataConstants.PREFER_BASIC_COMPOSITE_IDS));
		revengConfiguration.setPreferBasicCompositeIds(true);
		assertEquals(
				true, 
				((Properties)propertyField.get(revengConfiguration)).get(
						MetadataConstants.PREFER_BASIC_COMPOSITE_IDS));
	}
	
	@Test
	public void testGetMetadata() {
		Metadata metadata = (Metadata)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class[] { Metadata.class }, 
				new InvocationHandler() {					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return null;
					}
				});
		assertNull(revengConfiguration.getMetadata());
		revengConfiguration.metadata = metadata;
		assertSame(metadata, revengConfiguration.getMetadata());
	}
	
	@Test
	public void testReadFromJDBC() throws Exception {
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:test");
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE FOO(id int primary key, bar varchar(255))");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.connection.url", "jdbc:h2:mem:test");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.default_schema", "PUBLIC");
		revengConfiguration.revengStrategy = new DefaultStrategy();
		assertNull(revengConfiguration.metadata);
		revengConfiguration.readFromJDBC();
		assertNotNull(revengConfiguration.metadata);
		statement.execute("DROP TABLE FOO");
		statement.close();
		connection.close();
	}
	
	@Test
	public void testGetClassMappings() throws Exception {
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:test");
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE FOO(id int primary key, bar varchar(255))");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.connection.url", "jdbc:h2:mem:test");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.default_schema", "PUBLIC");
		revengConfiguration.revengStrategy = new DefaultStrategy();
		Iterator<PersistentClass> classMappings = revengConfiguration.getClassMappings();
		assertNotNull(classMappings);
		assertFalse(classMappings.hasNext());
		revengConfiguration.readFromJDBC();
		classMappings = revengConfiguration.getClassMappings();
		assertNotNull(classMappings);
		assertTrue(classMappings.hasNext());
		PersistentClass pc = classMappings.next();
		assertEquals(pc.getEntityName(), "Foo");
		assertFalse(classMappings.hasNext());
		statement.execute("DROP TABLE FOO");
		statement.close();
		connection.close();
	}
	
	@Test
	public void testGetClassMapping() throws Exception {
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:test");
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE FOO(id int primary key, bar varchar(255))");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.connection.url", "jdbc:h2:mem:test");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.default_schema", "PUBLIC");
		revengConfiguration.revengStrategy = new DefaultStrategy();
		assertNull(revengConfiguration.getClassMapping("Foo"));
		revengConfiguration.readFromJDBC();
		assertNotNull(revengConfiguration.getClassMapping("Foo"));
		statement.execute("DROP TABLE FOO");
		statement.close();
		connection.close();
	}
	
	@Test
	public void testGetTableMappings() throws Exception {
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:test");
		Statement statement = connection.createStatement();
		statement.execute("CREATE TABLE FOO(id int primary key, bar varchar(255))");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.connection.url", "jdbc:h2:mem:test");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.default_schema", "PUBLIC");
		revengConfiguration.revengStrategy = new DefaultStrategy();
		Iterator<Table> tableMappings = revengConfiguration.getTableMappings();
		assertNotNull(tableMappings);
		assertFalse(tableMappings.hasNext());
		revengConfiguration.readFromJDBC();
		tableMappings = revengConfiguration.getTableMappings();
		assertNotNull(tableMappings);
		assertTrue(tableMappings.hasNext());
		Table table = tableMappings.next();
		assertEquals(table.getName(), "FOO");
		assertFalse(tableMappings.hasNext());
		statement.execute("DROP TABLE FOO");
		statement.close();
		connection.close();
	}
	
	@Test
	public void testAddFile() {
		try {
			revengConfiguration.addFile(new File("Foo"));
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addFile' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testSetEntityResolver() {
		try {
			revengConfiguration.setEntityResolver(new DefaultHandler());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setEntityResolver' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testSetNamingStrategy() {
		try {
			revengConfiguration.setNamingStrategy(new ImplicitNamingStrategyJpaCompliantImpl());
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'setNamingStrategy' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testConfigure() {
		try {
			revengConfiguration.configure(new File(""));
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'configure' should not be called on instances of " + RevengConfiguration.class.getName());
		}
		try {
			revengConfiguration.configure();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'configure' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}
	
	@Test
	public void testAddClass() {
		try {
			revengConfiguration.addClass(Object.class);
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'addClass' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testBuildMappings() throws Exception {
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.connection.url", "jdbc:h2:mem:test");
		((Properties)propertyField.get(revengConfiguration)).put("hibernate.default_schema", "PUBLIC");
		assertNull(revengConfiguration.metadata);
		revengConfiguration.buildMappings();
		assertNotNull(revengConfiguration.metadata);
	}

	@Test
	public void testBuildSessionFactory() {
		try {
			revengConfiguration.buildSessionFactory();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'buildSessionFactory' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testGetNamingStrategy() {
		try {
			revengConfiguration.getNamingStrategy();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'getNamingStrategy' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

	@Test
	public void testGetGetEntityResolver() {
		try {
			revengConfiguration.getEntityResolver();
			fail();
		} catch (RuntimeException e) {
			assertEquals(
					e.getMessage(),
					"Method 'getEntityResolver' should not be called on instances of " + RevengConfiguration.class.getName());
		}
	}

}
