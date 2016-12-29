/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;

import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;

/**
 * @author Steve Ebersole
 */
public interface PredicateImplementor extends Predicate {
	/**
	 * Access to the CriteriaBuilder
	 *
	 * @return The CriteriaBuilder
	 */
	public CriteriaBuilderImpl criteriaBuilder();

	/**
	 * Is this a conjunction or disjunction?
	 *
	 * @return {@code true} if this predicate is a junction (AND/OR); {@code false} otherwise
	 */
	public boolean isJunction();
}
