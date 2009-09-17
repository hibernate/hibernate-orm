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
import javax.persistence.metamodel.SetAttribute;
import org.hibernate.ejb.criteria.JoinImplementors.SetJoinImplementor;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.Set}, whose elements
 * are basic type.
 *
 * @author Steve Ebersole
 */
public class BasicSetJoinImpl<O,E>
		extends AbstractBasicPluralJoin<O,java.util.Set<E>,E> 
		implements JoinImplementors.SetJoinImplementor<O,E> {

	public BasicSetJoinImpl(
			QueryBuilderImpl queryBuilder,
			Class<E> javaType,
			PathImpl<O> lhs,
			SetAttribute<? super O, ?> joinProperty,
			JoinType joinType) {
		super( queryBuilder, javaType, lhs, joinProperty, joinType );
	}

	@Override
	public SetJoinImplementor<O, E> correlateTo(CriteriaSubqueryImpl subquery) {
		BasicSetJoinImpl<O,E> correlation = new BasicSetJoinImpl<O,E>(
				queryBuilder(),
				getJavaType(),
				(PathImpl<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
		correlation.defineJoinScope( subquery.getJoinScope() );
		return correlation;
	}

	@Override
	public SetAttribute<? super O, E> getAttribute() {
		return (SetAttribute<? super O, E>) super.getAttribute();
	}

	@Override
	public SetAttribute<? super O, E> getModel() {
        return getAttribute();
    }
}
