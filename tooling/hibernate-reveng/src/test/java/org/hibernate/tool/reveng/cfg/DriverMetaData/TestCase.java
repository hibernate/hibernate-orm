/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.cfg.DriverMetaData;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengDialectFactory;
import org.hibernate.tool.reveng.internal.core.dialect.JDBCMetaDataDialect;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private Properties properties = null;
	private ServiceRegistry serviceRegistry;

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		properties = Environment.getProperties();
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		serviceRegistry = ssrb.build();
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testExportedKeys() {
		RevengDialect dialect = new JDBCMetaDataDialect();
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = ssrb.build();
		ConnectionProvider connectionProvider =
				serviceRegistry.getService(ConnectionProvider.class);
		dialect.configure(connectionProvider);
		String catalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		String schema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		Iterator<Map<String,Object>> tables =
				dialect.getTables(
						catalog,
						schema,
						"TAB_MASTER");
		boolean foundMaster = false;
		while(tables.hasNext()) {
			Map<?,?> map = tables.next();
			String tableName = (String) map.get("TABLE_NAME");
			String schemaName = (String) map.get("TABLE_SCHEM");
			String catalogName = (String) map.get("TABLE_CAT");
			if(tableName.equals("TAB_MASTER")) {
				foundMaster = true;
				Iterator<?> exportedKeys =
						dialect.getExportedKeys(
								catalogName,
								schemaName,
								tableName );
				int cnt = 0;
				while ( exportedKeys.hasNext() ) {
					exportedKeys.next();
					cnt++;
				}
				assertEquals(1,cnt);
			}
		}
		assertTrue(foundMaster);
	}

	@Test
	public void testDataType() {
		RevengDialect dialect = RevengDialectFactory
				.fromDialectName(properties.getProperty(AvailableSettings.DIALECT));
		ConnectionProvider connectionProvider =
				serviceRegistry.getService(ConnectionProvider.class);
		assert dialect != null;
		dialect.configure(connectionProvider);
		String catalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		String schema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		Iterator<?> tables =
				dialect.getColumns(
						catalog,
						schema,
						"test",
						null );
		while(tables.hasNext()) {
			Map<?,?> map = (Map<?,?>) tables.next();
			System.out.println(map);
		}
	}

	@Test
	public void testCaseTest() {
		RevengDialect dialect = new JDBCMetaDataDialect();
		ConnectionProvider connectionProvider =
				serviceRegistry.getService(ConnectionProvider.class);
		dialect.configure(connectionProvider);
		String catalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		String schema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		Iterator<Map<String, Object>> tables =
				dialect.getTables(
						catalog,
						schema,
						"TAB_MASTER");

		JUnitUtil.assertIteratorContainsExactly(null, tables, 1);
	}

}
