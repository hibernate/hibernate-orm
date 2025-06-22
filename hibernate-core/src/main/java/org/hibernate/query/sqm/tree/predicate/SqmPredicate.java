/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.criteria.JpaPredicate;
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
	default JavaType<Boolean> getJavaTypeDescriptor(){
		return BooleanJavaType.INSTANCE;
	}

	@Override
	SqmPredicate not();

	@Override
	SqmPredicate copy(SqmCopyContext context);
}
