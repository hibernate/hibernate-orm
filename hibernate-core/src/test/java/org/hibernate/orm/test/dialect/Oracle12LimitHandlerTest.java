/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.pagination.AbstractLimitHandler;

import org.hibernate.dialect.pagination.Oracle12LimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.testing.TestForIssue;

import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.pagination.AbstractLimitHandler.hasFirstRow;
import static org.hibernate.dialect.pagination.AbstractLimitHandler.hasMaxRows;

@TestForIssue( jiraKey = "HHH-14649")
public class Oracle12LimitHandlerTest extends AbstractLimitHandlerTest {

	@Override
	protected AbstractLimitHandler getLimitHandler() {
		return Oracle12LimitHandler.INSTANCE;
	}

	@Override
	protected String getLimitClause() {
		Limit limit = getLimit();
		if ( hasFirstRow(limit) && hasMaxRows(limit) ) {
			return " offset ? rows fetch next ? rows only";
		}
		else if ( hasFirstRow(limit) ) {
			return " offset ? rows";
		}
		else {
			return " fetch first ? rows only";
		}
	}

	@Test
	public void testSqlWithSpace() {
		final String sql = "select  p.name from Person p where p.id = 1 for update";
		final String expected = "select * from (select  p.name from Person p where p.id = 1) where rownum<=? for update";

		assertGenerateExpectedSql(expected, sql);
	}

	@Test
	public void testSqlWithSpaceInsideQuotedString() {
		final String sql = "select p.name from Person p where p.name =  ' this is a  string with spaces  ' for update";
		final String expected = "select * from (select p.name from Person p where p.name =  ' this is a  string with spaces  ') where rownum<=? for update";

		assertGenerateExpectedSql(expected, sql);
	}

	@Test
	public void testSqlWithForUpdateInsideQuotedString() {
		final String sql = "select a.prop from A a where a.name =  'this is for update '";
		final String expected = "select a.prop from A a where a.name =  'this is for update ' fetch first ? rows only";

		assertGenerateExpectedSql(expected, sql);
	}

	@Test
	public void testSqlWithForUpdateInsideAndOutsideQuotedStringA() {
		final String sql = "select a.prop from A a where a.name =  'this is for update ' for update";
		final String expected = "select * from (select a.prop from A a where a.name =  'this is for update ') where rownum<=? for update";

		assertGenerateExpectedSql(expected, sql);
	}

}
