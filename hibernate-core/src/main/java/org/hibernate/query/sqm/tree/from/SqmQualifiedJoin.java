/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * Common contract for qualified/restricted/predicated joins.
 *
 * @author Steve Ebersole
 */
public interface SqmQualifiedJoin<O,T> extends SqmJoin<O,T> {
	/**
	 * Obtain the join predicate
	 *
	 * @return The join predicate
	 */
	SqmPredicate getJoinPredicate();

	/**
	 * Inject the join predicate
	 *
	 * @param predicate The join predicate
	 */
	void setJoinPredicate(SqmPredicate predicate);

	// todo : specialized Predicate for "mapped attribute join" conditions
}
