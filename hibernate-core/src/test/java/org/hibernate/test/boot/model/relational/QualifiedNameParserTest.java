/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedNameParser;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10174")
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
