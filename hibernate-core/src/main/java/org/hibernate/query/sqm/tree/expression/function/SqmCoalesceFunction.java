/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmCoalesceFunction implements SqmFunction {
	public static final String NAME = "coalesce";

	private AllowableFunctionReturnType resultType;
	private List<SqmExpression> arguments = new ArrayList<>();

	public SqmCoalesceFunction() {
	}

	@Override
	public AllowableFunctionReturnType getExpressableType() {
		return resultType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		if ( getExpressableType() == null ) {
			return null;
		}

		return getExpressableType().getJavaTypeDescriptor();
	}

	public List<SqmExpression> getArguments() {
		return arguments;
	}

	public void value(SqmExpression expression) {
		arguments.add( expression );

		if ( resultType == null ) {
			resultType = (AllowableFunctionReturnType) expression.getExpressableType();
		}
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCoalesceFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "coalesce(...)";
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public boolean hasArguments() {
		return true;
	}
}
