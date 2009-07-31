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
package org.hibernate.ejb.criteria;

import java.util.Set;
import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashSet;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import javax.persistence.metamodel.Type.PersistenceType;
import org.hibernate.ejb.criteria.expression.AbstractExpression;
import org.hibernate.ejb.criteria.expression.EntityTypeExpression;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class FromImpl<Z,X> extends PathImpl<X> implements From<Z,X> {
	public static final JoinType DEFAULT_JOIN_TYPE = JoinType.INNER;

	private final Expression<Class<? extends X>> type;
    private Set<Join<X, ?>> joins;
    private Set<Fetch<X, ?>> fetches;

	/**
	 * Special constructor for {@link RootImpl}.
	 *
	 * @param queryBuilder
	 * @param entityType
	 */
    protected FromImpl(QueryBuilderImpl queryBuilder, EntityType<X> entityType) {
		super( queryBuilder, entityType.getBindableJavaType(), null, entityType );
		this.type = new EntityTypeExpression( queryBuilder, entityType.getBindableJavaType() );
	}

	public FromImpl(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			PathImpl<Z> origin,
			Bindable<X> model,
			Expression<Class<? extends X>> type) {
		super( queryBuilder, javaType, origin, model );
		this.type = type;
	}

	@Override
	public Expression<Class<? extends X>> type() {
		return type;
	}


	// JOINS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Set<Join<X, ?>> getJoins() {
		return joins;
	}

	/**
	 * Retrieve the collection of joins, imnplementing delayed creations.  Calls on this method
	 * assume the set is physically needed, and the result is the set being built if not already.
	 *
	 * @return The set of joins.
	 */
	public Set<Join<X, ?>> getJoinsInternal() {
		if ( joins == null ) {
			joins = new LinkedHashSet<Join<X,?>>();
		}
		return joins;
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> singularAttribute) {
		return join( singularAttribute, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		if ( PersistenceType.BASIC.equals( attribute.getType().getPersistenceType() ) ) {
            throw new IllegalStateException( "Cannot join to basic type" );
        }
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection) {
		return join( collection, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> SetJoin<X, Y> join(SetAttribute<? super X, Y> set) {
		return join( set, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> SetJoin<X, Y> join(SetAttribute<? super X, Y> set, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> ListJoin<X, Y> join(ListAttribute<? super X, Y> list) {
		return join( list, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <Y> ListJoin<X, Y> join(ListAttribute<? super X, Y> list, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <K, V> MapJoin<X, K, V> join(MapAttribute<? super X, K, V> map) {
		return join( map, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <K, V> MapJoin<X, K, V> join(MapAttribute<? super X, K, V> map, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> Join<X, Y> join(String attributeName) {
		return join( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> Join<X, Y> join(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName) {
		return joinCollection( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName) {
		return joinSet( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> ListJoin<X, Y> joinList(String attributeName) {
		return joinList( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, Y> ListJoin<X, Y> joinList(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName) {
		return joinMap( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}


	// FETCHES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Set<Fetch<X, ?>> getFetches() {
		return fetches;
	}

	/**
	 * Retrieve the collection of fetches, imnplementing delayed creations.  Calls on this method
	 * assume the set is physically needed, and the result is the set being built if not already.
	 *
	 * @return The set of fetches.
	 */
	public Set<Fetch<X, ?>> getFetchesInternal() {
		if ( fetches == null ) {
			fetches = new LinkedHashSet<Fetch<X,?>>();
		}
		return fetches;
	}

	public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> singularAttribute) {
		return fetch( singularAttribute, DEFAULT_JOIN_TYPE );
	}

	public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> singularAttribute, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute) {
		return fetch( pluralAttribute, DEFAULT_JOIN_TYPE );
	}

	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	public <Y> Fetch<X, Y> fetch(String attributeName) {
		return fetch( attributeName, DEFAULT_JOIN_TYPE );
	}

	public <Y> Fetch<X, Y> fetch(String attributeName, JoinType jt) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}


	// PATH HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public <Y> Path<Y> get(SingularAttribute<? super X, Y> ySingularAttribute) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> collection) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> map) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}

	public <Y> Path<Y> get(String attributeName) {
		// TODO : implement
		throw new UnsupportedOperationException( "Not yet implemented!" );
	}
}
