/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.TemporalUnit;
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
		TemporalUnit unit = field.getUnit();
		sqlAppender.appendSql("add_");
		switch (unit) {
			case MILLISECOND:
			case MICROSECOND:
				sqlAppender.appendSql("second");
				break;
			case WEEK:
				sqlAppender.appendSql("day");
				break;
			case QUARTER:
				sqlAppender.appendSql("month");
				break;
			default:
				sqlAppender.appendSql( unit.toString() );
		}
		sqlAppender.appendSql("s(");
		datetime.accept(walker);
		sqlAppender.appendSql(",");
		switch (unit) {
			case MILLISECOND:
			case MICROSECOND:
			case WEEK:
			case QUARTER:
				sqlAppender.appendSql("(");
				break;
		}
		magnitude.accept(walker);
		switch (unit) {
			case MILLISECOND:
				sqlAppender.appendSql(")/1e3");
				break;
			case MICROSECOND:
				sqlAppender.appendSql(")/1e6");
				break;
			case WEEK:
				sqlAppender.appendSql(")*7");
				break;
			case QUARTER:
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
