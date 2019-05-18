/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;

/**
 * @author Gavin King
 */
public class TeradataTimestampaddEmulation extends IntervalTimestampaddEmulation {
	@Override
	protected void renderInterval(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			ExtractUnit field,
			Expression magnitude) {
		boolean literal = magnitude instanceof QueryLiteral;
		String fieldName = field.getName();
		switch ( fieldName ) {
			case "quarter":
				sqlAppender.appendSql("(");
				magnitude.accept(walker);
				sqlAppender.appendSql(") * interval '3' month");
				break;
			case "week":
				sqlAppender.appendSql("(");
				magnitude.accept(walker);
				sqlAppender.appendSql(") * interval '7' day");
				break;
			default:
				if (literal) {
					sqlAppender.appendSql("interval '");
					magnitude.accept(walker);
					sqlAppender.appendSql("'");
				}
				else {
					sqlAppender.appendSql("(");
					magnitude.accept(walker);
					sqlAppender.appendSql(") * interval '1'");
				}
				sqlAppender.appendSql(" ");
				sqlAppender.appendSql( fieldName );
		}
	}
}
