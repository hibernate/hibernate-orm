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
package org.hibernate.tool.cfg.DriverMetaData;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengDialectFactory;
import org.hibernate.tool.internal.reveng.dialect.JDBCMetaDataDialect;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

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
