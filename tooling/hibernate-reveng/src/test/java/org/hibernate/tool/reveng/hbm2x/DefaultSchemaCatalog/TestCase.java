/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.DefaultSchemaCatalog;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.mapping.NamedTable;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Table;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.OverrideRepository;
import org.hibernate.tool.reveng.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author max
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
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		RevengStrategy res = or.getReverseEngineeringStrategy(new DefaultStrategy());
		List<Table> tables = getTables(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata());
		assertEquals(2,tables.size());
		Table catchild = tables.get(0);
		Table catmaster = tables.get(1);
		if(catchild.getName().equals("CATMASTER")) {
			catchild = tables.get(1);
			catmaster = tables.get(0);
		}
		TableIdentifier masterid = TableIdentifier.create(catmaster);
		TableIdentifier childid = TableIdentifier.create(catchild);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATMASTER"), masterid);
		assertEquals(TableIdentifier.create(null, "OVRTEST", "CATCHILD"), childid);
	}

	@Test
	public void testOverlapping() {
		OverrideRepository or = new OverrideRepository();
		or.addSchemaSelection(createSchemaSelection("OVRTEST", null));
		or.addSchemaSelection(createSchemaSelection(null, "MASTER"));
		or.addSchemaSelection(createSchemaSelection(null, "CHILD"));
		RevengStrategy res =
				or.getReverseEngineeringStrategy(new DefaultStrategy());
		Metadata metadata = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(res, null)
				.createMetadata();
		Set<TableIdentifier> tables = new HashSet<>();
		for (Table element : metadata.collectTableMappings()) {
			boolean added = tables.add(TableIdentifier.create(element));
			if (!added)
				fail("duplicate table found for " + element);
		}
		assertEquals(4,tables.size());
	}

	@Test
	public void testUseDefault() {
		Properties properties = new Properties();
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		properties.setProperty(Environment.DEFAULT_SCHEMA, "OVRTEST");
		List<Table> tables = getTables(MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, properties)
				.createMetadata());
		assertEquals(2,tables.size());
		Table catchild = tables.get(0);
		Table catmaster = tables.get(1);
		if(catchild.getName().equals("CATMASTER")) {
			catchild = tables.get(1);
			catmaster = tables.get(0);
		}
		TableIdentifier masterid = TableIdentifier.create(catmaster);
		TableIdentifier childid = TableIdentifier.create(catchild);
		assertEquals(TableIdentifier.create(null, null, "CATMASTER"), masterid, "jdbcreader has not nulled out according to default schema");
		assertEquals(TableIdentifier.create(null, null, "CATCHILD"), childid, "jdbcreader has not nulled out according to default schema");
	}

	@Test
	public void testDatabaseNamesAreAlreadyPhysical() {
		final var properties = new Properties();
		properties.setProperty( Environment.DEFAULT_SCHEMA, "OVRTEST" );
		properties.setProperty( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
		properties.put( Environment.PHYSICAL_NAMING_STRATEGY, new AlreadyPhysicalStrategy() );
		final var metadata = MetadataDescriptorFactory.createReverseEngineeringDescriptor( null, properties ).createMetadata();
		final var tables = getTables( metadata );
		assertEquals( 2, tables.size() );
		assertEquals( "OVRTEST", metadata.getDatabase().getPhysicalImplicitNamespaceName().schema().getText() );
		org.junit.jupiter.api.Assertions.assertFalse( metadata.getDatabase().getPhysicalImplicitNamespaceName().schema().isQuoted() );
		final var sqlContext = org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.fromExplicit(
				metadata.getDatabase().getJdbcEnvironment(), metadata.getDatabase(), null, null );
		for ( var table : tables ) {
			final var physical = ((NamedTable) table).getPhysicalName();
			org.junit.jupiter.api.Assertions.assertNull( physical.schemaName() );
			org.junit.jupiter.api.Assertions.assertFalse( physical.objectName().isQuoted() );
			org.junit.jupiter.api.Assertions.assertTrue( Set.of( "CATMASTER", "CATCHILD" ).contains( physical.objectName().getText() ) );
			org.junit.jupiter.api.Assertions.assertTrue( table.getTableExpression( sqlContext ).endsWith( "OVRTEST." + table.getName() ) );
			org.junit.jupiter.api.Assertions.assertThrows( NoSuchMethodException.class,
					() -> NamedTable.class.getMethod( "setSchema", String.class ) );
		}
	}

	public static class AlreadyPhysicalStrategy extends PhysicalNamingStrategyStandardImpl {
		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(
				@Nonnull LogicalName name,
				@Nonnull PhysicalNamingContext context) {
			throw new AssertionError( "A JDBC table name must not pass through physical naming" );
		}

		@Override
		@Nullable
		public PhysicalName toPhysicalSchemaName(
				@Nullable LogicalName name,
				@Nonnull PhysicalNamingContext context) {
			return name == null ? null : context.getPhysicalNameFactory().create( "renamed_" + name.getText(), false );
		}
	}

	private List<Table> getTables(Metadata metadata) {
		return new ArrayList<>(metadata.collectTableMappings());
	}

	private SchemaSelection createSchemaSelection(String matchSchema, String matchTable) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return null;
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
