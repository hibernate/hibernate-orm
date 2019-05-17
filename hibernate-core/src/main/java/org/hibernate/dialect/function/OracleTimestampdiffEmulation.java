/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Gavin King
 */
public class OracleTimestampdiffEmulation extends IntervalTimestampdiffEmulation {

	@Override
	void renderSeconds(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			String fieldName) {
		switch (fieldName) {
			case "microsecond":
				sqlAppender.appendSql("1e6*");
				break;
			case "millisecond":
				sqlAppender.appendSql("1e3*");
				break;
		}
		sqlAppender.appendSql("(");
		sqlAppender.appendSql("60*60*24*");
		extractField(sqlAppender, walker, datetime1, datetime2, "day");
		sqlAppender.appendSql("+60*60*");
		extractField(sqlAppender, walker, datetime1, datetime2, "hour");
		sqlAppender.appendSql("+60*");
		extractField(sqlAppender, walker, datetime1, datetime2, "minute");
		sqlAppender.appendSql("+");
		extractField(sqlAppender, walker, datetime1, datetime2, "second");
		sqlAppender.appendSql(")");
	}

	@Override
	void extractField(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			String fieldName) {
		sqlAppender.appendSql("extract(");
		sqlAppender.appendSql(fieldName);
		sqlAppender.appendSql(" from (");
		datetime2.accept(walker);
		sqlAppender.appendSql("-");
		datetime1.accept(walker);
		sqlAppender.appendSql(") ");
		switch (fieldName) {
			case "year":
			case "month":
				sqlAppender.appendSql("year to month");
				break;
			case "day":
			case "hour":
			case "minute":
			case "second":
				sqlAppender.appendSql("day to second");
				break;
			default:
				throw new SemanticException(fieldName + " is not a legal field");
		}
		sqlAppender.appendSql(")");
	}

}
