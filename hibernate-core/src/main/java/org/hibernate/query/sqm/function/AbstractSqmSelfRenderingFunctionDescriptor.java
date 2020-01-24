package org.hibernate.query.sqm.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * @author Gavin King
 */
public abstract class AbstractSqmSelfRenderingFunctionDescriptor
		extends AbstractSqmFunctionDescriptor
		implements SqmSelfRenderingFunctionDescriptor {

	public AbstractSqmSelfRenderingFunctionDescriptor(String name, ArgumentsValidator argumentsValidator, FunctionReturnTypeResolver returnTypeResolver) {
		super( name, argumentsValidator, returnTypeResolver );
	}

	@Override
	protected <T> SelfRenderingSqlFunctionExpression<T> generateSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine,
			TypeConfiguration typeConfiguration) {
		return new SelfRenderingSqlFunctionExpression<T>(
				this,
				getRenderingSupport(),
				arguments,
				impliedResultType,
				getReturnTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		);
	}

}
