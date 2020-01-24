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
 * A {@link SqmFunctionDescriptor function descriptor} for functions
 * which produce SQL in the standard form {@code f(x, y, z)}.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class NamedSqmFunctionDescriptor
		extends AbstractSqmSelfRenderingFunctionDescriptor
		implements FunctionRenderingSupport {
	private final String functionName;
	private String sqlFunctionName;
	private final boolean requiresArguments;
	private final String argumentListSignature;

	public NamedSqmFunctionDescriptor(
			String functionName,
			String sqlFunctionName,
			boolean requiresArguments,
			ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			String argumentListSignature) {
		super( functionName, argumentsValidator, returnTypeResolver );

		this.functionName = functionName;
		this.sqlFunctionName = sqlFunctionName;
		this.requiresArguments = requiresArguments;
		this.argumentListSignature = argumentListSignature;
	}

	public String getFunctionName() {
		return functionName;
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
		final boolean useParens = requiresArguments || !sqlAstArguments.isEmpty();

		sqlAppender.appendSql( sqlFunctionName );
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
	public String getArgumentListSignature() {
		return argumentListSignature==null ? super.getArgumentListSignature() : argumentListSignature;
	}

	@Override
	public boolean requiresArgumentList() {
		return requiresArguments;
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
