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
public class PostgresTimestampaddEmulation extends IntervalTimestampaddEmulation {
	@Override
	protected void renderInterval(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			ExtractUnit field,
			Expression magnitude) {
		boolean literal = magnitude instanceof QueryLiteral;
		if (!literal) {
			sqlAppender.appendSql("(");
		}
		magnitude.accept(walker);
		if (!literal) {
			sqlAppender.appendSql(")");
		}
		sqlAppender.appendSql(" * interval '1 ");
		sqlAppender.appendSql(field.getName());
		sqlAppender.appendSql("'");
	}
}
