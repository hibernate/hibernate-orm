/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.QualifiedNameParser.NameParts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class QualifiedNameParserTests {
	private final Identifier NO_CATALOG = Identifier.toIdentifier( "" );
	private final Identifier NO_SCHEMA = Identifier.toIdentifier( "" );

	private final Identifier CATALOG1 = Identifier.toIdentifier( "catalog1", false );
	private final Identifier SCHEMA1 = Identifier.toIdentifier( "schema1", false );

	private final Identifier CATALOG2 = Identifier.toIdentifier( "catalog2", true );
	private final Identifier SCHEMA2 = Identifier.toIdentifier( "schema2", true );

	@Test
	void testSimpleUnquoted() {
		final String nameText = "tbl";
		final Identifier name = Identifier.toIdentifier( nameText );
		assert !name.isQuoted();

		test(
				nameText,
				NO_CATALOG,
				NO_SCHEMA,
				NO_CATALOG,
				NO_SCHEMA,
				name
		);
		test(
				nameText,
				CATALOG1,
				SCHEMA1,
				CATALOG1,
				SCHEMA1,
				name
		);
	}

	@Test
	void testSimpleQuoted() {
		final String nameText = "`tbl`";
		final Identifier name = Identifier.toIdentifier( nameText );
		assert name.isQuoted();

		test(
				nameText,
				NO_CATALOG,
				NO_SCHEMA,
				NO_CATALOG,
				NO_SCHEMA,
				name
		);
		test(
				nameText,
				CATALOG1,
				SCHEMA1,
				CATALOG1,
				SCHEMA1,
				name
		);
	}

	@Test
	void testIndividualQuotes() {
		final String name = "`schema2`.`catalog2`.`tbl`";
		test(
				name,
				NO_CATALOG,
				NO_SCHEMA,
				CATALOG2,
				SCHEMA2,
				Identifier.toIdentifier( "tbl", true )
		);
		test(
				name,
				CATALOG1,
				SCHEMA1,
				CATALOG2,
				SCHEMA2,
				Identifier.toIdentifier( "tbl", true )
		);
	}

	@Test
	void testCrazyName() {
		final String nameText = "`abc.def.ghi::other.stuff`";
		final Identifier name = Identifier.toIdentifier( nameText );
		assert name.isQuoted();

		test(
				nameText,
				NO_CATALOG,
				NO_SCHEMA,
				NO_CATALOG,
				NO_SCHEMA,
				name
		);

		test(
				nameText,
				CATALOG1,
				SCHEMA1,
				CATALOG1,
				SCHEMA1,
				name
		);
	}


	private static void test(
			String name,
			Identifier defaultCatalogName,
			Identifier defaultSchemaName,
			Identifier expectedCatalogName,
			Identifier expectedSchemaName,
			Identifier expectedName) {
		final NameParts parsed = QualifiedNameParser.INSTANCE.parse( name, defaultCatalogName, defaultSchemaName );

		assertSame( parsed.getCatalogName(), expectedCatalogName );
		assertSame( parsed.getSchemaName(), expectedSchemaName );
		assertSame( parsed.getObjectName(), expectedName );
	}

	private static void assertSame(Identifier one, Identifier another) {
		if ( one == null ) {
			assertThat( another ).isNull();
		}
		else {
			assertThat( one ).isEqualTo( another );
		}
	}
}
