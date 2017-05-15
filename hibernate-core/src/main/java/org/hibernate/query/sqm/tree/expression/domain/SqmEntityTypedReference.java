/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface SqmEntityTypedReference extends SqmNavigableContainerReference {
	@Override
	EntityValuedExpressableType getReferencedNavigable();

	@Override
	EntityValuedExpressableType getExpressionType();

	@Override
	default String getTypeName() {
		return getExpressionType().getTypeName();
	}

	@Override
	default JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressionType().getJavaTypeDescriptor();
	}

	@Override
	default Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default EntityValuedExpressableType getInferableType() {
		return getExpressionType();
	}
}
