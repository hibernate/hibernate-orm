/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.TemporalUnit;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;

import java.util.function.UnaryOperator;

import static org.hibernate.query.UnaryArithmeticOperator.UNARY_MINUS;

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
		TemporalUnit unit = field.getUnit();
		switch ( unit ) {
			case QUARTER:
				sqlAppender.appendSql("(");
				magnitude.accept(walker);
				sqlAppender.appendSql(") * interval '3 month'");
				break;
			case WEEK:
				sqlAppender.appendSql("(");
				magnitude.accept(walker);
				sqlAppender.appendSql(") * interval '7 day'");
				break;
			default:
				if (literal) {
					sqlAppender.appendSql("interval '");
					magnitude.accept(walker);
				}
				else {
					sqlAppender.appendSql("(");
					magnitude.accept(walker);
					sqlAppender.appendSql(") * interval '1");
				}
				sqlAppender.appendSql(" ");
				sqlAppender.appendSql( unit.toString() );
				sqlAppender.appendSql("'");
		}
	}
}
