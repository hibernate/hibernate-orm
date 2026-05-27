/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.annotation.Nullable;

/**
 * @author Steve Ebersole
 */
public interface JpaCriteriaBase extends CommonAbstractCriteria, JpaCriteriaNode {
	@Nonnull
	@Override
	<U> JpaSubQuery<U> subquery(@Nonnull Class<U> type);

	@Nullable
	@Override
	JpaPredicate getRestriction();
}
