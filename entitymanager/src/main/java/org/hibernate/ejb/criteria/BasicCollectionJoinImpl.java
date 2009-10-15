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

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.From;
import javax.persistence.metamodel.CollectionAttribute;
import org.hibernate.ejb.criteria.JoinImplementors.CollectionJoinImplementor;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.Collection}, whose elements
 * are basic type.
 *
 * @author Steve Ebersole
 */
public class BasicCollectionJoinImpl<O,E>
		extends AbstractBasicPluralJoin<O,java.util.Collection<E>,E>
		implements JoinImplementors.CollectionJoinImplementor<O,E> {

	public BasicCollectionJoinImpl(
			QueryBuilderImpl queryBuilder,
			Class<E> javaType,
			PathImpl<O> lhs,
			CollectionAttribute<? super O, E> joinProperty,
			JoinType joinType) {
		super(queryBuilder, javaType, lhs, joinProperty, joinType);
	}

	@Override
	public CollectionAttribute<? super O, E> getAttribute() {
		return (CollectionAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public CollectionAttribute<? super O, E> getModel() {
        return getAttribute();
    }

	@Override
	public CollectionJoinImplementor<O, E> correlateTo(CriteriaSubqueryImpl subquery) {
		BasicCollectionJoinImpl<O,E> correlation = new BasicCollectionJoinImpl<O,E>(
				queryBuilder(),
				getJavaType(),
				(PathImpl<O>) getParentPath(),
				getModel(),
				getJoinType()
		);
		correlation.defineJoinScope( subquery.getJoinScope() );
		correlation.correlationParent = this;
		return correlation;
	}

	private From<O, E> correlationParent;

	/**
	 * {@inheritDoc}
	 */
	public boolean isCorrelated() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public From<O, E> getCorrelationParent() {
		return null;
	}
}
