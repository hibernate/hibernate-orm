/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.sqm.produce.spi.criteria.select.JpaSelection;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface JpaExpression<T> extends Expression<T>, JpaSelection<T> {
	ExpressableType getExpressionType();

	SqmExpression visitExpression(CriteriaVisitor visitor);

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toLong}
	 *
	 * @return <tt>this</tt> but as a long
	 */
	JpaExpression<Long> asLong();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toInteger}
	 *
	 * @return <tt>this</tt> but as an integer
	 */
	JpaExpression<Integer> asInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toFloat}
	 *
	 * @return <tt>this</tt> but as a float
	 */
	JpaExpression<Float> asFloat();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toDouble}
	 *
	 * @return <tt>this</tt> but as a double
	 */
	JpaExpression<Double> asDouble();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigDecimal}
	 *
	 * @return <tt>this</tt> but as a {@link BigDecimal}
	 */
	JpaExpression<BigDecimal> asBigDecimal();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigInteger}
	 *
	 * @return <tt>this</tt> but as a {@link BigInteger}
	 */
	JpaExpression<BigInteger> asBigInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toString}
	 *
	 * @return <tt>this</tt> but as a string
	 */
	JpaExpression<String> asString();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variant returns

	@Override
	JpaPredicate isNull();

	@Override
	JpaPredicate isNotNull();

	@Override
	JpaPredicate in(Object... values);

	@Override
	JpaPredicate in(Expression<?>[] values);

	@Override
	JpaPredicate in(Collection<?> values);

	@Override
	JpaPredicate in(Expression<Collection<?>> values);

	@Override
	<X> JpaExpression<X> as(Class<X> type);

	@Override
	JpaSelection<T> alias(String name);
}
