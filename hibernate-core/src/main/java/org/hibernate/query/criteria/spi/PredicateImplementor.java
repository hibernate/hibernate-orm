/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaPredicate;

/**
 * SPI-level contract for JpaPredicate
 *
 * @author Steve Ebersole
 */
public interface PredicateImplementor extends ExpressionImplementor<Boolean>, JpaPredicate {
	static BooleanOperator reverseOperator(BooleanOperator operator) {
		return operator == BooleanOperator.AND
				? BooleanOperator.OR
				: BooleanOperator.AND;
	}

	@Override
	PredicateImplementor not();
}
