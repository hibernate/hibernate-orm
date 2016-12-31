/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.sqm.parser.criteria.tree.JpaPredicate;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.Predicate}
 * contract.
 *
 * @author Steve Ebersole
 */
public interface JpaPredicateImplementor extends JpaPredicate, JpaExpressionImplementor<Boolean> {
	/**
	 * Access to the CriteriaBuilder
	 *
	 * @return The CriteriaBuilder
	 */
	HibernateCriteriaBuilder criteriaBuilder();

	@Override
	JpaPredicateImplementor not();

	/**
	 * Is this a conjunction or disjunction?
	 *
	 * @return {@code true} if this predicate is a junction (AND/OR); {@code false} otherwise
	 */
	boolean isJunction();
}
