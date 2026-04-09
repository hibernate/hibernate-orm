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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengDialectFactory;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategy.SchemaSelection;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.internal.reveng.reader.DatabaseReader;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.internal.reveng.strategy.OverrideRepository;
import org.hibernate.tools.test.util.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
	
	// TODO Reenable this test (HBX-1401) 
	@Disabled
	@Test
	public void testReadOnlySpecificSchema() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection(null, "cat.cat", null));
		RevengStrategy res = or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata();
		List<Table> tables = getTables(metadata);
		assertEquals(2,tables.size());
		Table catchild = (Table) tables.get(0);
		Table catmaster = (Table) tables.get(1);
		if(catchild.getName().equals("cat.master")) {
			catchild = (Table) tables.get(1);
			catmaster = (Table) tables.get(0);
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
		RevengDialect realMetaData = RevengDialectFactory.createMetaDataDialect( serviceRegistry.getService(JdbcServices.class).getDialect(), properties);
		assertTrue(realMetaData.needQuote("cat.cat"), "The name must be quoted!");
		assertTrue(realMetaData.needQuote("cat.child"), "The name must be quoted!");
		assertTrue(realMetaData.needQuote("cat.master"), "The name must be quoted!");
	}
	
	/**
	 * There are 2 solutions:
	 * 1. DatabaseCollector#addTable()/getTable() should be called for not quoted parameters - I think it is preferable way.
	 * 2. DatabaseCollector#addTable()/getTable() should be called for quoted parameters - here users should
	 * use the same quotes as JDBCReader.
	 * Because of this there are 2 opposite methods(and they are both failed as addTable uses quoted names
	 * but getTable uses non-quoted names )
	 */
	// TODO Reenable this test (HBX-1401) 
	@Disabled
	@Test
	public void testQuotedNamesAndDefaultDatabaseCollector() {
		Properties properties = Environment.getProperties();
		properties.put(AvailableSettings.DEFAULT_SCHEMA, "cat.cat");
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = builder.build();	
		RevengDialect realMetaData = RevengDialectFactory.createMetaDataDialect( 
				serviceRegistry.getService(JdbcServices.class).getDialect(), 
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
		if (name == null)
			return name;
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
		List<Table> list = new ArrayList<Table>();
		Iterator<Table> iter = metadata.collectTableMappings().iterator();
		while(iter.hasNext()) {
			Table element = iter.next();
			list.add(element);
		}
		return list;
	}
	
	private SchemaSelection createSchemaSelection(String matchCatalog, String matchSchema, String matchTable) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return matchCatalog;
			}
			@Override
			public String getMatchSchema() {
				return matchSchema;
			}
			@Override
			public String getMatchTable() {
				return matchTable;
			}		
		};
	}
}
