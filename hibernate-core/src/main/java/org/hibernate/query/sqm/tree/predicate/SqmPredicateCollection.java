/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.predicate;

import java.util.Collection;

/**
 * A grouping of predicates, such as a where-clause, join restriction, ...
 *
 * @author Steve Ebersole
 */
public interface SqmPredicateCollection {
	SqmPredicate getPredicate();

	void setPredicate(SqmPredicate predicate);

	void applyPredicate(SqmPredicate predicate);

	void applyPredicates(SqmPredicate... predicates);

	void applyPredicates(Collection<SqmPredicate> predicates);
}
