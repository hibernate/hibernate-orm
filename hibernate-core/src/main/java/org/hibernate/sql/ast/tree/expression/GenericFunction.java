/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * Represents a call to a function other than one of the standardized ones.
 *
 * @author Steve Ebersole
 */
public class GenericFunction extends AbstractFunction {
	private final String functionName;
	private final List<Expression> arguments;
	private final SqlExpressableType resultType;

	public GenericFunction(
			String functionName,
			SqlExpressableType resultType,
			List<Expression> arguments) {
		this.functionName = functionName;
		this.arguments = arguments;
		this.resultType = resultType;
	}

	public String getFunctionName() {
		return functionName;
	}

	public List<Expression> getArguments() {
		return arguments;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return resultType;
	}

	@Override
	public SqlExpressableType getType() {
		return resultType;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitNonStandardFunctionExpression( this );
	}

}
