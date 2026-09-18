/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;

/**
 * @author Steve Ebersole
 */
public interface SqmExpressionWrapper<T> extends SqmExpression<T> {
	@Nonnull
	SqmExpression<T> getWrappedExpression();
}
