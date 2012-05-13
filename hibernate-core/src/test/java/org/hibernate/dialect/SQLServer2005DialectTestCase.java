/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 *
 * @author Valotasion Yoryos
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SQLServer2005DialectTestCase extends BaseUnitTestCase {
	private SQLServer2005Dialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServer2005Dialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	public void testStripAliases() {
		String input = "some_field1 as f1, some_fild2 as f2, _field3 as f3 ";

		assertEquals( "some_field1, some_fild2, _field3", SQLServer2005Dialect.stripAliases(input) );
	}

	@Test
	public void testGetSelectFieldsWithoutAliases() {
		StringBuilder input = new StringBuilder( "select some_field1 as f12, some_fild2 as f879, _field3 as _f24674_3 from ...." );
		String output = SQLServer2005Dialect.getSelectFieldsWithoutAliases( input ).toString();

		assertEquals( " some_field1, some_fild2, _field3", output );
	}

	@Test
	public void testReplaceDistinctWithGroupBy() {
		StringBuilder input = new StringBuilder( "select distinct f1, f2 as ff, f3 from table where f1 = 5" );
		SQLServer2005Dialect.replaceDistinctWithGroupBy( input );

		assertEquals( "select f1, f2 as ff, f3 from table where f1 = 5 group by f1, f2, f3 ", input.toString() );
	}

	@Test
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc";

		assertEquals( "with query as (select f1 as f53245, row_number() over (order by f234, f67 desc) as __hibernate_row_nr__ from table849752  group by f1) select * from query where __hibernate_row_nr__ >= ? and __hibernate_row_nr__ < ?", dialect.getLimitString(input, 10, 15).toLowerCase() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6950")
	public void testGetLimitStringWithFromColumnName() {
		final String fromColumnNameSQL = "select persistent0_.rid as rid1688_, " +
				"persistent0_.deviationfromtarget as deviati16_1688_, " + // "from" character sequence as a part of the column name
				"persistent0_.sortindex as sortindex1688_ " +
				"from m_evalstate persistent0_ " +
				"where persistent0_.customerid=?";

		assertEquals(
				"WITH query AS (select persistent0_.rid as rid1688_, " +
						"persistent0_.deviationfromtarget as deviati16_1688_, " +
						"persistent0_.sortindex as sortindex1688_, " +
						"ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ " +
						"from m_evalstate persistent0_ " +
						"where persistent0_.customerid=?) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitString( fromColumnNameSQL, 1, 10 )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7019")
	public void testGetLimitStringWithSubselect() {
		final String subselectInSelectClauseSQL = "select persistent0_.id as col_0_0_, " +
				"(select max(persistent1_.acceptancedate) " +
				"from av_advisoryvariant persistent1_ " +
				"where persistent1_.clientid=persistent0_.id) as col_1_0_ " +
				"from c_customer persistent0_ " +
				"where persistent0_.type='v'";

		assertEquals(
				"WITH query AS (select persistent0_.id as col_0_0_, " +
						"(select max(persistent1_.acceptancedate) " +
						"from av_advisoryvariant persistent1_ " +
						"where persistent1_.clientid=persistent0_.id) as col_1_0_, " +
						"ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ " +
						"from c_customer persistent0_ " +
						"where persistent0_.type='v') " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitString( subselectInSelectClauseSQL, 2, 5 )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6728")
	public void testGetLimitStringCaseSensitive() {
		final String caseSensitiveSQL = "select persistent0_.id as col_0_0_, " +
				"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as col_1_0_ " +
				"from C_Customer persistent0_ " +
				"where persistent0_.type='Va' " +
				"order by persistent0_.Order";

		assertEquals(
				"WITH query AS (select persistent0_.id as col_0_0_, " +
						"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as col_1_0_, " +
						"ROW_NUMBER() OVER (order by persistent0_.Order) as __hibernate_row_nr__ " +
						"from C_Customer persistent0_ " +
						"where persistent0_.type='Va' ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitString( caseSensitiveSQL, 1, 2 )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6310")
	public void testGetLimitStringDistinctWithinAggregation() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) as f1 from table849752 p order by f1";

		assertEquals(
				"WITH query AS (select aggregate_function(distinct p.n) as f1, " +
						"ROW_NUMBER() OVER (order by f1) as __hibernate_row_nr__ from table849752 p ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitString( distinctInAggregateSQL, 2, 5 )
		);
	}
}
