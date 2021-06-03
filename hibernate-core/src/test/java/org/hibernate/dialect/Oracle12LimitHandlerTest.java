package org.hibernate.dialect;

import org.hibernate.dialect.pagination.Oracle12LimitHandler;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-14649")
public class Oracle12LimitHandlerTest {

	@Test
	public void testSqlWithSpace() {
		final String sql = "select  p.name from Person p where p.id = 1 for update";
		final String expected = "select * from ( select  p.name from Person p where p.id = 1 ) where rownum <= ? for update";

		final QueryParameters queryParameters = getQueryParameters( 0, 5 );
		final String processedSql = Oracle12LimitHandler.INSTANCE.processSql( sql, queryParameters );

		assertEquals( expected, processedSql );
	}

	@Test
	public void testSqlWithSpaceInsideQuotedString() {
		final String sql = "select p.name from Person p where p.name =  ' this is a  string with spaces  ' for update";
		final String expected = "select * from ( select p.name from Person p where p.name =  ' this is a  string with spaces  ' ) where rownum <= ? for update";

		final QueryParameters queryParameters = getQueryParameters( 0, 5 );
		final String processedSql = Oracle12LimitHandler.INSTANCE.processSql( sql, queryParameters );

		assertEquals( expected, processedSql );
	}

	@Test
	public void testSqlWithForUpdateInsideQuotedString() {
		final String sql = "select a.prop from A a where a.name =  'this is for update '";
		final String expected = "select a.prop from A a where a.name =  'this is for update ' fetch first ? rows only";

		final QueryParameters queryParameters = getQueryParameters( 0, 5 );
		final String processedSql = Oracle12LimitHandler.INSTANCE.processSql( sql, queryParameters );

		assertEquals( expected, processedSql );
	}

	@Test
	public void testSqlWithForUpdateInsideAndOutsideQuotedStringA() {
		final String sql = "select a.prop from A a where a.name =  'this is for update ' for update";
		final String expected = "select * from ( select a.prop from A a where a.name =  'this is for update ' ) where rownum <= ? for update";

		final QueryParameters queryParameters = getQueryParameters( 0, 5 );
		final String processedSql = Oracle12LimitHandler.INSTANCE.processSql( sql, queryParameters );

		assertEquals( expected, processedSql );
	}

	private QueryParameters getQueryParameters(int firstRow, int maxRow) {
		final QueryParameters queryParameters = new QueryParameters();
		RowSelection rowSelection = new RowSelection();
		rowSelection.setFirstRow( firstRow );
		rowSelection.setMaxRows( maxRow );
		queryParameters.setRowSelection( rowSelection );
		return queryParameters;
	}
}
