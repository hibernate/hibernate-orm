/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.Dialect;
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
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.type.spi.TypeConfiguration.isTimestampType;

/**
 * @author Gavin King
 */
public class TimestampaddFunction
		extends AbstractSqmFunctionTemplate implements SelfRenderingFunctionSupport {

	private Dialect dialect;

	public TimestampaddFunction(Dialect dialect) {
		super(
				StandardArgumentsValidators.exactly( 3 ),
				StandardFunctionReturnTypeResolvers.useArgType( 3 )
		);
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		ExtractUnit field = (ExtractUnit) arguments.get(0);
		Expression magnitude = (Expression) arguments.get(1);
		Expression to = (Expression) arguments.get(2);
		TemporalUnit unit = field.getUnit();
		dialect.timestampadd(
				unit,
				() -> magnitude.accept( walker ),
				() -> to.accept( walker ),
				sqlAppender::appendSql,
				isTimestampType( to.getType() )
		);
	}

	@Override
	protected <T> SelfRenderingSqmFunction<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqmFunction<>(
				this,
				arguments,
				impliedResultType,
				queryEngine.getCriteriaBuilder(),
				"timestampdiff"
		);
	}

}
