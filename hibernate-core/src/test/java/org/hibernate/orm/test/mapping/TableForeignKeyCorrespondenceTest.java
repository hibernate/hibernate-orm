/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ForeignKeyColumnMapping;
import org.hibernate.mapping.ForeignKeyColumnMappings;
import org.hibernate.mapping.Table;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TableForeignKeyCorrespondenceTest {
	@Test
	void reusesForeignKeyForSameOrderedCorrespondence() {
		final Table table = new Table( "orm", "child" );
		final Column childOne = column( "child_one" );
		final Column childTwo = column( "child_two" );
		final Column parentOne = column( "parent_one" );
		final Column parentTwo = column( "parent_two" );

		final ForeignKey first = table.createForeignKey(
				null,
				new ForeignKeyColumnMappings( List.of(
						new ForeignKeyColumnMapping( childOne, parentOne ),
						new ForeignKeyColumnMapping( childTwo, parentTwo )
				) ),
				"Parent",
				null,
				null
		);
		final ForeignKey second = table.createForeignKey(
				null,
				new ForeignKeyColumnMappings( List.of(
						new ForeignKeyColumnMapping( childOne, parentOne ),
						new ForeignKeyColumnMapping( childTwo, parentTwo )
				) ),
				"Parent",
				null,
				null
		);

		assertSame( first, second );
		assertEquals( 1, table.getForeignKeyCollection().size() );
	}

	@Test
	void keepsDifferentReferencedCorrespondenceDistinct() {
		final Table table = new Table( "orm", "child" );
		final Column childOne = column( "child_one" );
		final Column childTwo = column( "child_two" );
		final Column parentOne = column( "parent_one" );
		final Column parentTwo = column( "parent_two" );

		final ForeignKey first = table.createForeignKey(
				null,
				new ForeignKeyColumnMappings( List.of(
						new ForeignKeyColumnMapping( childOne, parentOne ),
						new ForeignKeyColumnMapping( childTwo, parentTwo )
				) ),
				"Parent",
				null,
				null
		);
		final ForeignKey second = table.createForeignKey(
				null,
				new ForeignKeyColumnMappings( List.of(
						new ForeignKeyColumnMapping( childOne, parentTwo ),
						new ForeignKeyColumnMapping( childTwo, parentOne )
				) ),
				"Parent",
				null,
				null
		);

		assertNotSame( first, second );
		assertEquals( 2, table.getForeignKeyCollection().size() );
	}

	@Test
	void retainedColumnsAreProjectedFromCorrespondenceOrder() {
		final Table table = new Table( "orm", "child" );
		final Column childOne = column( "child_one" );
		final Column childTwo = column( "child_two" );
		final Column parentOne = column( "parent_one" );
		final Column parentTwo = column( "parent_two" );

		final ForeignKey foreignKey = table.createForeignKey(
				null,
				new ForeignKeyColumnMappings( List.of(
						new ForeignKeyColumnMapping( childTwo, parentTwo ),
						new ForeignKeyColumnMapping( childOne, parentOne )
				) ),
				"Parent",
				null,
				null
		);

		assertEquals( List.of( childTwo, childOne ), foreignKey.getColumns() );
		assertEquals( List.of( parentTwo, parentOne ), foreignKey.getReferencedColumns() );
	}

	private static Column column(String name) {
		return new Column( name );
	}
}
