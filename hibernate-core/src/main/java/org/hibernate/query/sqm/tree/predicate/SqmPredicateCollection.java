/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
