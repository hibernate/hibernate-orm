/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaInImplementor;
import org.hibernate.query.criteria.internal.expression.function.CastFunction;
import org.hibernate.query.criteria.internal.selection.AbstractSimpleSelection;
import org.hibernate.query.criteria.spi.JpaCriteriaBuilderImplementor;
import org.hibernate.query.criteria.spi.JpaExpressionImplementor;
import org.hibernate.query.criteria.spi.JpaPredicateImplementor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models an expression in the criteria query language.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractExpression<T>
		extends AbstractSimpleSelection<T>
		implements JpaExpressionImplementor<T>, Serializable {
	public AbstractExpression(JpaCriteriaBuilderImplementor criteriaBuilder, JavaTypeDescriptor<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> JpaExpressionImplementor<X> as(JavaTypeDescriptor<X> type) {
		return type.equals( getJavaTypeDescriptor() )
				? (JpaExpressionImplementor<X>) this
				: new CastFunction<>( getCriteriaBuilder(), type, this );
	}

	@Override
	public JpaPredicateImplementor isNull() {
		return getCriteriaBuilder().isNull( this );
	}

	@Override
	public JpaPredicateImplementor isNotNull() {
		return getCriteriaBuilder().isNotNull( this );
	}

	@Override
	public JpaInImplementor in(Object... values) {
		return getCriteriaBuilder().in( this, values );
	}

	@Override
	public JpaPredicateImplementor in(Expression<?>... values) {
		return getCriteriaBuilder().in( this, values );
	}

	@Override
	public JpaPredicateImplementor in(Collection<?> values) {
		return getCriteriaBuilder().in( this, values.toArray() );
	}

	@Override
	public JpaPredicateImplementor in(Expression<Collection<?>> values) {
		return getCriteriaBuilder().in( this, values );
	}
}
