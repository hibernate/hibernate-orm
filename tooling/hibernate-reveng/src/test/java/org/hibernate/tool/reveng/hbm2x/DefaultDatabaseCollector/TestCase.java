/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.DefaultDatabaseCollector;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengDialectFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.reader.DatabaseReader;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.OverrideRepository;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		RevengDialect realMetaData;
		try (ServiceRegistry serviceRegistry = ssrb.build()) {
			realMetaData = RevengDialectFactory.createMetaDataDialect(
					Objects.requireNonNull( serviceRegistry.getService( JdbcServices.class ) ).getDialect(),
					properties );
		}
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
