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
public class OracleTimestampdiffEmulation extends IntervalTimestampdiffEmulation {

	@Override
	void renderSeconds(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			TemporalUnit unit) {
		switch (unit) {
			case MICROSECOND:
				sqlAppender.appendSql("1e6*");
				break;
			case MILLISECOND:
				sqlAppender.appendSql("1e3*");
				break;
		}
		sqlAppender.appendSql("(");
		sqlAppender.appendSql("60*60*24*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.DAY);
		sqlAppender.appendSql("+60*60*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.HOUR);
		sqlAppender.appendSql("+60*");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.MINUTE);
		sqlAppender.appendSql("+");
		extractField(sqlAppender, walker, datetime1, datetime2, TemporalUnit.SECOND);
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
		sqlAppender.appendSql(" from (");
		datetime2.accept(walker);
		sqlAppender.appendSql("-");
		datetime1.accept(walker);
		sqlAppender.appendSql(") ");
		switch (unit) {
			case YEAR:
			case MONTH:
				sqlAppender.appendSql("year to month");
				break;
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
				sqlAppender.appendSql("day to second");
				break;
			default:
				throw new SemanticException(unit + " is not a legal field");
		}
		sqlAppender.appendSql(")");
	}

}
