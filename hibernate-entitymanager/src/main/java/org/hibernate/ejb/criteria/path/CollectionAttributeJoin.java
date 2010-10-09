/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.ejb.criteria.path;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.CollectionAttribute;

import org.hibernate.ejb.criteria.CollectionJoinImplementor;
import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaSubqueryImpl;
import org.hibernate.ejb.criteria.FromImplementor;
import org.hibernate.ejb.criteria.PathImplementor;
import org.hibernate.ejb.criteria.PathSource;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CollectionAttributeJoin<O,E>
		extends PluralAttributeJoinSupport<O,Collection<E>,E>
		implements CollectionJoinImplementor<O,E>, Serializable {
	public CollectionAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<E> javaType,
			PathSource<O> pathSource,
			CollectionAttribute<? super O, E> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	public final CollectionAttributeJoin<O,E> correlateTo(CriteriaSubqueryImpl subquery) {
		return (CollectionAttributeJoin<O,E>) super.correlateTo( subquery );
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
	protected FromImplementor<O, E> createCorrelationDelegate() {
		return new CollectionAttributeJoin<O,E>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}
}
