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
import org.hibernate.sql.ast.tree.expression.ExtractUnit;

/**
 * @author Gavin King
 */
public class OracleTimestampaddEmulation extends IntervalTimestampaddEmulation {
	@Override
	protected void renderInterval(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			ExtractUnit field,
			Expression magnitude) {
		TemporalUnit unit = field.getUnit();
		switch ( unit ) {
			case YEAR:
			case QUARTER:
			case MONTH:
				sqlAppender.appendSql("numtoyminterval");
				break;
			case WEEK:
			case DAY:
			case HOUR:
			case MINUTE:
			case SECOND:
			case MILLISECOND:
			case MICROSECOND:
				sqlAppender.appendSql("numtodsinterval");
				break;
			default:
				throw new SemanticException(unit + " is not a legal field");
		}
		sqlAppender.appendSql("(");
		switch ( unit ) {
			case QUARTER:
				sqlAppender.appendSql("3*(");
				break;
			case WEEK:
				sqlAppender.appendSql("7*(");
				break;
			case MILLISECOND:
				sqlAppender.appendSql("1e-3*(");
				break;
			case MICROSECOND:
				sqlAppender.appendSql("1e-6*(");
				break;
		}
		magnitude.accept(walker);
		switch ( unit ) {
			case MILLISECOND:
			case MICROSECOND:
			case QUARTER:
			case WEEK:
				sqlAppender.appendSql(")");
				break;
		}
		sqlAppender.appendSql(",'");
		switch ( unit ) {
			case QUARTER:
				sqlAppender.appendSql("month");
				break;
			case WEEK:
				sqlAppender.appendSql("day");
				break;
			case MILLISECOND:
			case MICROSECOND:
				sqlAppender.appendSql("second");
				break;
			default:
				sqlAppender.appendSql( unit.toString() );
		}
		sqlAppender.appendSql("')");
	}
}
