/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionDescriptor extends AbstractSqmFunctionDescriptor implements FunctionRenderingSupport {
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;

	public NamedSqmFunctionDescriptor(
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver) {
		super( argumentsValidator, returnTypeResolver );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	protected String resolveFunctionName(String functionName) {
		return this.functionName;
	}

	@Override
	public FunctionRenderingSupport getRenderingSupport() {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			String functionName,
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
			sqlAstArgument.accept( walker );
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
