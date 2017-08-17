/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FunctionAsExpressionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {

	private static final Logger log = Logger.getLogger( FunctionAsExpressionTemplate.class );

	private final String expressionStart;
	private final String argumentSeparator;
	private final String expressionEnd;

	public FunctionAsExpressionTemplate(String expressionStart, String argumentSeparator, String expressionEnd) {
		this( expressionStart, argumentSeparator, expressionEnd, null );
	}

	public FunctionAsExpressionTemplate(
			String expressionStart,
			String argumentSeparator,
			String expressionEnd,
			FunctionReturnTypeResolver returnTypeResolver) {
		this( expressionStart, argumentSeparator, expressionEnd, returnTypeResolver, null );
	}

	public FunctionAsExpressionTemplate(
			String expressionStart,
			String argumentSeparator,
			String expressionEnd,
			FunctionReturnTypeResolver returnTypeResolver,
			ArgumentsValidator argumentsValidator) {
		super( returnTypeResolver, argumentsValidator );
		this.expressionStart = expressionStart;
		this.argumentSeparator = argumentSeparator;
		this.expressionEnd = expressionEnd;
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType resolvedReturnType) {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( expressionStart );

		if ( sqlAstArguments.isEmpty() ) {
			log.debugf( "No arguments found for FunctionAsExpressionTemplate, this is most likely a query syntax error" );
		}
		else {
			// render the first argument..
			sqlAstArguments.get( 0 ).accept( walker );
			renderArgument( sqlAppender, sqlAstArguments.get( 0 ), walker, sessionFactory );

			// render the rest of the arguments, preceded by the separator
			for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
				sqlAppender.appendSql( argumentSeparator );
				renderArgument( sqlAppender, sqlAstArguments.get( i ), walker, sessionFactory );
			}
		}

		sqlAppender.appendSql( expressionEnd );
	}

	/**
	 * Called from {@link #render} to render an argument.
	 *
	 * @param sqlAppender The sql appender to append the rendered argument.
	 * @param sqlAstArgument The argument being processed.
	 * @param walker The walker to use for rendering {@link SqlAstNode} expressions
	 * @param sessionFactory The session factory
	 */
	protected void renderArgument(
			SqlAppender sqlAppender,
			Expression sqlAstArgument,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAstArgument.accept( walker );
	}
}
