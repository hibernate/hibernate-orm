/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

/**
 * API extension to the JPA {@link Expression} contract
 *
 * @author Steve Ebersole
 */
public interface JpaExpression<T> extends JpaSelection<T>, Expression<T> {
	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toLong(Expression)}.
	 *
	 * Returns the same instance with its internal type reset to Long
	 */
	JpaExpression<Long> asLong();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toInteger(Expression)}
	 *
	 * Returns the same instance with its internal type reset to Integer
	 */
	JpaExpression<Integer> asInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toFloat(Expression)}
	 *
	 * Returns the same instance with its internal type reset to Float
	 */
	JpaExpression<Float> asFloat();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toDouble(Expression)}
	 *
	 * Returns the same instance with its internal type reset to Double
	 */
	JpaExpression<Double> asDouble();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigDecimal(Expression)}
	 *
	 * Returns the same instance with its internal type reset to BigDecimal
	 */
	JpaExpression<BigDecimal> asBigDecimal();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigInteger(Expression)}
	 *
	 * Returns the same instance with its internal type reset to BigInteger
	 */
	JpaExpression<BigInteger> asBigInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toString(Expression)}
	 *
	 * Returns the same instance with its internal type reset to String
	 */
	JpaExpression<String> asString();

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec JPA explicitly says that this should return a "new expression object"
	 */
	@Override
	<X> JpaExpression<X> as(Class<X> type);

	@Override
	JpaPredicate isNull();

	@Override
	JpaPredicate isNotNull();

	@Override
	JpaPredicate in(Object... values);

	@Override
	JpaPredicate in(Expression<?>... values);

	@Override
	JpaPredicate in(Collection<?> values);

	@Override
	JpaPredicate in(Expression<Collection<?>> values);
}
