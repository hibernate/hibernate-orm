/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.IncrementalSchemaReading;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengDialectFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.reader.DatabaseReader;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.TableSelectorStrategy;
import org.hibernate.tool.reveng.test.utils.JUnitUtil;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	private Properties properties = null;
	private String defaultSchema = null;
	private String defaultCatalog = null;
	private final List<String> gottenTables = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
		properties = Environment.getProperties();
		defaultSchema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		defaultCatalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testReadSchemaIncremental() {
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		builder.applySettings(properties);
		ServiceRegistry serviceRegistry = builder.build();
		Dialect dialect = Objects.requireNonNull(serviceRegistry.getService(JdbcServices.class)).getDialect();
		TableSelectorStrategy tss = new TableSelectorStrategy(new DefaultStrategy());
		RevengDialect mockedMetaDataDialect = createMockedMetaDataDialect(
				RevengDialectFactory.createMetaDataDialect(dialect, properties));
		DatabaseReader reader = DatabaseReader.create( properties, tss, mockedMetaDataDialect, serviceRegistry);

		tss.addSchemaSelection( createSchemaSelection("CHILD") );

		RevengMetadataCollector dc = new RevengMetadataCollector();
		reader.readDatabaseSchema(dc);

		assertEquals(1, gottenTables.size());
		assertEquals("CHILD", gottenTables.get(0));

		Iterator<Table> iterator = dc.iterateTables();
		Table firstChild = iterator.next();
		assertEquals("CHILD", firstChild.getName());
		assertFalse(iterator.hasNext());

		assertFalse(firstChild.getForeignKeyCollection().iterator().hasNext(), "should not record foreignkey to table it doesn't know about yet");

		tss.clearSchemaSelections();
		tss.addSchemaSelection( createSchemaSelection("MASTER") );

		gottenTables.clear();
		reader.readDatabaseSchema(dc);

		assertEquals(1, gottenTables.size());
		assertEquals("MASTER", gottenTables.get(0));


		iterator = dc.iterateTables();
		assertNotNull(iterator.next());
		assertNotNull(iterator.next());
		assertFalse(iterator.hasNext());

		Table table = getTable(dc, mockedMetaDataDialect, defaultCatalog, defaultSchema, "CHILD" );
		assertSame( firstChild, table );

		JUnitUtil.assertIteratorContainsExactly(
				"should have recorded one foreignkey to child table",
				firstChild.getForeignKeyCollection().iterator(),
				1);


		tss.clearSchemaSelections();
		reader.readDatabaseSchema(dc);

		Table finalMaster = getTable(dc, mockedMetaDataDialect, defaultCatalog, defaultSchema, "MASTER" );

		assertSame(firstChild, getTable(dc, mockedMetaDataDialect, defaultCatalog, defaultSchema, "CHILD" ));
		JUnitUtil.assertIteratorContainsExactly(
				null,
				firstChild.getForeignKeyCollection().iterator(),
				1);
		JUnitUtil.assertIteratorContainsExactly(
				null,
				finalMaster.getForeignKeyCollection().iterator(),
				0);
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

	private SchemaSelection createSchemaSelection(String matchTable) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
			}
			@Override
			public String getMatchSchema() {
				return null;
			}
			@Override
			public String getMatchTable() {
				return matchTable;
			}
		};
	}

	private RevengDialect createMockedMetaDataDialect(RevengDialect delegate) {
		return (RevengDialect)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { RevengDialect.class },
				(proxy, method, args) -> {
					if ("getTables".equals(method.getName())) {
						gottenTables.add((String)args[2]);
						return delegate.getTables(
								(String)args[0],
								(String)args[1],
								args[2] == null ? "%" : (String)args[2]);
					} else if ("getColumns".equals(method.getName())) {
						return delegate.getColumns(
								(String)args[0],
								(String)args[1],
								args[2] == null ? "%" : (String)args[2],
								args[3] == null ? "%" : (String)args[3]);
					} else {
						return method.invoke(delegate, args);

					}
				});
	}

}
