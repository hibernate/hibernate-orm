/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Base support for {@link org.hibernate.query.criteria.JpaExpression} impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractExpression<T> extends AbstractSelection<T> implements ExpressionImplementor<T> {
	protected AbstractExpression(Class<T> javaType, CriteriaNodeBuilder criteriaBuilder) {
		super( javaType, criteriaBuilder );
	}

	protected AbstractExpression(JavaTypeDescriptor<T> javaTypeDescriptor, CriteriaNodeBuilder criteriaBuilder) {
		super( javaTypeDescriptor, criteriaBuilder );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> JpaExpression<X> as(Class<X> type) {
		return type.equals( getJavaType() )
				? (JpaExpression<X>) this
				: nodeBuilder().cast( this, type );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Long> asLong() {
		setJavaType( Long.class );
		return (JpaExpression<Long>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Integer> asInteger() {
		setJavaType( Integer.class );
		return (JpaExpression<Integer>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Float> asFloat() {
		setJavaType( Float.class );
		return (JpaExpression<Float>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Double> asDouble() {
		setJavaType( Double.class );
		return (JpaExpression<Double>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<BigDecimal> asBigDecimal() {
		setJavaType( BigDecimal.class );
		return (JpaExpression<BigDecimal>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<BigInteger> asBigInteger() {
		setJavaType( BigInteger.class );
		return (JpaExpression<BigInteger>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<String> asString() {
		setJavaType( String.class );
		return (JpaExpression<String>) this;
	}
}
