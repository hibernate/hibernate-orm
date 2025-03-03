/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

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

		assertThat( nameParts.getCatalogName(), is( nullValue() ) );
		assertThat( nameParts.getSchemaName().getText(), is( DEFAULT_SCHEMA.getText() ) );
		assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}

	@Test
	public void testStringSplittingWithCatalogAndSchema() {
		QualifiedNameParser.NameParts nameParts = PARSER.parse(
				"schema.catalog.MyEntity",
				DEFAULT_CATALOG,
				DEFAULT_SCHEMA
		);

		assertThat( nameParts.getCatalogName().getText(), is( DEFAULT_CATALOG.getText() ) );
		assertThat( nameParts.getSchemaName().getText(), is( DEFAULT_SCHEMA.getText() ) );
		assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}

	@Test
	public void testStringSplittingWithoutCatalogAndSchema() {
		QualifiedNameParser.NameParts nameParts = PARSER.parse(
				"MyEntity",
				null,
				null
		);

		assertThat( nameParts.getCatalogName(), is( nullValue() ) );
		assertThat( nameParts.getSchemaName(), is( nullValue() ) );
		assertThat( nameParts.getObjectName().getText(), is( "MyEntity" ) );
	}
}
