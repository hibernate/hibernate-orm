/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import org.hibernate.ejb.criteria.QueryBuilderImpl;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class AbstractExpression<T> extends SelectionImpl<T> implements Expression<T> {
	protected AbstractExpression(QueryBuilderImpl queryBuilder, Class<T> javaType) {
		super( queryBuilder, javaType );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X> Expression<X> as(Class<X> type) {
		// TODO-STEVE : implement - needs a cast expression
		throw new UnsupportedOperationException( "Not yet implemented!" );
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
