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
		switch ( field.getName().toLowerCase() ) {
			case "year":
			case "month":
				sqlAppender.appendSql("numtoyminterval");
				break;
			case "day":
			case "hour":
			case "minute":
			case "second":
				sqlAppender.appendSql("numtodsinterval");
				break;
			default: throw new IllegalArgumentException(field.getName() + " is not a legal field");
		}
		sqlAppender.appendSql("(");
		magnitude.accept(walker);
		sqlAppender.appendSql(",'");
		sqlAppender.appendSql(field.getName());
		sqlAppender.appendSql("')");
	}
}
