/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.ExpressionImplementor;
import org.hibernate.jpa.criteria.expression.function.CastFunction;

/**
 * Models an expression in the criteria query language.
 *
 * @author Steve Ebersole
 */
public abstract class ExpressionImpl<T>
		extends SelectionImpl<T>
		implements ExpressionImplementor<T>, Serializable {
	public ExpressionImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> Expression<X> as(Class<X> type) {
		return type.equals( getJavaType() )
				? (Expression<X>) this
				: new CastFunction<X, T>( criteriaBuilder(), type, this );
	}

	@Override
	public Predicate isNull() {
		return criteriaBuilder().isNull( this );
	}

	@Override
	public Predicate isNotNull() {
		return criteriaBuilder().isNotNull( this );
	}

	@Override
	public Predicate in(Object... values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public Predicate in(Expression<?>... values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public Predicate in(Collection<?> values) {
		return criteriaBuilder().in( this, values.toArray() );
	}

	@Override
	public Predicate in(Expression<Collection<?>> values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<Long> asLong() {
		resetJavaType( Long.class );
		return (ExpressionImplementor<Long>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<Integer> asInteger() {
		resetJavaType( Integer.class );
		return (ExpressionImplementor<Integer>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<Float> asFloat() {
		resetJavaType( Float.class );
		return (ExpressionImplementor<Float>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<Double> asDouble() {
		resetJavaType( Double.class );
		return (ExpressionImplementor<Double>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<BigDecimal> asBigDecimal() {
		resetJavaType( BigDecimal.class );
		return (ExpressionImplementor<BigDecimal>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<BigInteger> asBigInteger() {
		resetJavaType( BigInteger.class );
		return (ExpressionImplementor<BigInteger>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public ExpressionImplementor<String> asString() {
		resetJavaType( String.class );
		return (ExpressionImplementor<String>) this;
	}
}
