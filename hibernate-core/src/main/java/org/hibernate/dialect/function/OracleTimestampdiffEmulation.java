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

/**
 * @author Gavin King
 */
public class OracleTimestampdiffEmulation extends IntervalTimestampdiffEmulation {

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
		}
		sqlAppender.appendSql(")");
	}

}
