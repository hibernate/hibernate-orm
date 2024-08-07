/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.util.Locale;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.query.spi.Limit;

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
 * @author Lukasz Antoniak
 * @author Chris Cranford
 */
public class SQLServer2005DialectTestCase extends BaseUnitTestCase {
	private SQLServerLegacyDialect dialect;

	@Before
	public void setup() {
		dialect = new SQLServerLegacyDialect( DatabaseVersion.make( 9 ) );
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	public void testGetLimitString() {
		String input = "select distinct f1 as f53245 from table849752 order by f234, f67 desc";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select distinct top(?) f1 as f53245 from table849752 order by f234, f67 desc) row_)" +
						" select f53245 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( input, toRowSelection( 10, 15 ) ).toLowerCase(Locale.ROOT) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelect() {
		final String query = "select" + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						query + ") row_) select * from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelectWithMultipleSpaces() {
		final String query = "select    " + System.lineSeparator() + "* FROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						query + ") row_) select * from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8507")
	public void testGetLimitStringWithNewlineAfterColumnList() {
		final String query = "select E.fieldA,E.fieldB\r\nFROM Employee E WHERE E.firstName = :firstName";
		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select E.fieldA as col0_,E.fieldB\r\n as col1_" +
						"FROM Employee E WHERE E.firstName = :firstName) row_) select col0_,col1_ from query_ " +
						"where rownumber_>=? and rownumber_<?",
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
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						fromColumnNameSQL + ") row_) " +
						"select rid1688_,deviati16_1688_,sortindex1688_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( fromColumnNameSQL, toRowSelection( 1, 10 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8301")
	public void testGetLimitStringAliasGeneration() {
		final String notAliasedSQL = "select column1, column2, column3, column4 from table1";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select column1 as col0_, column2 as col1_, column3 as col2_, column4 as col3_ from table1) row_) " +
						"select col0_,col1_,col2_,col3_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( notAliasedSQL, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringAliasGenerationWithAliasesNoAs() {
		final String aliasedSQLNoAs = "select column1 c1, column c2, column c3, column c4 from table1";
		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select column1 c1, column c2, column c3, column c4 from table1) row_) " +
						"select c1,c2,c3,c4 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( aliasedSQLNoAs, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11352")
	public void testPagingWithColumnNameStartingWithFrom() {
		final String sql = "select column1 c1, from_column c2 from table1";
		assertEquals( "with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
				"select column1 c1, from_column c2 from table1) row_) " +
				"select c1,c2 from query_ where rownumber_>=? and rownumber_<?",
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
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						subselectInSelectClauseSQL + ") row_) " +
						"select col_0_0_,col_1_0_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( subselectInSelectClauseSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselect() {
		final String selectDistinctSubselectSQL = "select col0_.CONTENTID as CONTENT1_12_ " +
				"where col0_.CONTENTTYPE='PAGE' and (col0_.CONTENTID in " +
				"(select distinct col2_.PREVVER from CONTENT col2_ where (col2_.PREVVER is not null)))";

		assertEquals(
				"select top(?) col0_.CONTENTID as CONTENT1_12_ " +
						"where col0_.CONTENTTYPE='PAGE' and (col0_.CONTENTID in " +
						"(select distinct col2_.PREVVER from CONTENT col2_ where (col2_.PREVVER is not null)))",
				dialect.getLimitHandler().processSql( selectDistinctSubselectSQL, toRowSelection( null, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselectNotFirst() {
		final String selectDistinctSubselectSQL = "select col0_.CONTENTID as CONTENT1_12_ FROM CONTEXT col0_ " +
				"where col0_.CONTENTTYPE='PAGE' and (col0_.CONTENTID in " +
				"(select distinct col2_.PREVVER from CONTENT col2_ where (col2_.PREVVER is not null)))";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ " +
				"from (" + selectDistinctSubselectSQL + ") row_) " +
				"select CONTENT1_12_ from query_ where rownumber_>=? and rownumber_<?",
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
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) persistent0_.id as col0_, persistent0_.uid AS tmp1, " +
						"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as col1_ " +
						"from C_Customer persistent0_ where persistent0_.type='Va' order by persistent0_.Order) " +
						"row_) select col0_,tmp1,col1_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( caseSensitiveSQL, toRowSelection( 1, 2 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6310")
	public void testGetLimitStringDistinctWithinAggregation() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) as f1 from table849752 p order by f1";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) aggregate_function(distinct p.n) as f1 from table849752 p order by f1) row_) " +
						"select f1 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithoutAlias() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) from table849752 p order by f1";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) aggregate_function(distinct p.n) as col0_ from table849752 p order by f1) row_) " +
						"select col0_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithAliasNoAs() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) f1 from table849752 p order by f1";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) aggregate_function(distinct p.n) f1 from table849752 p order by f1) row_) " +
						"select f1 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7370")
	public void testGetLimitStringWithMaxOnly() {
		final String query = "select product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select top(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( query, toRowSelection( null, 1 ) )
		);

		final String distinctQuery = "select distinct product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from Product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select distinct top(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from Product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( distinctQuery, toRowSelection( null, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7781")
	public void testGetLimitStringWithCastOperator() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiID as col_1_0_ " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC) row_) " +
						"select col_0_0_,col_1_0_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithAliasNoAs() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)) f1, lc302_doku6_.dokumentiID f2 " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) f1, lc302_doku6_.dokumentiID f2 " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC) row_) " +
						"select f1,f2 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithoutAliases() {
		final String query = "select cast(lc302_doku6_.redniBrojStavke as varchar(255)), lc302_doku6_.dokumentiID " +
				"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) cast(lc302_doku6_.redniBrojStavke as varchar(255)) as col0_, lc302_doku6_.dokumentiID as col1_ " +
						"from LC302_Dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiID DESC) row_) " +
						"select col0_,col1_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingMultipleColumnsFromSeveralTables() {
		final String query = "select t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc) row_) " +
						"select * from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingAllColumns() {
		final String query = "select * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc) row_) " +
						"select * from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithFromInColumnName() {
		final String query = "select [Created From Nonstock Item], field2 from table1";

		assertEquals( "with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select [Created From Nonstock Item] as col0_, field2 as col1_ from table1) row_) " +
						"select col0_,col1_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAlias() {
		final String query = "select [Created From Item] c1, field2 from table1";

		assertEquals( "with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select [Created From Item] c1, field2 as col0_ from table1) row_) " +
						"select c1,col0_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAliasWithAs() {
		final String query = "select [Created From Item] as c1, field2 from table1";

		assertEquals( "with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select [Created From Item] as c1, field2 as col0_ from table1) row_) " +
						"select c1,col0_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesis() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC) row_) " +
						"select col_0_0,col_1_0 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11650")
	public void testGetLimitWithStringValueContainingParenthesis() {
		final String query = "select t1.c1 as col_0_0 FROM table1 t1 where t1.c1 = '(123' ORDER BY t1.c1 ASC";

		assertEquals(
				"with query_ as (select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"select top(?) t1.c1 as col_0_0 FROM table1 t1 where t1.c1 = '(123' ORDER BY t1.c1 ASC) row_) select col_0_0 from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesisOnlyTop() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC";

		assertEquals(
				"select top(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 WHERE (t2.c1 in (?))) as col_1_0 from table1 t1 WHERE 1=1 ORDER BY t1.c1 ASC",
				dialect.getLimitHandler().processSql( query, toRowSelection( null, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryNoOffset() {
		Limit selection = toRowSelection( null, 5 );

		// test top-based CTE with single CTE query_ definition with no odd formatting
		final String query1 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT top(?) c1, c2 FROM a",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test top-based CTE with single CTE query_ definition and various tab, newline spaces
		final String query2 = "  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT top(?) c1, c2 FROM a",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test top-based CTE with multiple CTE query_ definitions with no odd formatting
		final String query3 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
				"SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
						"SELECT top(?) c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query_ definitions and various tab, newline spaces
		final String query4 = "  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t" +
				"(SELECT b1, b2 FROM t2)    SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, b2 FROM t2)" +
						"    SELECT top(?) c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query4, selection )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryWithOffset() {
		Limit selection = toRowSelection( 1, 5 );

		// test non-top based CTE with single CTE query_ definition with no odd formatting
		final String query1 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t) SELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t) , query_ as (select row_.*,row_number() over " +
						"(order by current_timestamp) as rownumber_ from (SELECT c1 as col0_, c2 as col1_ " +
						"FROM a) row_) select col0_,col1_ from query_ where rownumber_>=? " +
						"and rownumber_<?",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test non-top based CTE with single CTE query_ definition and various tab, newline spaces
		final String query2 = "  \n\tWITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\nSELECT c1, c2 FROM a";
		assertEquals(
				"WITH a (c1\n\t,c2)\t\nAS (SELECT\n\tc1,c2 FROM t)\t\n, query_ as (select row_.*,row_number()" +
						" over (order by current_timestamp) as rownumber_ from (SELECT c1 as col0_, c2 " +
						"as col1_ FROM a) row_) select col0_,col1_ from query_ where rownumber_>=" +
						"? and rownumber_<?",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test non-top based CTE with multiple CTE query_ definitions with no odd formatting
		final String query3 = "WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2) " +
				" SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS (SELECT c1, c2 FROM t1), b (b1, b2) AS (SELECT b1, b2 FROM t2)  , query_ as (" +
						"select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"SELECT c1 as col0_, c2 as col1_, b1 as col2_, b2 as col3_ FROM t1, t2 WHERE t1.c1 = t2.b1) row_)" +
						" select col0_,col1_,col2_,col3_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query_ definitions and various tab, newline spaces
		final String query4 = "  \n\r\tWITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, " +
				"b2 FROM t2)    SELECT c1, c2, b1, b2 FROM t1, t2 WHERE t1.c1 = t2.b1";
		assertEquals(
				"WITH a (c1, c2) AS\n\r (SELECT c1, c2 FROM t1)\n\r, b (b1, b2)\tAS\t(SELECT b1, b2 FROM t2)    , query_ as (" +
						"select row_.*,row_number() over (order by current_timestamp) as rownumber_ from (" +
						"SELECT c1 as col0_, c2 as col1_, b1 as col2_, b2 as col3_ FROM t1, t2 WHERE t1.c1 = t2.b1) row_)" +
						" select col0_,col1_,col2_,col3_ from query_ where rownumber_>=? and rownumber_<?",
				dialect.getLimitHandler().processSql( query4, selection )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLocking() {
		final String expectedLockHint = "tab1 with (updlock,rowlock,readpast)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintReadPastLockingNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock,rowlock,readpast,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_SKIPLOCKED );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticRead() {
		final String expectedLockHint = "tab1 with (holdlock,rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticReadNoTimeOut() {
		final String expectedLockHint = "tab1 with (holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_READ );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWrite() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintWriteWithNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.WRITE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );

		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWait() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoWaitNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.UPGRADE_NOWAIT );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgrade() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintUpgradeNoTimeout() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWrite() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9635")
	public void testAppendLockHintPessimisticWriteNoTimeOut() {
		final String expectedLockHint = "tab1 with (updlock,holdlock,rowlock,nowait)";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setTimeOut( LockOptions.NO_WAIT );
		String lockHint = dialect.appendLockHint( lockOptions, "tab1" );

		assertEquals( expectedLockHint, lockHint );
	}

	private Limit toRowSelection(Integer firstRow, Integer maxRows) {
		Limit selection = new Limit();
		selection.setFirstRow( firstRow );
		selection.setMaxRows( maxRows );
		return selection;
	}
}
