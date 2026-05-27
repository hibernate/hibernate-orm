/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public interface SqmPredicate
		extends SqmVisitableNode, JpaPredicate, SqmBooleanExpression {
	@Override
	default @Nonnull JavaType<Boolean> getJavaTypeDescriptor(){
		return BooleanJavaType.INSTANCE;
	}

	@Override
	default @Nonnull JavaType<Boolean> getNodeJavaType() {
		return getNodeType().getExpressibleJavaType();
	}

	@Override
	default @Nonnull SqmBindableType<Boolean> getExpressible() {
		return getNodeType();
	}

	@Nonnull SqmBindableType<Boolean> getNodeType();

	@Nonnull
	@Override
	SqmPredicate not();

	@Override
	SqmPredicate copy(SqmCopyContext context);
}
