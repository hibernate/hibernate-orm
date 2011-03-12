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
import java.util.Map;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.MapAttribute;

import org.hibernate.ejb.criteria.CriteriaBuilderImpl;
import org.hibernate.ejb.criteria.CriteriaSubqueryImpl;
import org.hibernate.ejb.criteria.FromImplementor;
import org.hibernate.ejb.criteria.MapJoinImplementor;
import org.hibernate.ejb.criteria.PathImplementor;
import org.hibernate.ejb.criteria.PathSource;
import org.hibernate.ejb.criteria.expression.MapEntryExpression;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class MapAttributeJoin<O,K,V>
		extends PluralAttributeJoinSupport<O, Map<K,V>, V>
		implements MapJoinImplementor<O,K,V>, Serializable {

	public MapAttributeJoin(
			CriteriaBuilderImpl criteriaBuilder,
			Class<V> javaType,
			PathSource<O> pathSource,
			MapAttribute<? super O, K, V> joinAttribute,
			JoinType joinType) {
		super( criteriaBuilder, javaType, pathSource, joinAttribute, joinType );
	}

	@Override
	public MapAttribute<? super O, K, V> getAttribute() {
		return (MapAttribute<? super O, K, V>) super.getAttribute();
	}

	@Override
	public MapAttribute<? super O, K, V> getModel() {
		return getAttribute();
	}

	@Override
	public final MapAttributeJoin<O,K,V> correlateTo(CriteriaSubqueryImpl subquery) {
		return (MapAttributeJoin<O,K,V>) super.correlateTo( subquery );
	}

	@Override
	protected FromImplementor<O, V> createCorrelationDelegate() {
		return new MapAttributeJoin<O,K,V>(
				criteriaBuilder(),
				getJavaType(),
				(PathImplementor<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
	}


	/**
	 * {@inheritDoc}
	 */
	public Path<V> value() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Expression<Map.Entry<K, V>> entry() {
		return new MapEntryExpression( criteriaBuilder(), Map.Entry.class, this, getAttribute() );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Path<K> key() {
		final MapKeyHelpers.MapKeySource<K,V> mapKeySource = new MapKeyHelpers.MapKeySource<K,V>(
				criteriaBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( criteriaBuilder(), getAttribute() );
		return new MapKeyHelpers.MapKeyPath( criteriaBuilder(), mapKeySource, mapKeyAttribute );
	}

}
