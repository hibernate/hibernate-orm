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
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import javax.persistence.metamodel.PluralAttribute.CollectionType;
import javax.persistence.metamodel.Type.PersistenceType;
import org.hibernate.ejb.criteria.expression.CollectionExpression;
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
		super( queryBuilder, entityType.getBindableJavaType(), null, null, entityType );
		this.type = new EntityTypeExpression( queryBuilder, entityType.getBindableJavaType() );
	}

	public FromImpl(
			QueryBuilderImpl queryBuilder,
			Class<X> javaType,
			PathImpl<Z> origin,
			Attribute<? super Z, ?> attribute,
			ManagedType<X> model) {
		super( queryBuilder, javaType, origin, attribute, model );
		this.type = new EntityTypeExpression( queryBuilder, model.getJavaType() );
	}

	@Override
	public Expression<Class<? extends X>> type() {
		return type;
	}

	/**
	 * Get the attribute by name from the underlying model.  This alows subclasses to
	 * define exactly how the attribute is derived.
	 * 
	 * @param name The attribute name
	 * 
	 * @return The attribute.
	 * 
	 * @throws IllegalArgumentException If no such attribute is found (follows exception type from {@link ManagedType}).
	 */
	protected abstract Attribute<X,?> getAttribute(String name);


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
		Join<X, Y> join = constructJoin( attribute, jt );
		getJoinsInternal().add( join );
		return join;
	}

	private <Y> JoinImplementors.JoinImplementor<X, Y> constructJoin(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		if ( PersistenceType.BASIC.equals( attribute.getType().getPersistenceType() ) ) {
			throw new BasicPathUsageException( "Cannot join to attribute of basic type", attribute );
        }

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<Y> attributeType = attribute.getBindableJavaType();
        return new JoinImpl<X, Y>(
				queryBuilder(),
				attributeType,
				this,
				attribute,
				jt
		);
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
		final CollectionJoin<X, Y> join = constructJoin( collection, jt );
		getJoinsInternal().add( join );
		return join;
	}

	private <Y> JoinImplementors.CollectionJoinImplementor<X, Y> constructJoin(CollectionAttribute<? super X, Y> collection, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<Y> attributeType = collection.getBindableJavaType();
		final JoinImplementors.CollectionJoinImplementor<X, Y> join;
		if ( isBasicCollection( collection ) ) {
			join = new BasicCollectionJoinImpl<X, Y>(
					queryBuilder(),
					attributeType,
					this,
					collection,
					jt
			);
		}
		else {
			join = new CollectionJoinImpl<X, Y>(
					queryBuilder(),
					attributeType,
					this,
					collection,
					jt
			);
		}
		return join;
	}

	private boolean isBasicCollection(PluralAttribute collection) {
		return PersistenceType.BASIC.equals( collection.getElementType().getPersistenceType() );
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
		final SetJoin<X, Y> join = constructJoin( set, jt );
		getJoinsInternal().add( join );
		return join;
	}

	private <Y> JoinImplementors.SetJoinImplementor<X, Y> constructJoin(SetAttribute<? super X, Y> set, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<Y> attributeType = set.getBindableJavaType();
		final JoinImplementors.SetJoinImplementor<X, Y> join;
		if ( isBasicCollection( set ) ) {
			join = new BasicSetJoinImpl<X, Y>( queryBuilder(), attributeType, this, set, jt );
		}
		else {
			join = new SetJoinImpl<X, Y>( queryBuilder(), attributeType, this, set, jt );
		}
		return join;
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
		final ListJoin<X, Y> join = constructJoin( list, jt );
		getJoinsInternal().add( join );
		return join;
	}

	private  <Y> JoinImplementors.ListJoinImplementor<X, Y> constructJoin(ListAttribute<? super X, Y> list, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<Y> attributeType = list.getBindableJavaType();
		final JoinImplementors.ListJoinImplementor<X, Y> join;
		if ( isBasicCollection( list ) ) {
			join = new BasicListJoinImpl<X, Y>( queryBuilder(), attributeType, this, list, jt );
		}
		else {
			join = new ListJoinImpl<X, Y>( queryBuilder(), attributeType, this, list, jt );
		}
		return join;
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
		final MapJoin<X, K, V> join = constructJoin( map, jt );
		getJoinsInternal().add( join );
		return join;
	}

	private <K, V> JoinImplementors.MapJoinImplementor<X, K, V> constructJoin(MapAttribute<? super X, K, V> map, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<V> attributeType = map.getBindableJavaType();
		final JoinImplementors.MapJoinImplementor<X, K, V> join;
		if ( isBasicCollection( map ) ) {
			join = new BasicMapJoinImpl<X,K,V>( queryBuilder(), attributeType, this, map, jt );
		}
		else {
			join = new MapJoinImpl<X,K,V>( queryBuilder(), attributeType, this, map, jt );
		}
		return join;
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> Join<X, Y> join(String attributeName) {
		return join( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> Join<X, Y> join(String attributeName, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( attribute.isCollection() ) {
			final PluralAttribute pluralAttribute = ( PluralAttribute ) attribute;
			if ( CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X,Y>) join( (CollectionAttribute) attribute, jt );
			}
			else if ( CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X,Y>) join( (ListAttribute) attribute, jt );
			}
			else if ( CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X,Y>) join( (SetAttribute) attribute, jt );
			}
			else {
				return (Join<X,Y>) join( (MapAttribute) attribute, jt );
			}
		}
		else {
			return (Join<X,Y>) join( (SingularAttribute)attribute, jt );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> CollectionJoin<X, Y> joinCollection(String attributeName) {
		return joinCollection( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> CollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt) {
		final Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( ! attribute.isCollection() ) {
            throw new IllegalArgumentException( "Requested attribute was not a collection" );
		}

		final PluralAttribute pluralAttribute = ( PluralAttribute ) attribute;
		if ( ! CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
            throw new IllegalArgumentException( "Requested attribute was not a collection" );
		}

		return (CollectionJoin<X,Y>) join( (CollectionAttribute) attribute, jt );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> SetJoin<X, Y> joinSet(String attributeName) {
		return joinSet( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> SetJoin<X, Y> joinSet(String attributeName, JoinType jt) {
		final Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( ! attribute.isCollection() ) {
            throw new IllegalArgumentException( "Requested attribute was not a set" );
		}

		final PluralAttribute pluralAttribute = ( PluralAttribute ) attribute;
		if ( ! CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
            throw new IllegalArgumentException( "Requested attribute was not a set" );
		}

		return (SetJoin<X,Y>) join( (SetAttribute) attribute, jt );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> ListJoin<X, Y> joinList(String attributeName) {
		return joinList( attributeName, DEFAULT_JOIN_TYPE );
	}

	/**
	 * {@inheritDoc}
	 */
	public <X,Y> ListJoin<X, Y> joinList(String attributeName, JoinType jt) {
		final Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( ! attribute.isCollection() ) {
            throw new IllegalArgumentException( "Requested attribute was not a list" );
		}

		final PluralAttribute pluralAttribute = ( PluralAttribute ) attribute;
		if ( ! CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
            throw new IllegalArgumentException( "Requested attribute was not a list" );
		}

		return (ListJoin<X,Y>) join( (ListAttribute) attribute, jt );
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
		final Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( ! attribute.isCollection() ) {
            throw new IllegalArgumentException( "Requested attribute was not a map" );
		}

		final PluralAttribute pluralAttribute = ( PluralAttribute ) attribute;
		if ( ! CollectionType.MAP.equals( pluralAttribute.getCollectionType() ) ) {
            throw new IllegalArgumentException( "Requested attribute was not a map" );
		}

		return (MapJoin<X,K,V>) join( (MapAttribute) attribute, jt );
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

	public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		Fetch<X, Y> fetch = constructJoin( attribute, jt );
		getFetchesInternal().add( fetch );
		return fetch;
	}

	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute) {
		return fetch( pluralAttribute, DEFAULT_JOIN_TYPE );
	}

	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute, JoinType jt) {
		final Fetch<X, Y> fetch;
		// TODO : combine Fetch and Join hierarchies (JoinImplementor extends Join,Fetch???)
		if ( CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (CollectionAttribute<X,Y>) pluralAttribute, jt );
		}
		else if ( CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (ListAttribute<X,Y>) pluralAttribute, jt );
		}
		else if ( CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (SetAttribute<X,Y>) pluralAttribute, jt );
		}
		else {
			fetch = constructJoin( (MapAttribute<X,?,Y>) pluralAttribute, jt );
		}
		getFetchesInternal().add( fetch );
		return fetch;
	}

	public <X,Y> Fetch<X, Y> fetch(String attributeName) {
		return fetch( attributeName, DEFAULT_JOIN_TYPE );
	}

	public <X,Y> Fetch<X, Y> fetch(String attributeName, JoinType jt) {
		Attribute<X,?> attribute = (Attribute<X, ?>) getAttribute( attributeName );
		if ( attribute.isCollection() ) {
			return (Fetch<X, Y>) fetch( (PluralAttribute) attribute, jt );
		}
		else {
			return (Fetch<X, Y>) fetch( (SingularAttribute) attribute, jt );
		}
	}


	// PATH HANDLING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <Y> Path<Y> get(SingularAttribute<? super X, Y> attribute) {
		if ( PersistentAttributeType.BASIC.equals( attribute.getPersistentAttributeType() ) ) {
            return new PathImpl<Y>( queryBuilder(), attribute.getJavaType(), this, attribute, attribute.getBindableType() );
        }
		else {
			return join( attribute );
        }
	}

	@Override
	public <E, C extends Collection<E>> Expression<C> get(PluralAttribute<X, C, E> collection) {
		return new CollectionExpression<C>( queryBuilder(), collection.getJavaType(), collection );
	}

	@Override
	public <K, V, M extends Map<K, V>> Expression<M> get(MapAttribute<X, K, V> map) {
		return ( Expression<M> ) new CollectionExpression<Map<K, V>>( queryBuilder(), map.getJavaType(), map );
	}

	@Override
	public <Y> Path<Y> get(String attributeName) {
		Attribute attribute = getAttribute( attributeName );
		if ( attribute.isCollection() ) {
			final PluralAttribute<X,?,Y> pluralAttribute = (PluralAttribute<X, ?, Y>) attribute;
			if ( CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
				return join( (CollectionAttribute<X,Y>) attribute );
			}
			else if ( CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
				return join( (ListAttribute<X,Y>) attribute );
			}
			else if ( CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
				return join( (SetAttribute<X,Y>) attribute );
			}
			else {
				return join( (MapAttribute<X,?,Y>) attribute );
			}
		}
		else {
			return get( (SingularAttribute<X,Y>) attribute );
		}
	}
}
