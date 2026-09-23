/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.tool.reveng.api.core.ForeignKeyDefinition;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Source parsing works without a service registry and preserves legacy coalescing rules.
///
/// @author Steve Ebersole
class OverrideSourceTest {
	@Test
	void retainsQuotingColumnOrderAndQualifierOverrides() {
		final var strategy = parse( """
				<table catalog="cat" schema="s" name="child">
				<foreign-key constraint-name="fk_pair" foreign-table="`Parent`" foreign-schema="other">
					<column-ref local-column="`first`" foreign-column="`Code`"/>
					<column-ref local-column="second" foreign-column="id"/>
				</foreign-key>
				</table>
				""" );
		final var key = strategy.getForeignKeys( TableIdentifier.create( "cat", "other", "Parent" ) ).get( 0 );
		assertEquals( "`Parent`", key.referencedTable().getName() );
		assertEquals( "child", key.table().getName() );
		assertEquals( List.of( new ForeignKeyDefinition.ColumnReference( "`first`", "`Code`" ),
				new ForeignKeyDefinition.ColumnReference( "second", "id" ) ), key.columns() );
		assertThrows( UnsupportedOperationException.class, () -> key.columns().clear() );
	}

	@Test
	void equivalentDeclarationsKeepFirstColumnsAndLastExplicitName() {
		final var strategy = parse( """
				<table name="child">
				<column name="parent_id" foreign-table="parent" foreign-column="id"/>
				<foreign-key constraint-name="first_name" foreign-table="parent">
					<column-ref local-column="PARENT_ID" foreign-column="ID"/>
				</foreign-key>
				<foreign-key constraint-name="last_name" foreign-table="parent">
					<column-ref local-column="parent_id" foreign-column="id"/>
				</foreign-key>
				</table>
				""" );
		final var keys = strategy.getForeignKeys( TableIdentifier.create( null, null, "parent" ) );
		assertEquals( 1, keys.size() );
		assertEquals( "last_name", keys.get( 0 ).name() );
		assertEquals( "parent_id", keys.get( 0 ).columns().get( 0 ).column() );
	}

	@Test
	void retainsDistinctUnnamedDefinitionsAndDuplicateColumnDiagnostic() {
		final var strategy = parse( """
				<table name="child">
				<column name="a" foreign-table="parent" foreign-column="id"/>
				<column name="b" foreign-table="parent" foreign-column="id"/>
				</table>
				""" );
		final var keys = strategy.getForeignKeys( TableIdentifier.create( null, null, "parent" ) );
		assertEquals( 2, keys.size() );
		assertNull( keys.get( 0 ).name() );
		assertNull( keys.get( 1 ).name() );
		assertThrows( MappingException.class, () -> parse( """
				<table name="child"><primary-key><key-column name="id"/></primary-key><column name="ID"/></table>
				""" ) );
	}

	@Test
	void retainsLegacyMixedQuotingCoalescingDirection() {
		final var quotedFirst = parse( """
				<table name="child">
				<column name="`id`" foreign-table="parent" foreign-column="id"/>
				<foreign-key constraint-name="fk" foreign-table="parent">
					<column-ref local-column="ID" foreign-column="id"/>
				</foreign-key></table>
				""" );
		assertEquals( 2, quotedFirst.getForeignKeys( TableIdentifier.create( null, null, "parent" ) ).size() );
		final var unquotedFirst = parse( """
				<table name="child">
				<column name="ID" foreign-table="parent" foreign-column="id"/>
				<foreign-key constraint-name="fk" foreign-table="parent">
					<column-ref local-column="`id`" foreign-column="id"/>
				</foreign-key></table>
				""" );
		final var keys = unquotedFirst.getForeignKeys( TableIdentifier.create( null, null, "parent" ) );
		assertEquals( 1, keys.size() );
		assertEquals( "ID", keys.get( 0 ).columns().get( 0 ).column() );
	}

	@Test
	void retainsLegacyMixedQuotingDuplicateColumnLookup() {
		assertDoesNotThrow( () -> parse( """
				<table name="child"><column name="ID"/><column name="`id`"/></table>
				""" ) );
		assertThrows( MappingException.class, () -> parse( """
				<table name="child"><column name="`id`"/><column name="ID"/></table>
				""" ) );
	}

	@Test
	void definitionsDefensivelyCopyAndRequirePairedNames() {
		final var columns = new ArrayList<ForeignKeyDefinition.ColumnReference>();
		final var table = TableIdentifier.create( null, null, "table" );
		final var key = new ForeignKeyDefinition( null, table, table, columns );
		columns.add( new ForeignKeyDefinition.ColumnReference( "id", "id" ) );
		assertTrue( key.columns().isEmpty() );
		assertThrows( IllegalArgumentException.class, () -> new ForeignKeyDefinition.ColumnReference( "", "id" ) );
	}

	private org.hibernate.tool.reveng.api.core.RevengStrategy parse(String tables) {
		final var repository = new OverrideRepository();
		repository.addInputStream( new ByteArrayInputStream(
				("<hibernate-reverse-engineering>" + tables + "</hibernate-reverse-engineering>").getBytes( StandardCharsets.UTF_8 ) ) );
		return repository.getReverseEngineeringStrategy( new DefaultStrategy() );
	}
}
