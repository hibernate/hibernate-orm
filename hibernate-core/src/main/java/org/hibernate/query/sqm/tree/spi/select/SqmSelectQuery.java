/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.select;

import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaSelectCriteria;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmNode;
import org.hibernate.query.sqm.tree.spi.SqmQuery;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteContainer;

/**
 * Common contract between a {@linkplain SqmSelectStatement root} and a
 * {@link SqmSubQuery sub-query}
 *
 * @author Steve Ebersole
 */
public interface SqmSelectQuery<T> extends SqmQuery<T>, JpaSelectCriteria<T>, SqmNode, SqmCteContainer {
	@Nonnull
	@Override
	SqmQuerySpec<T> getQuerySpec();

	@Nonnull
	SqmQueryPart<T> getQueryPart();

	@Nonnull
	@Override
	SqmSelectQuery<T> distinct(boolean distinct);

	@Override
	SqmSelectQuery<T> copy(SqmCopyContext context);
}
