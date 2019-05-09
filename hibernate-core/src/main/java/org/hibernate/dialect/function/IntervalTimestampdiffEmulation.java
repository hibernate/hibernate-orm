/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.spi.AbstractSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import java.util.List;

/**
 * @author Gavin King
 */
abstract class IntervalTimestampdiffEmulation
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	IntervalTimestampdiffEmulation() {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardSpiBasicTypes.INTEGER )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		ExtractUnit field = (ExtractUnit) arguments.get(0);
		Expression datetime1 = (Expression) arguments.get(1);
		Expression datetime2 = (Expression) arguments.get(2);
		//TODO: does not handle 'millisecond' / 'microsecond'
		String fieldName = field.getName();
		switch (fieldName) {
			case "year":
				extractField(sqlAppender, walker, datetime1, datetime2, "year");
				break;
			case "month":
				sqlAppender.appendSql("(");
				sqlAppender.appendSql("12*");
				extractField(sqlAppender, walker, datetime1, datetime2, "year");
				sqlAppender.appendSql("+");
				extractField(sqlAppender, walker, datetime1, datetime2, "month");
				sqlAppender.appendSql(")");
				break;
			case "day":
				extractField(sqlAppender, walker, datetime1, datetime2, "day");
				break;
			case "hour":
				sqlAppender.appendSql("(");
				sqlAppender.appendSql("24*");
				extractField(sqlAppender, walker, datetime1, datetime2, "day");
				sqlAppender.appendSql("+");
				extractField(sqlAppender, walker, datetime1, datetime2, "hour");
				sqlAppender.appendSql(")");
				break;
			case "minute":
				sqlAppender.appendSql("(");
				sqlAppender.appendSql("60*24*");
				extractField(sqlAppender, walker, datetime1, datetime2, "day");
				sqlAppender.appendSql("+60*");
				extractField(sqlAppender, walker, datetime1, datetime2, "hour");
				sqlAppender.appendSql("+");
				extractField(sqlAppender, walker, datetime1, datetime2, "minute");
				sqlAppender.appendSql(")");
				break;
			case "second":
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
				break;
		}
	}

	abstract void extractField(
			SqlAppender sqlAppender,
			SqlAstWalker walker,
			Expression datetime1,
			Expression datetime2,
			String fieldName);

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return new SelfRenderingSqmFunction<T>(
				this,
				arguments,
				impliedResultType,
				queryEngine.getCriteriaBuilder(),
				"timestampdiff"
		);
	}

}
