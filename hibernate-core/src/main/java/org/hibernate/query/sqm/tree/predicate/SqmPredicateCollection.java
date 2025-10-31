/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

/**
 * A grouping of predicates, such as a where-clause, join restriction, ...
 *
 * @author Steve Ebersole
 */
public interface SqmPredicateCollection {
	@Nullable SqmPredicate getPredicate();

	void setPredicate(@Nullable SqmPredicate predicate);

	void applyPredicate(SqmPredicate predicate);

	void applyPredicates(SqmPredicate... predicates);

	void applyPredicates(Collection<SqmPredicate> predicates);
}
