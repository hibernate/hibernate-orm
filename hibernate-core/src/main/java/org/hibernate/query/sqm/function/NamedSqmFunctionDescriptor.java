/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.Locale;

import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
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
public class NamedSqmFunctionDescriptor extends AbstractSqmFunctionDescriptor {
	private final String functionKey;
	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;

	public NamedSqmFunctionDescriptor(
			String functionKey,
			String functionName,
			boolean useParenthesesWhenNoArgs,
			ArgumentsValidator argumentsValidator) {
		super( argumentsValidator );
		this.functionKey = functionKey;

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
	}

	public String getFunctionKey() {
		return functionKey;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	protected FunctionRenderingSupport getRenderingSupport() {
		return (sqlAppender, functionName1, sqlAstArguments, walker, sessionFactory) -> {
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
		};
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
