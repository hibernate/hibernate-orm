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
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select distinct top(?) f1 as f53245 from table849752 order by f234, f67 desc ) inner_query )" +
						" select f53245 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( input, toRowSelection( 10, 15 ) ).toLowerCase(Locale.ROOT) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelect() {
		final String query = "select" + System.lineSeparator() + "* from Employee E where E.firstName = :firstName";
		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						query + " ) inner_query ) select * from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10736")
	public void testGetLimitStringWithNewlineAfterSelectWithMultipleSpaces() {
		final String query = "select    " + System.lineSeparator() + "* from employee e where e.firstName = :firstName";
		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						query + " ) inner_query ) select * from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 25 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8507")
	public void testGetLimitStringWithNewlineAfterColumnList() {
		final String query = "select e.fielda,e.fieldb\r\nfrom employee e where e.firstname = :firstname";
		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select e.fielda as page0_,e.fieldb as page1_\r\n" +
						"from employee e where e.firstname = :firstname ) inner_query ) select page0_, page1_ from query " +
						"where __row__ >= ? and __row__ < ?",
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
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						fromColumnNameSQL + " ) inner_query ) " +
						"select rid1688_, deviati16_1688_, sortindex1688_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( fromColumnNameSQL, toRowSelection( 1, 10 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8301")
	public void testGetLimitStringAliasGeneration() {
		final String notAliasedSQL = "select column1, column2, column3, column4 from table1";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select column1 as page0_, column2 as page1_, column3 as page2_, column4 as page3_ from table1 ) inner_query ) " +
						"select page0_, page1_, page2_, page3_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( notAliasedSQL, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringAliasGenerationWithAliasesNoAs() {
		final String aliasedSQLNoAs = "select column1 c1, column c2, column c3, column c4 from table1";
		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select column1 c1, column c2, column c3, column c4 from table1 ) inner_query ) " +
						"select c1, c2, c3, c4 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( aliasedSQLNoAs, toRowSelection( 3, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11352")
	public void testPagingWithColumnNameStartingWithFrom() {
		final String sql = "select column1 c1, from_column c2 from table1";
		assertEquals( "with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
				"select column1 c1, from_column c2 from table1 ) inner_query ) " +
				"select c1, c2 from query where __row__ >= ? and __row__ < ?",
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
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						subselectInSelectClauseSQL + " ) inner_query ) " +
						"select col_0_0_, col_1_0_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( subselectInSelectClauseSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselect() {
		final String selectDistinctSubselectSQL = "select page0_.contentid as content1_12_ " +
				"where page0_.contenttype='page' and (page0_.contentid in " +
				"(select distinct page2_.prevver from content page2_ where (page2_.prevver is not null)))";

		assertEquals(
				"select top(?) page0_.contentid as content1_12_ " +
						"where page0_.contenttype='page' and (page0_.contentid in " +
						"(select distinct page2_.prevver from content page2_ where (page2_.prevver is not null)))",
				dialect.getLimitHandler().processSql( selectDistinctSubselectSQL, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11084")
	public void testGetLimitStringWithSelectDistinctSubselectNotFirst() {
		final String selectDistinctSubselectSQL = "select page0_.contentid as content1_12_ from context page0_ " +
				"where page0_.contenttype='page' and (page0_.contentid in " +
				"(select distinct page2_.prevver from content page2_ where (page2_.prevver is not null)))";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ " +
				"from ( " + selectDistinctSubselectSQL + " ) inner_query ) " +
				"select content1_12_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( selectDistinctSubselectSQL, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6728")
	public void testGetLimitStringCaseSensitive() {
		final String caseSensitiveSQL = "select persistent0_.id, persistent0_.uid as tmp1, " +
				"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) " +
				"from c_customer persistent0_ " +
				"where persistent0_.type='Va' " +
				"order by persistent0_.Order";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) persistent0_.id as page0_, persistent0_.uid as tmp1, " +
						"(select case when persistent0_.name = 'Smith' then 'Neo' else persistent0_.id end) as page1_ " +
						"from c_customer persistent0_ where persistent0_.type='Va' order by persistent0_.Order ) " +
						"inner_query ) select page0_, tmp1, page1_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( caseSensitiveSQL, toRowSelection( 1, 2 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6310")
	public void testGetLimitStringDistinctWithinAggregation() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) as f1 from table849752 p order by f1";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) aggregate_function(distinct p.n) as f1 from table849752 p order by f1 ) inner_query ) " +
						"select f1 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithoutAlias() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) from table849752 p order by f1";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) aggregate_function(distinct p.n) as page0_ from table849752 p order by f1 ) inner_query ) " +
						"select page0_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringDistinctWithinAggregationWithAliasNoAs() {
		final String distinctInAggregateSQL = "select aggregate_function(distinct p.n) f1 from table849752 p order by f1";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) aggregate_function(distinct p.n) f1 from table849752 p order by f1 ) inner_query ) " +
						"select f1 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( distinctInAggregateSQL, toRowSelection( 2, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7370")
	public void testGetLimitStringWithMaxOnly() {
		final String query = "select product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select top(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( query, toRowSelection( 0, 1 ) )
		);

		final String distinctQuery = "select distinct product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
				"from product2 product2x0_ order by product2x0_.id";

		assertEquals(
				"select distinct top(?) product2x0_.id as id0_, product2x0_.description as descript2_0_ " +
						"from product2 product2x0_ order by product2x0_.id",
				dialect.getLimitHandler().processSql( distinctQuery, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7781")
	public void testGetLimitStringWithCastOperator() {
		final String query = "select cast(lc302_doku6_.rednibrojstavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiid as col_1_0_ " +
				"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) cast(lc302_doku6_.rednibrojstavke as varchar(255)) as col_0_0_, lc302_doku6_.dokumentiid as col_1_0_ " +
						"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc ) inner_query ) " +
						"select col_0_0_, col_1_0_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithAliasNoAs() {
		final String query = "select cast(lc302_doku6_.rednibrojstavke as varchar(255)) f1, lc302_doku6_.dokumentiid f2 " +
				"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) cast(lc302_doku6_.rednibrojstavke as varchar(255)) f1, lc302_doku6_.dokumentiid f2 " +
						"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc ) inner_query ) " +
						"select f1, f2 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10994")
	public void testGetLimitStringWithCastOperatorWithoutAliases() {
		final String query = "select cast(lc302_doku6_.rednibrojstavke as varchar(255)), lc302_doku6_.dokumentiid " +
				"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) cast(lc302_doku6_.rednibrojstavke as varchar(255)) as page0_, lc302_doku6_.dokumentiid as page1_ " +
						"from lc302_dokumenti lc302_doku6_ order by lc302_doku6_.dokumentiid desc ) inner_query ) " +
						"select page0_, page1_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingMultipleColumnsFromSeveralTables() {
		final String query = "select t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) t1.*, t2.* from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"select * from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8007")
	public void testGetLimitStringSelectingAllColumns() {
		final String query = "select * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) * from tab1 t1, tab2 t2 where t1.ref = t2.ref order by t1.id desc ) inner_query ) " +
						"select * from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 3 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithFromInColumnName() {
		final String query = "select [Created From Nonstock Item], field2 from table1";

		assertEquals( "with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select [Created From Nonstock Item] as page0_, field2 as page1_ from table1 ) inner_query ) " +
						"select page0_, page1_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAlias() {
		final String query = "select [Created From Item] c1, field2 from table1";

		assertEquals( "with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select [Created From Item] c1, field2 as page0_ from table1 ) inner_query ) " +
						"select c1, page0_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11145")
	public void testGetLimitStringWithQuotedColumnNamesAndAliasWithAs() {
		final String query = "select [Created From Item] as c1, field2 from table1";

		assertEquals( "with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select [Created From Item] as c1, field2 as page0_ from table1 ) inner_query ) " +
						"select c1, page0_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesis() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'added' else 'unmodified' end from table2 t2 where (t2.c1 in (?))) as col_1_0 from table1 t1 where 1=1 order by t1.c1 asc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'added' else 'unmodified' end from table2 t2 where (t2.c1 in (?))) as col_1_0 from table1 t1 where 1=1 order by t1.c1 asc ) inner_query ) " +
						"select col_0_0, col_1_0 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11650")
	public void testGetLimitWithStringValueContainingParenthesis() {
		final String query = "select t1.c1 as col_0_0 from table1 t1 where t1.c1 = '(123' order by t1.c1 asc";

		assertEquals(
				"with query as (select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( " +
						"select top(?) t1.c1 as col_0_0 from table1 t1 where t1.c1 = '(123' order by t1.c1 asc ) inner_query ) select col_0_0 from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query, toRowSelection( 1, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11324")
	public void testGetLimitStringWithSelectClauseNestedQueryUsingParenthesisOnlyTop() {
		final String query = "select t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 where (t2.c1 in (?))) as col_1_0 from table1 t1 where 1=1 order by t1.c1 asc";

		assertEquals(
				"select top(?) t1.c1 as col_0_0, (select case when count(t2.c1)>0 then 'ADDED' else 'UNMODIFIED' end from table2 t2 where (t2.c1 in (?))) as col_1_0 from table1 t1 where 1=1 order by t1.c1 asc",
				dialect.getLimitHandler().processSql( query, toRowSelection( 0, 5 ) )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryNoOffset() {
		RowSelection selection = toRowSelection( 0, 5 );

		// test top-based CTE with single CTE query definition with no odd formatting
		final String query1 = "with a (c1, c2) as (select c1, c2 from t) select c1, c2 from a";
		assertEquals(
				"with a (c1, c2) as (select c1, c2 from t) select top(?) c1, c2 from a",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test top-based CTE with single CTE query definition and various tab, newline spaces
		final String query2 = "  \n\twith a (c1\n\t,c2)\t\nas (select\n\tc1,c2 from t)\t\nselect c1, c2 from a";
		assertEquals(
				"  \n\twith a (c1\n\t,c2)\t\nas (select\n\tc1,c2 from t)\t\nselect top(?) c1, c2 from a",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test top-based CTE with multiple CTE query definitions with no odd formatting
		final String query3 = "with a (c1, c2) as (select c1, c2 from t1), b (b1, b2) as (select b1, b2 from t2) " +
				"select c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1";
		assertEquals(
				"with a (c1, c2) as (select c1, c2 from t1), b (b1, b2) as (select b1, b2 from t2) " +
						"select top(?) c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query definitions and various tab, newline spaces
		final String query4 = "  \n\r\twith a (c1, c2) as\n\r (select c1, c2 from t1)\n\r, b (b1, b2)\tas\t" +
				"(select b1, b2 from t2)    select c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1";
		assertEquals(
				"  \n\r\twith a (c1, c2) as\n\r (select c1, c2 from t1)\n\r, b (b1, b2)\tas\t(select b1, b2 from t2)" +
						"    select top(?) c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1",
				dialect.getLimitHandler().processSql( query4, selection )
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8916")
	public void testGetLimitStringUsingCTEQueryWithOffset() {
		RowSelection selection = toRowSelection( 1, 5 );

		// test non-top based CTE with single CTE query definition with no odd formatting
		final String query1 = "with a (c1, c2) as (select c1, c2 from t) select c1, c2 from a";
		assertEquals(
				"with a (c1, c2) as (select c1, c2 from t), query as (select inner_query.*, row_number() over " +
						"(order by current_timestamp) as __row__ from (  select c1 as page0_, c2 as page1_ " +
						"from a ) inner_query ) select page0_, page1_ from query where __row__ >= ? " +
						"and __row__ < ?",
				dialect.getLimitHandler().processSql( query1, selection )
		);

		// test non-top based CTE with single CTE query definition and various tab, newline spaces
		final String query2 = "  \n\twith a (c1\n\t,c2)\t\nas (select\n\tc1,c2 from t)\t\nselect c1, c2 from a";
		assertEquals(
				"  \n\twith a (c1\n\t,c2)\t\nas (select\n\tc1,c2 from t), query as (select inner_query.*, row_number()" +
						" over (order by current_timestamp) as __row__ from ( \t\nselect c1 as page0_, c2 " +
						"as page1_ from a ) inner_query ) select page0_, page1_ from query where __row__ >= " +
						"? and __row__ < ?",
				dialect.getLimitHandler().processSql( query2, selection )
		);

		// test non-top based CTE with multiple CTE query definitions with no odd formatting
		final String query3 = "with a (c1, c2) as (select c1, c2 from t1), b (b1, b2) as (select b1, b2 from t2) " +
				" select c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1";
		assertEquals(
				"with a (c1, c2) as (select c1, c2 from t1), b (b1, b2) as (select b1, b2 from t2), query as (" +
						"select inner_query.*, row_number() over (order by current_timestamp) as __row__ from (" +
						"   select c1 as page0_, c2 as page1_, b1 as page2_, b2 as page3_ from t1, t2 where t1.c1 = t2.b1 ) inner_query )" +
						" select page0_, page1_, page2_, page3_ from query where __row__ >= ? and __row__ < ?",
				dialect.getLimitHandler().processSql( query3, selection )
		);

		// test top-based CTE with multiple CTE query definitions and various tab, newline spaces
		final String query4 = "  \n\r\twith a (c1, c2) as\n\r (select c1, c2 from t1)\n\r, b (b1, b2)\tas\t(select b1, " +
				"b2 from t2)    select c1, c2, b1, b2 from t1, t2 where t1.c1 = t2.b1";
		assertEquals(
				"  \n\r\twith a (c1, c2) as\n\r (select c1, c2 from t1)\n\r, b (b1, b2)\tas\t(select b1, b2 from t2), query as (" +
						"select inner_query.*, row_number() over (order by current_timestamp) as __row__ from (" +
						"     select c1 as page0_, c2 as page1_, b1 as page2_, b2 as page3_ from t1, t2 where t1.c1 = t2.b1 ) inner_query )" +
						" select page0_, page1_, page2_, page3_ from query where __row__ >= ? and __row__ < ?",
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
