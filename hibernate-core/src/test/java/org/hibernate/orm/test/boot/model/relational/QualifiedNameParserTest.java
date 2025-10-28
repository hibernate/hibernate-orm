/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.model.relational;

import org.hamcrest.MatcherAssert;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-10174")
public class QualifiedNameParserTest {

	private static final Identifier DEFAULT_SCHEMA = Identifier.toIdentifier( "schema" );
	private static final Identifier DEFAULT_CATALOG = Identifier.toIdentifier( "catalog" );

	private static final QualifiedNameParser PARSER = new QualifiedNameParser();

	@Test
	public void testStringSplittingWithSchema() {
		QualifiedNameParser.NameParts nameParts = PARSER.parse( "schema.MyEntity", null, DEFAULT_SCHEMA );

		MatcherAssert.assertThat( nameParts.getCatalogName(), is( nullValue() ) );
		MatcherAssert.assertThat( nameParts.getSchemaName().getText(), is( DEFAULT_SCHEMA.getText() ) );
		MatcherAssert.assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}

	@Test
	public void testStringSplittingWithCatalogAndSchema() {
		QualifiedNameParser.NameParts nameParts = PARSER.parse(
				"catalog.schema.MyEntity",
				DEFAULT_CATALOG,
				DEFAULT_SCHEMA
		);

		MatcherAssert.assertThat( nameParts.getCatalogName().getText(), is( DEFAULT_CATALOG.getText() ) );
		MatcherAssert.assertThat( nameParts.getSchemaName().getText(), is( DEFAULT_SCHEMA.getText() ) );
		MatcherAssert.assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}

	@Test
	public void testStringSplittingWithoutCatalogAndSchema() {
		QualifiedNameParser.NameParts nameParts = PARSER.parse(
				"MyEntity",
				null,
				null
		);

		MatcherAssert.assertThat( nameParts.getCatalogName(), is( nullValue() ) );
		MatcherAssert.assertThat( nameParts.getSchemaName(), is( nullValue() ) );
		MatcherAssert.assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}
}
