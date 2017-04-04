/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.criteria.Expression;

/**
 * Internal contract for implementations of the JPA {@link Expression} contract.
 *
 * @author Steve Ebersole
 */
public interface ExpressionImplementor<T> extends SelectionImplementor<T>, Expression<T>, Renderable {
	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toLong}
	 *
	 * @return <tt>this</tt> but as a long
	 */
	public ExpressionImplementor<Long> asLong();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toInteger}
	 *
	 * @return <tt>this</tt> but as an integer
	 */
	public ExpressionImplementor<Integer> asInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toFloat}
	 *
	 * @return <tt>this</tt> but as a float
	 */
	public ExpressionImplementor<Float> asFloat();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toDouble}
	 *
	 * @return <tt>this</tt> but as a double
	 */
	public ExpressionImplementor<Double> asDouble();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigDecimal}
	 *
	 * @return <tt>this</tt> but as a {@link BigDecimal}
	 */
	public ExpressionImplementor<BigDecimal> asBigDecimal();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toBigInteger}
	 *
	 * @return <tt>this</tt> but as a {@link BigInteger}
	 */
	public ExpressionImplementor<BigInteger> asBigInteger();

	/**
	 * See {@link javax.persistence.criteria.CriteriaBuilder#toString}
	 *
	 * @return <tt>this</tt> but as a string
	 */
	public ExpressionImplementor<String> asString();
}
