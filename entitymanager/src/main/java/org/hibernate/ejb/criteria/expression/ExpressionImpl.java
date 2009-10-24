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
package org.hibernate.ejb.criteria.expression;

import java.util.Collection;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.expression.function.CastFunction;

/**
 * Models an expression in the criteria query language.
 *
 * @author Steve Ebersole
 */
public abstract class ExpressionImpl<T> extends SelectionImpl<T> implements ExpressionImplementor<T> {
	public ExpressionImpl(CriteriaBuilderImpl criteriaBuilder, Class<T> javaType) {
		super( criteriaBuilder, javaType );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Expression<X> as(Class<X> type) {
		return type.equals( getJavaType() )
				? (Expression<X>) this
				: new CastFunction<X, T>( queryBuilder(), type, this );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isNull() {
		return queryBuilder().isNull( this );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate isNotNull() {
		return queryBuilder().isNotNull( this );
	}

	/**
	 * {@inheritDoc}
	 */
    public Predicate in(Object... values) {
		return queryBuilder().in( this, values );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate in(Expression<?>... values) {
		return queryBuilder().in( this, values );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate in(Collection<?> values) {
		return queryBuilder().in( this, values );
	}

	/**
	 * {@inheritDoc}
	 */
	public Predicate in(Expression<Collection<?>> values) {
		return queryBuilder().in( this, values );
	}
}
