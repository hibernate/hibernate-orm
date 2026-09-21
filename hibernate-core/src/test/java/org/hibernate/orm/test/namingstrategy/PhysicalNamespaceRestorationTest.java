/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.relational.naming.spi.IdentifierComparisonPolicy;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Physical namespace archives preserve spelling and use the restored system's policy.
///
/// @author Steve Ebersole
class PhysicalNamespaceRestorationTest {
	@Test
	void restoresWithNewPolicyWithoutRepeatingNamingOrQuoting() {
		final var sourceEnvironment = environment( "original:", false );
		final var registry = registry( sourceEnvironment );
		final var options = mock( MappingResolutionOptions.class );
		final var strategy = new CountingStrategy();
		when( options.getServiceRegistry() ).thenReturn( registry );
		when( options.getMappingDefaults() ).thenReturn( mock( GlobalMappingDefaults.class ) );
		when( options.getPhysicalNamingStrategy() ).thenReturn( strategy );
		final var database = new Database( options );
		final var catalog = new LogicalName( "catalog", false, true );
		final var schema = new LogicalName( "Mixed", true, true );
		final var namespace = database.locateNamespace( catalog, schema );
		final var sourceName = namespace.getPhysicalName();
		final var logicalSequence = new LogicalName( "Seq", true, true );
		namespace.createSequence( logicalSequence, physical -> new Sequence(
				"orm", sourceName.catalog(), sourceName.schema(), physical ) );
		final var logicalTable = new LogicalName( "Records", false, true );
		final var sourceTable = (org.hibernate.mapping.PhysicalTable) namespace.createTable( logicalTable,
				physical -> new org.hibernate.mapping.PhysicalTable( "orm", namespace, physical, false ) );
		final var logicalView = new LogicalName( "Report", true, true );
		namespace.createTable( logicalView,
				physical -> new org.hibernate.mapping.DatabaseView( "orm", namespace, physical, "select id from p_Records" ) );
		final var sourceColumn = new org.hibernate.mapping.Column(
				sourceEnvironment.getIdentifierHelper().getPhysicalNameFactory().create( "p_code", false ) );
		sourceTable.addColumn( sourceColumn );
		final int calls = strategy.calls;

		// The policy and the strategy's runtime state must not enter the archive.
		final var restored = (Database) SerializationHelper.clone( database );
		assertThrows( IllegalStateException.class, restored::getPhysicalImplicitNamespaceName );
		assertThrows( IllegalStateException.class, () -> restored.findNamespace( catalog, schema ).getPhysicalName() );
		assertThrows( IllegalStateException.class,
				() -> ((org.hibernate.mapping.NamedTable) restored.findNamespace( catalog, schema ).locateTable( logicalTable )).getPhysicalName() );
		final var targetEnvironment = environment( "restored:", true );
		when( registry.getService( JdbcEnvironment.class ) ).thenReturn( targetEnvironment );
		strategy.forbidNaming = true;
		restored.reattach( options );

		final var restoredName = restored.findNamespace( catalog, schema ).getPhysicalName();
		final var factory = targetEnvironment.getIdentifierHelper().getPhysicalNameFactory();
		assertEquals( "p_catalog", restoredName.catalog().getText() );
		assertFalse( restoredName.catalog().isQuoted(), "Restoration must not apply new global quoting" );
		assertEquals( "p_Mixed", restoredName.schema().getText() );
		assertTrue( restoredName.schema().isQuoted() );
		assertEquals( factory.create( "p_catalog", false ), restoredName.catalog() );
		assertEquals( factory.create( "p_Mixed", true ), restoredName.schema() );
		assertNotEquals( sourceName.catalog(), restoredName.catalog(), "Cached source keys must not survive restoration" );
		assertEquals( factory.create( "fallback_catalog", false ), restored.getPhysicalImplicitNamespaceName().catalog() );
		assertEquals( factory.create( "fallback_schema", false ), restored.getPhysicalImplicitNamespaceName().schema() );
		final var restoredSequence = restored.findNamespace( catalog, schema ).locateSequence( logicalSequence );
		assertEquals( factory.create( "p_Seq", true ), restoredSequence.getName().objectName() );
		assertEquals( restoredName.catalog(), restoredSequence.getName().catalogName() );
		assertEquals( restoredName.schema(), restoredSequence.getName().schemaName() );
		assertEquals( calls, strategy.calls );

		final var restoredTable = (org.hibernate.mapping.PhysicalTable) restored.findNamespace( catalog, schema ).locateTable( logicalTable );
		assertEquals( factory.create( "p_Records", false ), restoredTable.getPhysicalName().objectName() );
		assertFalse( restoredTable.getPhysicalName().objectName().isQuoted() );
		final var restoredColumn = restoredTable.getColumn( factory.create( "p_code", false ) );
		assertNotNull( restoredColumn );
		assertFalse( restoredColumn.isQuoted() );
		assertNotEquals( sourceColumn.getPhysicalName(), restoredColumn.getPhysicalName() );
		assertNotEquals( sourceTable, restoredTable );
		final var equivalent = new org.hibernate.mapping.PhysicalTable( "orm", restoredTable.getPhysicalName(), false );
		assertEquals( equivalent, restoredTable );
		assertEquals( equivalent.hashCode(), restoredTable.hashCode() );
		final var denormalized = new org.hibernate.mapping.DenormalizedTable( "orm", restoredTable.getPhysicalName(), false, equivalent );
		assertEquals( restoredTable, denormalized );
		assertEquals( restoredTable.hashCode(), denormalized.hashCode() );
		assertTrue( java.util.Set.of( restoredTable ).contains( equivalent ) );
		final var restoredView = (org.hibernate.mapping.DatabaseView) restored.findNamespace( catalog, schema ).locateTable( logicalView );
		assertEquals( "select id from p_Records", restoredView.getViewQuery() );
		assertTrue( restoredView.getPhysicalName().objectName().isQuoted() );
		assertNotEquals( restoredView, new org.hibernate.mapping.PhysicalTable( "orm", restoredView.getPhysicalName(), false ) );

		// A second round trip must preserve the same data-only serial form.
		final var restoredAgain = (Database) SerializationHelper.clone( restored );
		restoredAgain.reattach( options );
		assertEquals( restoredTable, restoredAgain.findNamespace( catalog, schema ).locateTable( logicalTable ) );
		assertEquals( restoredName, restoredAgain.findNamespace( catalog, schema ).getPhysicalName() );
		assertEquals( restoredSequence.getName(), restoredAgain.findNamespace( catalog, schema ).locateSequence( logicalSequence ).getName() );
		assertEquals( calls, strategy.calls );
	}

