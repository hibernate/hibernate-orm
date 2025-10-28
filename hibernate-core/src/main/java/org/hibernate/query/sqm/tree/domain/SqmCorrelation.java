/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Specialization of {@link SqmFrom} for sub-query correlations
 *
 * @see org.hibernate.query.criteria.JpaSubQuery#correlate
 *
 * @param <L> The left-hand side of the correlation.  See {@linkplain #getCorrelatedRoot()}
 * @param <R> The right-hand side of the correlation, which is the type of this node.
 *
 * @author Steve Ebersole
 */
public interface SqmCorrelation<L,R> extends SqmFrom<L,R>, SqmPathWrapper<R,R> {
	SqmRoot<L> getCorrelatedRoot();

	@Override
	default SqmRoot<?> findRoot() {
		return getCorrelatedRoot();
	}
}
