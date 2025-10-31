/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public interface SqmPredicate
		extends SqmVisitableNode, JpaPredicate, SqmExpression<Boolean> {
	@Override
	default @NonNull JavaType<Boolean> getJavaTypeDescriptor(){
		return BooleanJavaType.INSTANCE;
	}

	@Override
	default @NonNull JavaType<Boolean> getNodeJavaType() {
		return getNodeType().getExpressibleJavaType();
	}

	@Override
	default @NonNull SqmBindableType<Boolean> getExpressible() {
		return getNodeType();
	}

	@NonNull SqmBindableType<Boolean> getNodeType();

	@Override
	SqmPredicate not();

	@Override
	SqmPredicate copy(SqmCopyContext context);
}
