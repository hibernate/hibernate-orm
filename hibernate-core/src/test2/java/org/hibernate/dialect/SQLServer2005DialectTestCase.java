/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.util.Locale;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.RowSelection;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of the behavior of the SQLServerDialect utility methods
 *
 * @author Valotasion Yoryos
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
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
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __hibernate_row_nr__ from ( " +
						"select distinct top(?) f1 as f53245 from table849752 order by f234, f67 desc ) inner_query )" +
						" select f53245 from query where __hibernate_row_nr__ >= ? and __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( input, toRowSelection( 10, 15 ) ).toLowerCase(Locale.ROOT) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelect() {
		final String query = "select" + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						query + " ) inner_query ) SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelectWithMultipleSpaces() {
		final String query = "select    " + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						query + " ) inner_query ) SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8507")
	public void testGetLimitStringWithNewlineAfterColumnList() {
		final String query = "select E.fieldA,E.fieldB\r\nFROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select E.fieldA as page0_,E.fieldB as page1_\r\n" +
						"FROM Employee E WHERE E.firstName = :firstName ) inner_query ) SELECT page0_, page1_ FROM query " +
						"WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
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
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						fromColumnNameSQL + " ) inner_query ) " +
						"SELECT rid1688_, deviati16_1688_, sortindex1688_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( fromColumnNameSQL, toRowSelection( 1, 10 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8301")
	public void testGetLimitStringAliasGeneration() {
		final String notAliasedSQL = "select column1, column2, column3, column4 from table1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select column1 as page0_, column2 as page1_, column3 as page2_, column4 as page3_ from table1 ) inner_query ) " +
						"SELECT page0_, page1_, page2_, page3_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( notAliasedSQL, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringAliasGenerationWithAliasesNoAs() {
		final String aliasedSQLNoAs = "select column1 c1, column c2, column c3, column c4 from table1";
		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select column1 c1, column c2, column c3, column c4 from table1 ) inner_query ) " +
						"SELECT c1, c2, c3, c4 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( aliasedSQLNoAs, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11352")
	public void testPagingWithColumnNameStartingWithFrom() {
		final String sql = "select column1 c1, from_column c2 from table1";
		assertEquals( "WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
				"select column1 c1, from_column c2 from table1 ) inner_query ) " +
				"SELECT c1, c2 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql(sql, toRowSelection(3, 5)));
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
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						subselectInSelectClauseSQL + " ) inner_query ) " +
						"SELECT col_0_0_, col_1_0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( subselectInSelectClauseSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselect() {
		final String selectDistinctSubselectSQL = "select page0_.CONTENTID as CONTENT1_12_ " +
				"where page0_.CONTENTTYPE='PAGE' and (page0_.CONTENTID in " +
				"(select distinct page2_.PREVVER from CONTENT page2_ where (page2_.PREVVER is not null)))";

		assertEquals(
				"select TOP(?) page0_.CONTENTID as CONTENT1_12_ " +
						"where page0_.CONTENTTYPE='PAGE' and (page0_.CONTENTID in " +
						"(select distinct page2_.PREVVER from CONTENT page2_ where (page2_.PREVVER is not null)))",
				dialect.getLimitHandler().processSql( selectDistinctSubselectSQL, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselectNotFirst() {
		final String selectDistinctSubselectSQL = "select page0_.CONTENTID as CONTENT1_12_ FROM CONTEXT page0_ " +
				"where page0_.CONTENTTYPE='PAGE' and (page0_.CONTENTID in " +
				"(select distinct page2_.PREVVER from CONTENT page2_ where (page2_.PREVVER is not null)))";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ " +
				"FROM ( " + selectDistinctSubselectSQL + " ) inner_query ) " +
				"SELECT CONTENT1_12_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( selectDistinctSubselectSQL, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6728")
	public void testGetLimitStringCaseSensitive() {
		final String caseSensitiveSQL = "select persistent0_.id, persistent0_.uid AS tmp1, " +
				"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) " +
				"from C_Customer persistent0_ " +
				"where persistent0_.type='Va' " +
				"order by persistent0_.Order";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) persistent0_.id as page0_, persistent0_.uid AS tmp1, " +
						"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as page1_ " +
						"from C_Customer persistent0_ where persistent0_.type='Va' order by persistent0_.Order ) " +
						"inner_query ) SELECT page0_, tmp1, page1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( caseSensitiveSQL, toRowSelection( 1, 2 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6310")
	public void testGetLimitStringDistinctWithinAggregation() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) as f1 from table849752 p order by f1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) aggregate_function(distinct p.n) as f1 from table849752 p order by f1 ) inner_query ) " +
						"SELECT f1 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithoutAlias() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) from table849752 p order by f1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) aggregate_function(distinct p.n) as page0_ from table849752 p order by f1 ) inner_query ) " +
						"SELECT page0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithAliasNoAs() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) f1 from table849752 p order by f1";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) aggregate_function(distinct p.n) f1 from table849752 p order by f1 ) inner_query ) " +
						"SELECT f1 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7370")
	public void testGetLimitStringWithMaxOnly() {
		final String query = "select product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select TOP(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( query, toRowSelection( 0, 1 ) )
		);

		final String distinctQuery = "select distinct product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select distinct TOP(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( distinctQuery, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7781")
	public void testGetLimitStringWithCastOperator() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC ) inner_query ) " +
						"SELECT col_0_0_, col_1_0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithAliasNoAs() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)) f1, lc302_doku6_.dokumentiID f2 " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) f1, lc302_doku6_.dokumentiID f2 " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC ) inner_query ) " +
						"SELECT f1, f2 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithoutAliases() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)), lc302_doku6_.dokumentiID " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) as page0_, lc302_doku6_.dokumentiID as page1_ " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC ) inner_query ) " +
						"SELECT page0_, page1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingMultipleColumnsFromSeveralTables() {
		final String query = "select t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingAllColumns() {
		final String query = "select * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"SELECT * FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithFromInColumnName() {
		final String query = "select [Created From Nonstock Item], field2 from table1";

		assertEquals( "WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select [Created From Nonstock Item] as page0_, field2 as page1_ from table1 ) inner_query ) " +
						"SELECT page0_, page1_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAlias() {
		final String query = "select [Created From Item] c1, field2 from table1";

		assertEquals( "WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select [Created From Item] c1, field2 as page0_ from table1 ) inner_query ) " +
						"SELECT c1, page0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAliasWithAs() {
		final String query = "select [Created From Item] as c1, field2 from table1";

		assertEquals( "WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select [Created From Item] as c1, field2 as page0_ from table1 ) inner_query ) " +
						"SELECT c1, page0_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesis() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC ) inner_query ) " +
						"SELECT col_0_0, col_1_0 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11650")
	public void testGetLimitWithStringValueContainingParenthesis() {
		final String query = "select t1.c1 as col_0_0 FROM table1 t1 where t1.c1 = '(123' ORDER BY t1.c1 ASC";

		assertEquals(
				"WITH query AS (SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( " +
						"select TOP(?) t1.c1 as col_0_0 FROM table1 t1 where t1.c1 = '(123' ORDER BY t1.c1 ASC ) inner_query ) SELECT col_0_0 FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesisOnlyTop() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC";

		assertEquals(
				"select TOP(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC",
				dialect.getLimitHandler().processSql( query, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryNoOffset() {
		RowSelection selection = toRowSelection( 0, 5 );

		// test top-based CTE with single CTE query definition with no odd formatting
		final String query1 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT TOP(?) c1, c2 FROM a",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test top-based CTE with single CTE query definition and various tab, newline spaces
		final String query2 = "  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT c1, c2 FROM a";
		assertEquals(
				"  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT TOP(?) c1, c2 FROM a",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test top-based CTE with multiple CTE query definitions with no odd formatting
		final String query3 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
				"SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
						"SELECT TOP(?) c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query definitions and various tab, newline spaces
		final String query4 = "  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t" +
				"(SELECT b1, b2 FROM t2)    SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, b2 FROM t2)" +
						"    SELECT TOP(?) c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query4, selection )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryWithOffset() {
		RowSelection selection = toRowSelection( 1, 5 );

		// test non-top based CTE with single CTE query definition with no odd formatting
		final String query1 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t), query AS (SELECT inner_query.*, ROW_NUMBER() OVER " +
						"(ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM (  SELECT c1 as page0_, c2 as page1_ " +
						"FROM a ) inner_query ) SELECT page0_, page1_ FROM query WHERE __hibernate_row_nr__ >= ? " +
						"AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test non-top based CTE with single CTE query definition and various tab, newline spaces
		final String query2 = "  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT c1, c2 FROM a";
		assertEquals(
				"  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t), query AS (SELECT inner_query.*, ROW_NUMBER()" +
						" OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM ( \t\nSELECT c1 as page0_, c2 " +
						"as page1_ FROM a ) inner_query ) SELECT page0_, page1_ FROM query WHERE __hibernate_row_nr__ >= " +
						"? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test non-top based CTE with multiple CTE query definitions with no odd formatting
		final String query3 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
				" SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2), query AS (" +
						"SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM (" +
						"   SELECT c1 as page0_, c2 as page1_, b1 as page2_, b2 as page3_ FROM t1, t2 WHERE t1.c1 = t2.b1 ) inner_query )" +
						" SELECT page0_, page1_, page2_, page3_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query definitions and various tab, newline spaces
		final String query4 = "  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, " +
				"b2 FROM t2)    SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, b2 FROM t2), query AS (" +
						"SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__ FROM (" +
						"     SELECT c1 as page0_, c2 as page1_, b1 as page2_, b2 as page3_ FROM t1, t2 WHERE t1.c1 = t2.b1 ) inner_query )" +
						" SELECT page0_, page1_, page2_, page3_ FROM query WHERE __hibernate_row_nr__ >= ? AND __hibernate_row_nr__ < ?",
				dialect.getLimitHandler().processSql( query4, selection )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLocking() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, readpast)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLockingNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, rowlock, readpast, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticRead() {
		final String expectedLockHint = "tab1 with (holdlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticReadNoTimeOut() {
		final String expectedLockHint = "tab1 with (holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWrite() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWriteWithNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );

		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWait() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWaitNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgrade() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWrite() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWriteNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock, holdlock, rowlock, nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	private RowSelection toRowSelection(int firstRow, int maxRows) {
		RowSelection selection = new RowSelection();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
