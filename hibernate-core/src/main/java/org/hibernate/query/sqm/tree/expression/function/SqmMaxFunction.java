/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmMaxFunction
		extends AbstractSqmAggregateFunction
		implements SqmAggregateFunction {
	public static final String NAME = "max";

	public SqmMaxFunction(SqmExpression argument) {
		super( argument, (AllowableFunctionReturnType) argument.getExpressableType() );
	}

	public SqmMaxFunction(SqmExpression argument, BasicValuedExpressableType resultType) {
		super( argument, resultType );
	}

	@Override
	public String getFunctionName() {
		return NAME;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMaxFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "MAX(" + getArgument().asLoggableText() + ")";
	}
}
