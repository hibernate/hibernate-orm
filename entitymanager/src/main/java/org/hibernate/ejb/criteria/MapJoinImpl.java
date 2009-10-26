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

import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.From;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Type.PersistenceType;
import org.hibernate.ejb.criteria.JoinImplementors.MapJoinImplementor;

/**
 * Represents a join to a persistent collection, defined as type {@link java.util.Map}, whose elements
 * are associations.
 *
 * @author Steve Ebersole
 */
public class MapJoinImpl<O,K,V>
		extends JoinImpl<O,V>
		implements JoinImplementors.MapJoinImplementor<O,K,V> {

	public MapJoinImpl(
			CriteriaBuilderImpl criteriaBuilder,
			Class<V> javaType,
			PathImpl<O> lhs,
			MapAttribute<? super O, K, V> joinProperty,
			JoinType joinType) {
		super( criteriaBuilder, javaType, lhs, joinProperty, joinType);
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
	protected ManagedType<V> getManagedType() {
		return ( ManagedType<V> ) getAttribute().getElementType();
	}

	/**
	 * {@inheritDoc}
	 */
	public Join<Map<K, V>, K> joinKey() {
		return joinKey( DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Join<Map<K, V>, K> joinKey(JoinType jt) {
		if ( PersistenceType.BASIC.equals( getAttribute().getKeyType().getPersistenceType() ) ) {
			throw new BasicPathUsageException( "Cannot join to map key of basic type", getAttribute() );
        }

		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final MapKeyHelpers.MapPath<K,V> mapKeySource = new MapKeyHelpers.MapPath<K,V>(
				queryBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute(),
				getParentPath().getModel()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( queryBuilder(), getAttribute() );
		return new MapKeyHelpers.MapKeyJoin<K,V>(
				queryBuilder(),
				mapKeySource,
				mapKeyAttribute,
				jt
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	public Path<K> key() {
		final MapKeyHelpers.MapPath<K,V> mapKeySource = new MapKeyHelpers.MapPath<K,V>(
				queryBuilder(),
				getAttribute().getJavaType(),
				this,
				getAttribute(),
				getParentPath().getModel()
		);
		final MapKeyHelpers.MapKeyAttribute mapKeyAttribute = new MapKeyHelpers.MapKeyAttribute( queryBuilder(), getAttribute() );
		return new MapKeyHelpers.MapKeyPath( queryBuilder(), mapKeySource, mapKeyAttribute );
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
	public Expression<Entry<K, V>> entry() {
		return new MapKeyHelpers.MapEntryExpression( queryBuilder(), Map.Entry.class, this, getAttribute() );
	}


	private From<O, V> correlationParent;

	@Override
	@SuppressWarnings({ "unchecked" })
	public MapJoinImplementor<O, K, V> correlateTo(CriteriaSubqueryImpl subquery) {
		MapJoinImpl<O, K, V> correlation = new MapJoinImpl<O, K, V>(
				queryBuilder(),
				getJavaType(),
				(PathImpl<O>) getParentPath(),
				getAttribute(),
				getJoinType()
		);
		correlation.defineJoinScope( subquery.getJoinScope() );
		correlation.correlationParent = this;
		return correlation;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isCorrelated() {
		return getCorrelationParent() != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public From<O, V> getCorrelationParent() {
		return correlationParent;
	}
}
