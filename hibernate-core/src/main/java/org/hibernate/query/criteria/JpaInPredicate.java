/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CriteriaBuilder;

/**
 * @author Steve Ebersole
 */
public interface JpaInPredicate<T> extends JpaPredicate, CriteriaBuilder.In<T>  {
	/**
	 * Return the expression to be tested against the
	 * list of values.
	 * @return expression
	 */
	JpaExpression<T> getExpression();

	/**
	 *  Add to list of values to be tested against.
	 *  @param value value
	 *  @return in predicate
	 */
	JpaInPredicate<T> value(T value);

	/**
	 *  Add to list of values to be tested against.
	 *  @param value expression
	 *  @return in predicate
	 */
	JpaInPredicate<T> value(JpaExpression<? extends T> value);
}
