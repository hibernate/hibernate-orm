/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionTemplate extends AbstractSelfRenderingFunctionTemplate {
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;

	public NamedSqmFunctionTemplate(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( returnTypeResolver, argumentsValidator );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return functionName;
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new RenderingSupport();
	}

	private class RenderingSupport implements SelfRenderingFunctionSupport {

		RenderingSupport() {
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<Expression> sqlAstArguments,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			final boolean useParens = useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty();

			sqlAppender.appendSql( functionName );
			if ( useParens ) {
				sqlAppender.appendSql( "(" );
			}

			boolean firstPass = true;
			for ( Expression sqlAstArgument : sqlAstArguments ) {
				if ( !firstPass ) {
					sqlAppender.appendSql( ", " );
				}
				sqlAstArgument.accept( walker );
				firstPass = false;
			}

			if ( useParens ) {
				sqlAppender.appendSql( ")" );
			}
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"NamedSqmFunctionTemplate(%s)",
				functionName
		);
	}

	public static class Builder {
	}
}
