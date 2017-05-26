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
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * A function which takes no arguments
 *
 * @author Michi
 */
public class NoArgsSqmFunctionTemplate extends AbstractSelfRenderingFunctionTemplate implements SelfRenderingFunctionSupport {
	private String name;
	private final boolean needsParenthesis;

	/**
	 * Constructs a NoArgSQLFunction
	 *
	 * @param name The function name
	 * @param returnType The function return type
	 */
	public NoArgsSqmFunctionTemplate(String name, boolean needsParenthesis, AllowableFunctionReturnType returnType) {
		super( returnType, ArgumentsValidator.NO_ARGS );
		this.name = name;
		this.needsParenthesis = needsParenthesis;
	}

	public NoArgsSqmFunctionTemplate(String name, AllowableFunctionReturnType argumentType) {
		this( name, true, argumentType );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<Expression> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( name );
		if ( needsParenthesis ) {
			sqlAppender.appendSql( "()" );
		}
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return this;
	}
}
