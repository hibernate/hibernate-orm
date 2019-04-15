/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFunction<T> extends AbstractSqmExpression<T> implements SqmFunction<T> {
	public AbstractSqmFunction(AllowableFunctionReturnType<T> resultType, NodeBuilder nodeBuilder) {
		super( resultType, nodeBuilder );
	}

	@Override
	public AllowableFunctionReturnType<T> getExpressableType() {
		return (AllowableFunctionReturnType<T>) super.getExpressableType();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public boolean isAggregator() {
		return false;
	}
}
