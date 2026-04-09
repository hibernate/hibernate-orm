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
package org.hibernate.tool.hbm2x.DefaultDatabaseCollector;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengDialectFactory;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.internal.reveng.reader.DatabaseReader;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
public class TestCase {
		
	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}
	
	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}
	
	@Test
	public void testReadOnlySpecificSchema() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection());
		RevengStrategy res = or.getReverseEngineeringStrategy(new DefaultStrategy());
		List<Table> tables = getTables(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata());
		assertEquals(2,tables.size());
		Table catchild = tables.get(0);
		Table catmaster = tables.get(1);
		if(catchild.getName().equals("cat.master")) {
			catchild = tables.get(1);
			catmaster = tables.get(0);
		} 
		TableIdentifier masterid = TableIdentifier.create(catmaster);
		TableIdentifier childid = TableIdentifier.create(catchild);
		assertEquals(TableIdentifier.create(null, "cat.cat", "cat.child"), childid);
		assertEquals(TableIdentifier.create(null, "cat.cat", "cat.master"), masterid);
	}
	
	@Test
	public void testNeedQuote() {
		Properties properties = Environment.getProperties();
		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySettings(properties);
		ServiceRegistry serviceRegistry = ssrb.build();
		RevengDialect realMetaData = RevengDialectFactory.createMetaDataDialect(
				Objects.requireNonNull(serviceRegistry.getService(JdbcServices.class)).getDialect(),
				properties );
		assertTrue(realMetaData.needQuote("cat.cat"), "The name must be quoted!");
		assertTrue(realMetaData.needQuote("cat.child"), "The name must be quoted!");
		assertTrue(realMetaData.needQuote("cat.master"), "The name must be quoted!");
	}
	
	/**
	 * There are 2 solutions:
	 * 1. DatabaseCollector#addTable()/getTable() should be called for not quoted parameters - I think it is a preferable way.
	 * 2. DatabaseCollector#addTable()/getTable() should be called for quoted parameters - here users should
	 * use the same quotes as JDBCReader.
	 * Because of this there are 2 opposite methods(and they are both failed as addTable uses quoted names
	 * but getTable uses non-quoted names )
	 */
	@Test
	public void testQuotedNamesAndDefaultDatabaseCollector() {
		Properties properties = Environment.getProperties();
		properties.put(AvailableSettings.DEFAULT_SCHEMA, "cat.cat");
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = builder.build();	
		RevengDialect realMetaData = RevengDialectFactory.createMetaDataDialect( 
				Objects.requireNonNull(serviceRegistry.getService(JdbcServices.class)).getDialect(),
				properties);
		DatabaseReader reader = DatabaseReader.create( 
				properties, new DefaultStrategy(), 
				realMetaData, serviceRegistry );
		RevengMetadataCollector dc = new RevengMetadataCollector();
		reader.readDatabaseSchema(dc);
		String defaultCatalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		assertNotNull(getTable(dc, realMetaData, defaultCatalog, "cat.cat", "cat.child"), "The table should be found");
		assertNotNull(getTable(dc, realMetaData, defaultCatalog, "cat.cat", "cat.master"), "The table should be found");
		assertNull(getTable(dc, realMetaData, defaultCatalog, doubleQuote("cat.cat"), doubleQuote("cat.child")), "Quoted names should not return the table");
		assertNull(getTable(dc, realMetaData, defaultCatalog, doubleQuote("cat.cat"), doubleQuote("cat.master")), "Quoted names should not return the table");
		assertEquals(1, dc.getOneToManyCandidates().size(), "Foreign key 'masterref' was filtered!");
	}
	
	private Table getTable(
			RevengMetadataCollector revengMetadataCollector, 
			RevengDialect metaDataDialect, 
			String catalog, 
			String schema, 
			String name) {
		return revengMetadataCollector.getTable(
				TableIdentifier.create(
						quote(metaDataDialect, catalog), 
						quote(metaDataDialect, schema), 
						quote(metaDataDialect, name)));
	}
 	
	private String quote(RevengDialect metaDataDialect, String name) {
		if (name == null) return null;
		if (metaDataDialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name; // avoid double quoting
			}
			return "`" + name + "`";
		} else {
			return name;
		}
	}
	
	private static String doubleQuote(String name) {
		return "\"" + name + "\"";
	}
	
	private List<Table> getTables(Metadata metadata) {
        return new ArrayList<>(metadata.collectTableMappings());
	}
	
	private SchemaSelection createSchemaSelection() {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
			}
			@Override
			public String getMatchSchema() {
				return "cat.cat";
			}
			@Override
			public String getMatchTable() {
				return null;
			}		
		};
	}
	
}
