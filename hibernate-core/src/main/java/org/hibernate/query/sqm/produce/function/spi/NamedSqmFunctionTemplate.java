/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

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
	private final String argumentListSignature;

	public NamedSqmFunctionTemplate(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this( functionName, useParenthesesWhenNoArgs, argumentsValidator, returnTypeResolver, functionName, null );
	}

	public NamedSqmFunctionTemplate(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String name,
			String argumentListSignature) {
		super( name, returnTypeResolver, argumentsValidator );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		this.argumentListSignature = argumentListSignature;
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
	public String getArgumentListSignature() {
		return argumentListSignature==null ? super.getArgumentListSignature() : argumentListSignature;
	}

	@Override
	public boolean alwaysIncludesParentheses() {
		return useParenthesesWhenNoArgs;
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
			SqlAstWalker walker) {
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
			sqlAstArgument.accept(walker);
			firstPass = false;
		}

		if ( useParens ) {
			sqlAppender.appendSql( ")" );
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

}
