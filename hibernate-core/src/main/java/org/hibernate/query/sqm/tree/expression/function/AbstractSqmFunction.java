/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunction implements SqmFunction {
	private final AllowableFunctionReturnType resultType;

	public AbstractSqmFunction(AllowableFunctionReturnType resultType) {
		this.resultType = resultType;
	}

	@Override
	public AllowableFunctionReturnType getExpressableType() {
		return resultType;
	}

	@Override
	public AllowableFunctionReturnType getInferableType() {
		return getExpressableType();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
