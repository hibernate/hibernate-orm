/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.pagination.AbstractLimitHandler.hasFirstRow;
import static org.hibernate.dialect.pagination.AbstractLimitHandler.hasMaxRows;
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
		assertEquals(expected, getLimitHandler().processSql(sql, getLimit(), QueryOptions.NONE));
	}

	protected abstract LimitHandler getLimitHandler();

	protected Limit getLimit() {
		return new Limit(0, 10);
	}

	protected String getLimitClause() {
		LimitHandler handler = getLimitHandler();
		if (handler instanceof OffsetFetchLimitHandler) {
			OffsetFetchLimitHandler oflh = (OffsetFetchLimitHandler) handler;
			Limit limit = getLimit();
			if (hasFirstRow(limit) && hasMaxRows(limit)) {
				return " offset " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getFirstRow()))
						+ " rows fetch next " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getMaxRows())) + " rows only";
			}
			else if (hasFirstRow(limit)) {
				return " offset " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getFirstRow())) + " rows";
			} else {
				return " fetch first " + (oflh.supportsVariableLimit() ? "?" : String.valueOf(limit.getMaxRows())) + " rows only";
			}
		}
		return " limit ?";
	}
}
