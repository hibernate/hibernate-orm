/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Predicate;

/**
 * Basic template support for {@link Predicate} implementors providing
 * expression handling, negation and conjunction/disjunction handling.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPredicate
		extends AbstractExpression<Boolean>
		implements PredicateImplementor {

	protected AbstractPredicate(CriteriaNodeBuilder criteriaBuilder) {
		super( Boolean.class, criteriaBuilder );
	}

	public boolean isNegated() {
		return false;
	}

	public PredicateImplementor not() {
		return new NegatedPredicateWrapper( this );
	}

}
