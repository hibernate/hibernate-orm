/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.TemporalUnit;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Gavin King
 */
public class PostgresTimestampdiffEmulation extends IntervalTimestampdiffEmulation {

	@Override
	void renderSeconds(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			TemporalUnit unit) {
		sqlAppender.appendSql("(");
		switch (unit) {
			case MILLISECOND:
				sqlAppender.appendSql("1e3*(");
				break;
			case MICROSECOND:
				sqlAppender.appendSql("1e6*(");
				break;
		}
		sqlAppender.appendSql("60*60*24*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.DAY);
		sqlAppender.appendSql("+60*60*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.HOUR);
		sqlAppender.appendSql("+60*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.MINUTE);
		switch (unit) {
			case MILLISECOND:
			case MICROSECOND:
				sqlAppender.appendSql(")");
				break;
		}
		sqlAppender.appendSql("+");
		extractField(sqlAppender, walker, datetime1, datetime2, unit);
		sqlAppender.appendSql(")");
	}

	@Override
	void extractField(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			TemporalUnit unit) {
		sqlAppender.appendSql("extract(");
		sqlAppender.appendSql( unit.toString() );
		sqlAppender.appendSql(" from ");
		switch (unit) {
			case YEAR:
			case MONTH:
				sqlAppender.appendSql("age(");
				break;
		}
		datetime2.accept(walker);
		switch (unit) {
			case YEAR:
			case MONTH:
				sqlAppender.appendSql(",");
				break;
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
			case MILLISECOND:
			case MICROSECOND:
				sqlAppender.appendSql("-");
				break;
			default:
				throw new SemanticException(unit + " is not a legal field");
		}
		datetime1.accept(walker);
		switch (unit) {
			case YEAR:
			case MONTH:
				sqlAppender.appendSql(")");
				break;
		}
		sqlAppender.appendSql(")");
	}

}
