/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.mapping.Column;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Aliases should always be lower-case. This tests that an alias for
 * a column name that ends in a character that is not a letter gets
 * generated to be all in lower-case.
 *
 * @author Gail Badner
 */
public class ColumnLastIndexNotLetterAliasTest {
	// Arbitrarily choose PostgreSQL
	private static final Dialect DIALECT = new PostgreSQL94Dialect();;

	@Test
	@TestForIssue(jiraKey = "HHH-14720")
	public void testColumnNameEndinWithNonCharacter() {
		test( "aColumn1" );
		test( "aColumn_" );
		test( "aVeryVeryVeryLongColumnName1" );
		test( "aVeryVeryVeryLongColumnName_" );
	}

	private void test(String columnName) {
		final Column column = new Column( columnName );
		final String alias = column.getAlias( DIALECT );
		assertEquals( alias.toLowerCase( Locale.ROOT ), alias );
	}
}
