/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.query.criteria.JpaCriteriaBase;

/**
 * Commonality between a top-level statement and a sub-query
 *
 * @author Steve Ebersole
 */
public interface SqmQuery<T> extends JpaCriteriaBase, SqmNode {
	@Nonnull
	@Override
	SqmQuery<T> copy(@Nonnull SqmCopyContext context);

	@Nonnull
	String generateAlias();
}
