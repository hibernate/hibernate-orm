/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


import org.hibernate.boot.mapping.internal.context.GlobalMappingDefaultsImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptionsImpl;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.Table;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Logical namespace keys survive naming and serialization independently of SQL names.
///
/// @author Steve Ebersole
@ServiceRegistry
class NamespaceLogicalNamesTest {
	@Test
	void namespaceKeysDistinguishQuotingAndIgnoreOrigin(ServiceRegistryScope scope) {
		final var database = new Database( new MappingResolutionOptionsImpl( scope.getRegistry() ) );
		final var plain = database.locateNamespace( name( "catalog", false, true ), name( "schema", false, true ) );
		assertSame( plain, database.locateNamespace( name( "CATALOG", false, false ), name( "SCHEMA", false, false ) ) );
		assertNotSame( plain, database.locateNamespace( name( "catalog", true, true ), name( "schema", false, true ) ) );
		assertNotSame( plain, database.locateNamespace( name( "catalog", false, true ), name( "schema", true, true ) ) );
		assertNull( database.findNamespace( name( "missing", false, true ), null ) );
		assertSame( database.getDefaultNamespace(), database.findNamespace( null, null ) );
	}

	@Test
	void tableAndSequenceKeysRemainLogical(ServiceRegistryScope scope) {
		final var options = new MappingResolutionOptionsImpl( scope.getRegistry() );
		final var strategy = new PrefixStrategy();
		options.applyPhysicalNamingStrategy( strategy );
		final var namespace = new Database( options ).getDefaultNamespace();
		final var plain = name( "record", false, true );
		final var quoted = name( "record", true, false );
		final var table = table( namespace, plain );
		final var quotedTable = table( namespace, quoted );
		assertNotSame( table, quotedTable );
		assertEquals( "p_record", table.getName() );
		assertTrue( quotedTable.isQuoted() );
		assertSame( table, namespace.locateTable( name( "RECORD", false, false ) ) );
		assertSame( quotedTable, namespace.locateTable( quoted ) );
		assertNull( namespace.locateTable( name( "p_record", false, true ) ) );
		assertNull( namespace.locateTable( name( "RECORD", true, true ) ) );
		assertSame( table, table( namespace, name( "RECORD", false, false ) ) );

		final var sequence = sequence( namespace, plain );
		final var quotedSequence = sequence( namespace, quoted );
		assertNotSame( sequence, quotedSequence );
		assertSame( sequence, namespace.locateSequence( name( "RECORD", false, false ) ) );
		assertSame( quotedSequence, namespace.locateSequence( quoted ) );
		assertNull( namespace.locateSequence( name( "p_record", false, true ) ) );
		assertThrows( org.hibernate.HibernateException.class, () -> sequence( namespace, name( "RECORD", false, false ) ) );
		assertEquals( 4, strategy.calls );
		assertSame( quoted, strategy.lastInput );
	}

	@Test
	void databaseRoundTripRetainsKeysWithoutRepeatingNaming(ServiceRegistryScope scope) {
		final var options = new MappingResolutionOptionsImpl( scope.getRegistry() );
		final var strategy = new PrefixStrategy();
		options.applyPhysicalNamingStrategy( strategy );
		((GlobalMappingDefaultsImpl) options.getMappingDefaults()).applyImplicitlyQuoteIdentifiers( true );
		final var database = new Database( options );
		final var schema = name( "schema", true, true );
		final var namespace = database.locateNamespace( null, schema );
		final var logical = name( "record", false, false );
		table( namespace, logical );
		sequence( namespace, logical );

		final var restored = (Database) SerializationHelper.clone( database );
		restored.reattach( options );
		final var restoredNamespace = restored.findNamespace( null, schema );
		assertNotNull( restoredNamespace );
		assertTrue( restoredNamespace.getName().schema().isExplicit() );
		assertEquals( schema, restoredNamespace.getName().schema() );
		assertEquals( "p_schema", restoredNamespace.getPhysicalName().schema().getText() );
		assertTrue( restoredNamespace.getPhysicalName().schema().isQuoted() );
		assertNull( restored.findNamespace( null, name( "p_schema", true, true ) ) );
		assertEquals( "p_record", restoredNamespace.locateTable( logical ).getName() );
		assertEquals( "p_record", restoredNamespace.locateSequence( logical ).getName().getObjectName().getText() );
		assertNull( restoredNamespace.locateTable( name( "p_record", false, false ) ) );
		assertEquals( 3, strategy.calls );
		assertSame( restoredNamespace.locateTable( logical ), table( restoredNamespace, logical ) );
		assertEquals( 3, strategy.calls );
		final var newTable = table( restoredNamespace, name( "new", false, true ) );
		assertEquals( "p_new", newTable.getName() );
		assertTrue( newTable.isQuoted() );
		assertEquals( 4, strategy.calls );
	}

	private static LogicalName name(String text, boolean quoted, boolean explicit) {
		return new LogicalName( text, quoted, explicit );
	}

	private static Table table(Namespace namespace, LogicalName name) {
		return namespace.createTable( name, physical -> new org.hibernate.mapping.PhysicalTable( "orm", namespace, physical, false ) );
	}

	private static Sequence sequence(Namespace namespace, LogicalName name) {
		return namespace.createSequence( name, physical -> new Sequence(
				"orm", namespace.getPhysicalName().catalog(), namespace.getPhysicalName().schema(), physical ) );
	}

	private static class PrefixStrategy extends PhysicalNamingStrategyStandardImpl {
		private int calls;
		private LogicalName lastInput;

		@Override
		@Nullable
		public PhysicalName toPhysicalSchemaName(@Nullable LogicalName name, @Nonnull PhysicalNamingContext context) {
			return name == null ? null : prefixed( name, context );
		}

		@Override
		@Nonnull
		public PhysicalName toPhysicalTableName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return prefixed( name, context );
		}

		@Override
		@Nonnull
		public PhysicalName toPhysicalSequenceName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			return prefixed( name, context );
		}

		private PhysicalName prefixed(LogicalName name, PhysicalNamingContext context) {
			calls++;
			lastInput = name;
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}
}
