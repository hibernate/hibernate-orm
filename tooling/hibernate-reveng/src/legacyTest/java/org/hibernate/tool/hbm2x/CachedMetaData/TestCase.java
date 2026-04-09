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
package org.hibernate.tool.hbm2x.CachedMetaData;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengDialectFactory;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.internal.reveng.dialect.CachedMetaDataDialect;
import org.hibernate.tool.internal.reveng.reader.DatabaseReader;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JUnitUtil;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author max
 * @author koen
 */
public class TestCase {

	public static class MockedMetaDataDialect implements RevengDialect {

		RevengDialect delegate;
		private boolean failOnDelegateAccess;

		public MockedMetaDataDialect(RevengDialect realMetaData) {
			delegate = realMetaData;
		}

		public void close() {
			delegate.close();
		}

		public void close(Iterator<?> iterator) {
			delegate.close( iterator );
		}

		public void configure(ConnectionProvider cp) {
			delegate.configure(cp);			
		}
		
		public Iterator<Map<String, Object>> getColumns(String catalog, String schema, String table, String column) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getColumns( catalog, schema, table, column );
			}
		}

		public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getExportedKeys( catalog, schema, table );
			}
		}

		public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema, String table) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getIndexInfo( catalog, schema, table );
			}
		}

		public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema, String name) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getPrimaryKeys( catalog, schema, name );
			}
		}

		public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getTables( catalog, schema, table );
			}
		}

		public boolean needQuote(String name) {
			return delegate.needQuote( name );
		}

		public void setDelegate(Object object) {
			this.delegate = null;			
		}

		public void setFailOnDelegateAccess(boolean b) {
			failOnDelegateAccess = b;			
		}

		public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String name) {
			if(failOnDelegateAccess) {
				throw new IllegalStateException("delegate not accessible");
			} else {
				return delegate.getSuggestedPrimaryKeyStrategyName(catalog, schema, name);
			}
		}
		
	}
	
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testCachedDialect() {
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = builder.build();		
		Properties properties = Environment.getProperties();
		RevengDialect realMetaData = RevengDialectFactory.createMetaDataDialect( 
				Objects.requireNonNull(serviceRegistry.getService(JdbcServices.class)).getDialect(),
				Environment.getProperties() );
		MockedMetaDataDialect mock = new MockedMetaDataDialect(realMetaData);
		CachedMetaDataDialect dialect = new CachedMetaDataDialect(mock);
		DatabaseReader reader = DatabaseReader.create( 
				properties, 
				new DefaultStrategy(), 
				dialect, 
				serviceRegistry );
		RevengMetadataCollector dc = new RevengMetadataCollector();
		reader.readDatabaseSchema(dc);
		validate( dc );				
		mock.setFailOnDelegateAccess(true);	
		reader = DatabaseReader.create( 
				properties, 
				new DefaultStrategy(), 
				dialect, 
				serviceRegistry );
		dc = new RevengMetadataCollector();
		reader.readDatabaseSchema(dc);
		validate(dc);
 	}

	private void validate(RevengMetadataCollector dc) {
		Iterator<Table> iterator = dc.iterateTables();
		Table table = iterator.next();
		Table master = null, child = null;
		if ("MASTER".equals(table.getName())) {
			master = table;
			child = iterator.next();
		} else if ("CHILD".equals(table.getName())) {
			child = table;
			master = iterator.next();
		} else {
			fail("Only tables named 'MASTER' and 'CHILD' should exist");
		}
		assertNotNull(child);
		assertNotNull(master);
		
		iterator = dc.iterateTables();
		assertNotNull(iterator.next());
		assertNotNull(iterator.next());
		assertFalse(iterator.hasNext());		
		
		JUnitUtil.assertIteratorContainsExactly(
				"should have recorded one foreignkey to child table",  
				child.getForeignKeyCollection().iterator(),
				1);
	}

}
