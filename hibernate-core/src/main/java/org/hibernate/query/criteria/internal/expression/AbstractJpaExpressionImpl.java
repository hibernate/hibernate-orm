/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.sqm.parser.criteria.tree.JpaExpression;

import org.hibernate.test.sqm.domain.BasicType;
import org.hibernate.test.sqm.domain.Type;
import org.hibernate.test.sqm.parser.criteria.tree.CriteriaBuilderImpl;
import org.hibernate.test.sqm.parser.criteria.tree.expression.function.CastFunction;
import org.hibernate.test.sqm.parser.criteria.tree.select.AbstractSimpleSelection;

/**
 * Models an expression in the criteria query language.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaExpressionImpl<T>
		extends AbstractSimpleSelection<T>
		implements JpaExpression<T>, Serializable {
	private Type sqmType;

	public AbstractJpaExpressionImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Type sqmType,
			Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	@Override
	public Type getExpressionSqmType() {
		return sqmType;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> Expression<X> as(Class<X> type) {
		if ( type.equals( getJavaType() ) ) {
			return (Expression<X>) this;
		}
		else {
			return new CastFunction<>(
					this,
					(BasicType<X>) criteriaBuilder().consumerContext().getDomainMetamodel().resolveBasicType( type ),
					type,
					criteriaBuilder()
			);
		}
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
	public JpaExpression<Long> asLong() {
		resetJavaType( Long.class );
		return (JpaExpression<Long>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Integer> asInteger() {
		resetJavaType( Integer.class );
		return (JpaExpression<Integer>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Float> asFloat() {
		resetJavaType( Float.class );
		return (JpaExpression<Float>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<Double> asDouble() {
		resetJavaType( Double.class );
		return (JpaExpression<Double>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<BigDecimal> asBigDecimal() {
		resetJavaType( BigDecimal.class );
		return (JpaExpression<BigDecimal>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<BigInteger> asBigInteger() {
		resetJavaType( BigInteger.class );
		return (JpaExpression<BigInteger>) this;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public JpaExpression<String> asString() {
		resetJavaType( String.class );
		return (JpaExpression<String>) this;
	}
}
