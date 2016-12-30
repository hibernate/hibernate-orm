/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpressionImplementor;
import org.hibernate.query.criteria.JpaInImplementor;
import org.hibernate.query.criteria.JpaPredicateImplementor;
import org.hibernate.query.criteria.internal.expression.function.CastFunction;
import org.hibernate.query.criteria.internal.selection.AbstractSimpleSelection;

/**
 * Models an expression in the criteria query language.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractExpression<T>
		extends AbstractSimpleSelection<T>
		implements JpaExpressionImplementor<T>, Serializable {
	public AbstractExpression(HibernateCriteriaBuilder criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> JpaExpressionImplementor<X> as(Class<X> type) {
		return type.equals( getJavaType() )
				? (JpaExpressionImplementor<X>) this
				: new CastFunction<>( criteriaBuilder(), type, this );
	}

	@Override
	public JpaPredicateImplementor isNull() {
		return criteriaBuilder().isNull( this );
	}

	@Override
	public JpaPredicateImplementor isNotNull() {
		return criteriaBuilder().isNotNull( this );
	}

	@Override
	public JpaInImplementor in(Object... values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public JpaPredicateImplementor in(Expression<?>... values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	public JpaPredicateImplementor in(Collection<?> values) {
		return criteriaBuilder().in( this, values.toArray() );
	}

	@Override
	public JpaPredicateImplementor in(Expression<Collection<?>> values) {
		return criteriaBuilder().in( this, values );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<Long> asLong() {
		resetJavaType( Long.class );
		return (JpaExpressionImplementor<Long>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<Integer> asInteger() {
		resetJavaType( Integer.class );
		return (JpaExpressionImplementor<Integer>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<Float> asFloat() {
		resetJavaType( Float.class );
		return (JpaExpressionImplementor<Float>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<Double> asDouble() {
		resetJavaType( Double.class );
		return (JpaExpressionImplementor<Double>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<BigDecimal> asBigDecimal() {
		resetJavaType( BigDecimal.class );
		return (JpaExpressionImplementor<BigDecimal>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<BigInteger> asBigInteger() {
		resetJavaType( BigInteger.class );
		return (JpaExpressionImplementor<BigInteger>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpressionImplementor<String> asString() {
		resetJavaType( String.class );
		return (JpaExpressionImplementor<String>) this;
	}
}
