/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;
import java.util.Locale;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;

	public NamedSqmFunctionTemplate(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this( functionName, useParenthesesWhenNoArgs, argumentsValidator, returnTypeResolver, functionName );
	}

	public NamedSqmFunctionTemplate(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String name) {
		super( name, returnTypeResolver, argumentsValidator );

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
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<?> impliedResultType,
			QueryEngine queryEngine) {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		final boolean useParens = useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty();

		sqlAppender.appendSql( functionName );
		if ( useParens ) {
			sqlAppender.appendSql( "(" );
		}

		boolean firstPass = true;
		for ( SqlAstNode sqlAstArgument : sqlAstArguments ) {
			if ( !firstPass ) {
				sqlAppender.appendSql( ", " );
			}
			renderArgument( sqlAppender, sqlAstArgument, walker, sessionFactory );
			firstPass = false;
		}

		if ( useParens ) {
			sqlAppender.appendSql( ")" );
		}
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
			SqlAstNode sqlAstArgument,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAstArgument.accept( walker );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"NamedSqmFunctionTemplate(%s)",
				functionName
		);
	}

}
