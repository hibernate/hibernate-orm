/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.persistence.criteria.Expression;

/**
 * TODO : javadoc
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