	@Test
	void preservesNullQualifiers() {
		final var environment = environment( "system:", false );
		final var options = mock( MappingResolutionOptions.class );
		final var registry = registry( environment );
		when( options.getServiceRegistry() ).thenReturn( registry );
		when( options.getMappingDefaults() ).thenReturn( mock( GlobalMappingDefaults.class ) );
		when( options.getPhysicalNamingStrategy() ).thenReturn( PhysicalNamingStrategyStandardImpl.INSTANCE );
		final var database = new Database( options );
		database.getDefaultNamespace();
		final var restored = (Database) SerializationHelper.clone( database );
		restored.reattach( options );
		assertNull( restored.getPhysicalImplicitNamespaceName().catalog() );
		assertNull( restored.getPhysicalImplicitNamespaceName().schema() );
		assertNull( restored.getDefaultNamespace().getPhysicalName().catalog() );
		assertNull( restored.getDefaultNamespace().getPhysicalName().schema() );
	}

	static StandardServiceRegistry registry(JdbcEnvironment environment) {
		final var registry = mock( StandardServiceRegistry.class );
		final var jdbcServices = mock( JdbcServices.class );
		when( registry.requireService( JdbcServices.class ) ).thenReturn( jdbcServices );
		when( jdbcServices.getDialect() ).thenReturn( new H2Dialect() );
		when( registry.getService( JdbcEnvironment.class ) ).thenReturn( environment );
		return registry;
	}

	static JdbcEnvironment environment(String keyPrefix, boolean globallyQuoted) {
		final var environment = mock( JdbcEnvironment.class );
		final var builder = IdentifierHelperBuilder.from( environment );
		builder.setGloballyQuoteIdentifiers( globallyQuoted );
		builder.setComparisonPolicy( new IdentifierComparisonPolicy() {
			@Override
			public String toDatabaseName(String text, boolean quoted) {
				return text;
			}

			@Override
			public String comparisonKey(String text, boolean quoted) {
				return keyPrefix + quoted + ':' + text;
			}
		} );
		when( environment.getIdentifierHelper() ).thenReturn( builder.build() );
		return environment;
	}

	private static class CountingStrategy extends PhysicalNamingStrategyStandardImpl {
		private final Object runtimeState = new Object();
		int calls;
		boolean forbidNaming;

		@Override
		public PhysicalName toPhysicalCatalogName(LogicalName name, PhysicalNamingContext context) {
			return resolve( name, context, "fallback_catalog" );
		}

		@Override
		public PhysicalName toPhysicalSchemaName(LogicalName name, PhysicalNamingContext context) {
			return resolve( name, context, "fallback_schema" );
		}

		@Override
		public PhysicalName toPhysicalTableName(LogicalName name, PhysicalNamingContext context) {
			return resolve( name, context, "unused" );
		}

		@Override
		public PhysicalName toPhysicalSequenceName(LogicalName name, PhysicalNamingContext context) {
			return resolve( name, context, "unused" );
		}

		private PhysicalName resolve(LogicalName name, PhysicalNamingContext context, String fallback) {
			assertFalse( forbidNaming, "Restoration must not invoke the naming strategy" );
			calls++;
			return context.getPhysicalNameFactory().create( name == null ? fallback : "p_" + name.getText(), name != null && name.isQuoted() );
		}
	}
}
