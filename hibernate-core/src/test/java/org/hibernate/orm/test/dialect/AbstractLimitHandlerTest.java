/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.query.spi.Limit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yanming Zhou
 */
public abstract class AbstractLimitHandlerTest {

	@Test
	public void testSqlWithSemicolonInsideQuotedString() {
		String sql = "select * from Person p where p.name like ';'";
		String expected = "select * from Person p where p.name like ';'" + getLimitClause();
		assertGenerateExpectedSql(expected, sql);

		sql = "select * from Person p where p.name like ';' ";
		expected = "select * from Person p where p.name like ';'" + getLimitClause() + " ";
		assertGenerateExpectedSql(expected, sql);
	}

	@Test
	public void testSqlWithSemicolonInsideQuotedStringAndEndsWithSemicolon() {
		String sql = "select * from Person p where p.name like ';';";
		String expected = "select * from Person p where p.name like ';'" + getLimitClause() + ";";
		assertGenerateExpectedSql(expected, sql);

		sql = "select * from Person p where p.name like ';' ; ";
		expected = "select * from Person p where p.name like ';'" + getLimitClause() + " ; ";
		assertGenerateExpectedSql(expected, sql);
	}

	protected void assertGenerateExpectedSql(String expected, String sql) {
		final Limit limit = getLimit();
		assertEquals(
				expected,
				getLimitHandler().processSql(
						new PaginationRequest( sql, limit.getFirstRow(), limit.getMaxRows(), 0, null )
				).sql()
		);
	}

	protected abstract LimitHandler getLimitHandler();

	protected Limit getLimit() {
		return new Limit(null, 10);
	}

	protected String getLimitClause() {
		LimitHandler handler = getLimitHandler();
		if (handler instanceof OffsetFetchLimitHandler) {
			OffsetFetchLimitHandler oflh = (OffsetFetchLimitHandler) handler;
			Limit limit = getLimit();
			if ( limit.getFirstRow() != null && limit.getMaxRows() != null ) {
				return " offset " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getFirstRow()))
						+ " rows fetch next " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getMaxRows())) + " rows only";
			}
			else if ( limit.getFirstRow() != null ) {
				return " offset " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getFirstRow())) + " rows";
			}
			else if ( limit.getMaxRows() != null ) {
				return " fetch first " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getMaxRows())) + " rows only";
			}
			else {
				return "";
			}
		}
		return " limit ?";
	}

}
