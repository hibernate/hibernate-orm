/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

/**
 * @author Steve Ebersole
 */
public interface SqmExpressionWrapper<T> extends SqmExpression<T> {
	SqmExpression<T> getWrappedExpression();
}
