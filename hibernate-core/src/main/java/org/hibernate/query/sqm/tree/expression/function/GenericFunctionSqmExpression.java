/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.List;

import org.hibernate.persister.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class GenericFunctionSqmExpression extends AbstractFunctionSqmExpression implements SqmExpression {
	private final String functionName;
	private final List<SqmExpression> arguments;

	public GenericFunctionSqmExpression(
			String functionName,
			BasicValuedExpressableType resultType,
			List<SqmExpression> arguments) {
		super( resultType );
		this.functionName = functionName;
		this.arguments = arguments;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	public boolean hasArguments() {
		return arguments != null && !arguments.isEmpty();
	}

	public List<SqmExpression> getArguments() {
		return arguments;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitGenericFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "function(" + getFunctionName() + " ...)";
	}
}
