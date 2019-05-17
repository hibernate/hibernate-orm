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
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;

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
		String fieldName = field.getName().toLowerCase();
		switch ( fieldName ) {
			case "year":
			case "quarter":
			case "month":
				sqlAppender.appendSql("numtoyminterval");
				break;
			case "week":
			case "day":
			case "hour":
			case "minute":
			case "second":
			case "millisecond":
			case "microsecond":
				sqlAppender.appendSql("numtodsinterval");
				break;
			default:
				throw new SemanticException(field.getName() + " is not a legal field");
		}
		sqlAppender.appendSql("(");
		switch ( fieldName ) {
			case "quarter":
				sqlAppender.appendSql("3*(");
				break;
			case "week":
				sqlAppender.appendSql("7*(");
				break;
			case "millisecond":
				sqlAppender.appendSql("1e-3*(");
				break;
			case "microsecond":
				sqlAppender.appendSql("1e-6*(");
				break;
		}
		magnitude.accept(walker);
		switch ( fieldName ) {
			case "millisecond":
			case "microsecond":
			case "quarter":
			case "week":
				sqlAppender.appendSql(")");
				break;
		}
		sqlAppender.appendSql(",'");
		switch ( fieldName ) {
			case "quarter":
				sqlAppender.appendSql("month");
				break;
			case "week":
				sqlAppender.appendSql("day");
				break;
			case "millisecond":
			case "microsecond":
				sqlAppender.appendSql("second");
				break;
			default:
				sqlAppender.appendSql( fieldName );
		}
		sqlAppender.appendSql("')");
	}
}
