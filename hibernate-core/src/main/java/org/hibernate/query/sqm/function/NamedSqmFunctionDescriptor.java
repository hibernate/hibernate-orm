/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
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
public class NamedSqmFunctionDescriptor
		extends AbstractSqmSelfRenderingFunctionDescriptor {
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;
	private final String argumentListSignature;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		this(
				functionName,
				useParenthesesWhenNoArgs,
				argumentsValidator,
				returnTypeResolver,
				functionName,
				null,
				SqlAstNodeRenderingMode.DEFAULT
		);
	}

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String name,
			String argumentListSignature,
			SqlAstNodeRenderingMode argumentRenderingMode) {
		super( name, argumentsValidator, returnTypeResolver );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
		this.argumentListSignature = argumentListSignature;
		this.argumentRenderingMode = argumentRenderingMode;
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
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
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
			walker.render( sqlAstArgument, argumentRenderingMode );
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
