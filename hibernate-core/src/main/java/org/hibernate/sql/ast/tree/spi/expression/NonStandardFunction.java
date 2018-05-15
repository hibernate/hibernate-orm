/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.Arrays;
import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Represents a call to a function other than one of the standardized ones.
 *
 * @author Steve Ebersole
 */
public class NonStandardFunction extends AbstractFunction {
	private final String functionName;
	private final List<Expression> arguments;
	private final SqlExpressableType resultType;

	public NonStandardFunction(
			String functionName,
			SqlExpressableType resultType,
			List<Expression> arguments) {
		this.functionName = functionName;
		this.arguments = arguments;
		this.resultType = resultType;
	}

	public NonStandardFunction(
			String functionName,
			SqlExpressableType resultType,
			Expression... arguments) {
		this(
				functionName,
				resultType,
				Arrays.asList( arguments )
		);
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

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				getExpressableType()
		);
	}
}
