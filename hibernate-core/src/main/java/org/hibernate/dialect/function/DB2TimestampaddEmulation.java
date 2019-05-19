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
 * Uses the appropriate add{unit}s() functions to emulate
 * timestampadd() on DB2.
 *
 * @author Gavin King
 */
public class DB2TimestampaddEmulation
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	public DB2TimestampaddEmulation() {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		ExtractUnit field = (ExtractUnit) arguments.get(0);
		Expression magnitude = (Expression) arguments.get(1);
		Expression datetime = (Expression) arguments.get(2);
		String fieldName = field.getName();
		sqlAppender.appendSql("add_");
		switch (fieldName) {
			case "millisecond":
			case "microsecond":
				sqlAppender.appendSql("second");
				break;
			case "week":
				sqlAppender.appendSql("day");
				break;
			case "quarter":
				sqlAppender.appendSql("month");
				break;
			default:
				sqlAppender.appendSql( fieldName );
		}
		sqlAppender.appendSql("s(");
		datetime.accept(walker);
		sqlAppender.appendSql(",");
		switch (fieldName) {
			case "millisecond":
			case "microsecond":
			case "week":
			case "quarter":
				sqlAppender.appendSql("(");
				break;
		}
		magnitude.accept(walker);
		switch (fieldName) {
			case "millisecond":
				sqlAppender.appendSql(")/1e3");
				break;
			case "microsecond":
				sqlAppender.appendSql(")/1e6");
				break;
			case "week":
				sqlAppender.appendSql(")*7");
				break;
			case "quarter":
				sqlAppender.appendSql(")*3");
				break;
		}
		sqlAppender.appendSql(")");
	}

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
