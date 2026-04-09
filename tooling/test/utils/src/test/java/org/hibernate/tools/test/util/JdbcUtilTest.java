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
package org.hibernate.tools.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JdbcUtilTest {
	
	@TempDir
	public File outputFolder = new File("output");
	
	@BeforeEach
	public void setUp() throws Exception {
		clearConnectionTable();
		createHibernateProperties("sa", "", "jdbc:h2:mem:test");
		setUpClassLoader();
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		clearConnectionTable();
		Files.delete(new File(outputFolder, "hibernate.properties").toPath());
		restoreClassLoader();
	}

	@Test
	public void testGetConnectionPropertiesForTest() throws Exception {
		Properties properties = JdbcUtil.getConnectionProperties(null);
		assertEquals("jdbc:h2:mem:test", properties.get("url"));
		assertEquals("sa", properties.get("user"));
		assertEquals("", properties.get("password"));
	}
	
	@Test
	public void testEstablishJdbcConnection() throws Exception {
		assertNull(JdbcUtil.CONNECTION_TABLE.get(this));
		JdbcUtil.establishJdbcConnection(this);
		Connection connection = JdbcUtil.CONNECTION_TABLE.get(this);
		assertFalse(connection.isClosed());
	}
	
	@Test
	public void testReleaseJdbcConnection() throws Exception {
		Connection connection = 
				DriverManager.getConnection("jdbc:h2:mem:test;USER=sa");
		JdbcUtil.CONNECTION_TABLE.put(this, connection);
		assertNotNull(JdbcUtil.CONNECTION_TABLE.get(this));
		assertFalse(connection.isClosed());
		JdbcUtil.releaseJdbcConnection(this);
		assertNull(JdbcUtil.CONNECTION_TABLE.get(this));
		assertTrue(connection.isClosed());
	}
	
	@Test
	public void testExecuteDDL() throws Exception {
		Connection connection = DriverManager
				.getConnection("jdbc:h2:mem:test;USER=sa");
		JdbcUtil.CONNECTION_TABLE.put(this, connection);
		ResultSet resultSet = connection
				.getMetaData()
				.getTables(null, null, "FOO", null);
		assertFalse(resultSet.next());
		String[] sqls = new String[] {
				"CREATE TABLE FOO (BAR INT)"
		};
		JdbcUtil.executeSql(this, sqls);
		resultSet = connection
				.getMetaData()
				.getTables(null, null, "FOO", null);
		assertTrue(resultSet.next());
	}
	
	@Test
	public void testToIdentifier() throws Exception {
		MetaDataInvocationHandler metaDataInvocationHandler = new MetaDataInvocationHandler();
		ConnectionInvocationHandler connectionInvocationHandler = new ConnectionInvocationHandler();
		connectionInvocationHandler.metaDataInvocationHandler = metaDataInvocationHandler;
		Connection connection = (Connection)Proxy.newProxyInstance(
				getClass().getClassLoader(), 
				new Class[] { Connection.class }, 
				connectionInvocationHandler );
		JdbcUtil.CONNECTION_TABLE.put(this, connection);
		metaDataInvocationHandler.whichCase = "MIXED";
		assertEquals("Foo", JdbcUtil.toIdentifier(this, "Foo"));
		metaDataInvocationHandler.whichCase = "UPPER";
		assertEquals("FOO", JdbcUtil.toIdentifier(this, "Foo"));
		metaDataInvocationHandler.whichCase = "LOWER";
		assertEquals("foo", JdbcUtil.toIdentifier(this, "Foo"));
	}
	
	@Test
	public void testIsDatabaseOnline() throws Exception {
		assertTrue(JdbcUtil.isDatabaseOnline(null));
//		new File(outputFolder, "hibernate.properties").delete();
		Files.delete(new File(outputFolder, "hibernate.properties").toPath());
		createHibernateProperties("foo", "bar", "jdbc:sqlserver://org.foo.bar:1433");
		assertFalse(JdbcUtil.isDatabaseOnline(null));
	}
	
	private void clearConnectionTable() throws Exception {
		for (Connection c : JdbcUtil.CONNECTION_TABLE.values()) {
			c.close();
		}
		JdbcUtil.CONNECTION_TABLE.clear();
	}
	
	private void createHibernateProperties(
			String user, 
			String password, 
			String url) 
					throws Exception {
		Properties properties = new Properties();
		properties.put("hibernate.connection.username", user);
		properties.put("hibernate.connection.password", password);
		properties.put("hibernate.connection.url", url);
		File propertiesFile = new File(outputFolder, "hibernate.properties");
		FileWriter writer = new FileWriter(propertiesFile);
		properties.store(writer, null);
		writer.close();
	}
	
	private void setUpClassLoader() throws Exception {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				new URLClassLoader(
					new URL[] { outputFolder.toURI().toURL() }, 
					currentClassLoader));
	}
	
	private void restoreClassLoader() {
		URLClassLoader currentClassLoader = 
				(URLClassLoader)Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(currentClassLoader.getParent());
	}
	
	private static class MetaDataInvocationHandler implements InvocationHandler {
		String whichCase;
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if ("storesLowerCaseIdentifiers".equals(methodName)) {
				return "LOWER".equals(whichCase);
			} else if ("storesUpperCaseIdentifiers".equals(methodName)) {
				return "UPPER".equals(whichCase);
			} else {
				return false;
			}
		}	
	}
	
	private static class ConnectionInvocationHandler implements InvocationHandler {
		InvocationHandler metaDataInvocationHandler;
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("getMetaData")) {
				return (DatabaseMetaData)Proxy.newProxyInstance(
						getClass().getClassLoader(), 
						new Class[] { DatabaseMetaData.class } , 
						metaDataInvocationHandler);
			} else {
				return null;
			}
		}	
	}
	
}
