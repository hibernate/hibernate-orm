/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.QueryException;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.spi.AbstractSelfRenderingFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * A Cach&eacute; defintion of a convert function.
 *
 * @author Jonathan Levinson
 */
public class ConvertFunctionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {
	public ConvertFunctionTemplate() {
		super( StandardArgumentsValidators.between( 2, 3 ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		final int argCount = sqlAstArguments.size();

		if ( argCount == 2 ) {
			sqlAppender.appendSql( "{fn convert(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ", " );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( ")}" );
		}
		else if ( argCount != 3 ) {
			sqlAppender.appendSql( "convert(" );
			sqlAstArguments.get( 0 ).accept( walker );
			sqlAppender.appendSql( ", " );
			sqlAstArguments.get( 1 ).accept( walker );
			sqlAppender.appendSql( "," );
			sqlAstArguments.get( 2 ).accept( walker );
			sqlAppender.appendSql( ")" );
		}
		else {
			throw new QueryException( "convert() requires two or three arguments" );
		}
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		final int argCount = arguments.size();
		if ( argCount < 2 || argCount > 3 ) {
			throw new QueryException( "convert() requires two or three arguments" );
		}

		return this;
	}
}
