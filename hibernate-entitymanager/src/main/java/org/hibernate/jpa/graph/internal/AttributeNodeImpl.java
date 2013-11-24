/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.graph.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.internal.metamodel.Helper;
import org.hibernate.jpa.internal.metamodel.PluralAttributeImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerFactoryAware;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * Hibernate implementation of the JPA AttributeNode contract
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<T> implements AttributeNode<T>, AttributeNodeImplementor<T>, HibernateEntityManagerFactoryAware {
	private final HibernateEntityManagerFactory entityManagerFactory;
	private final Attribute<?,T> attribute;

	private Map<Class, Subgraph> subgraphMap;
	private Map<Class, Subgraph> keySubgraphMap;

	public <X> AttributeNodeImpl(HibernateEntityManagerFactory entityManagerFactory, Attribute<X,T> attribute) {
		this.entityManagerFactory = entityManagerFactory;
		this.attribute = attribute;
	}

	/**
	 * Intended only for use from {@link #makeImmutableCopy()}
	 */
	private AttributeNodeImpl(
			HibernateEntityManagerFactory entityManagerFactory,
			Attribute<?,T> attribute,
			Map<Class, Subgraph> subgraphMap,
			Map<Class, Subgraph> keySubgraphMap) {
		this.entityManagerFactory = entityManagerFactory;
		this.attribute = attribute;
		this.subgraphMap = subgraphMap;
		this.keySubgraphMap = keySubgraphMap;
	}

	@Override
	public HibernateEntityManagerFactory getFactory() {
		return entityManagerFactory;
	}

	private SessionFactoryImplementor sessionFactory() {
		return (SessionFactoryImplementor) getFactory().getSessionFactory();
	}

	@Override
	public Attribute<?,T> getAttribute() {
		return attribute;
	}

	public String getRegistrationName() {
		return getAttributeName();
	}

	@Override
	public String getAttributeName() {
		return attribute.getName();
	}

	@Override
	public Map<Class, Subgraph> getSubgraphs() {
		return subgraphMap == null ? Collections.<Class, Subgraph>emptyMap() : subgraphMap;
	}

	@Override
	public Map<Class, Subgraph> getKeySubgraphs() {
		return keySubgraphMap == null ? Collections.<Class, Subgraph>emptyMap() : keySubgraphMap;
	}

	@SuppressWarnings("unchecked")
	public <T> SubgraphImpl<T> makeSubgraph() {
		return (SubgraphImpl<T>) internalMakeSubgraph( null );
	}

	@SuppressWarnings("unchecked")
	public <X extends T> SubgraphImpl<X> makeSubgraph(Class<X> type) {
		return (SubgraphImpl<X>) internalMakeSubgraph( type );
	}

	private SubgraphImpl internalMakeSubgraph(Class type) {
		if ( attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC
				|| attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED ) {
			throw new IllegalArgumentException(
					String.format( "Attribute [%s] is not of managed type", getAttributeName() )
			);
		}
		if ( attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.ELEMENT_COLLECTION ) {
			throw new IllegalArgumentException(
					String.format( "Collection elements [%s] is not of managed type", getAttributeName() )
			);
		}

		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<Class, Subgraph>();
		}

		final AssociationType attributeType = (AssociationType) Helper.resolveType( sessionFactory(), attribute );
		final Joinable joinable = attributeType.getAssociatedJoinable( sessionFactory() );

		if ( joinable.isCollection() ) {
			final EntityPersister elementEntityPersister = ( (QueryableCollection) joinable ).getElementPersister();
			if ( type == null ) {
				type = elementEntityPersister.getMappedClass();
			}
			else  {
				if ( !isTreatableAs( elementEntityPersister, type ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Collection elements [%s] cannot be treated as requested type [%s] : %s",
									getAttributeName(),
									type.getName(),
									elementEntityPersister.getMappedClass().getName()
							)
					);
				}
			}
		}
		else {
			final EntityPersister entityPersister = (EntityPersister) joinable;
			if ( type == null ) {
				type = entityPersister.getMappedClass();
			}
			else {
				if ( !isTreatableAs( entityPersister, type ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Attribute [%s] cannot be treated as requested type [%s] : %s",
									getAttributeName(),
									type.getName(),
									entityPersister.getMappedClass().getName()
							)
					);
				}
			}
		}
		
		ManagedType managedType = null;
		try {
			managedType = entityManagerFactory.getEntityTypeByName( type.getName() );
		}
		catch (IllegalArgumentException e) {
			// do nothing
		}
		if (managedType == null) {
			managedType = attribute.getDeclaringType();
		}

		final SubgraphImpl subgraph = new SubgraphImpl( this.entityManagerFactory, managedType, type );
		subgraphMap.put( type, subgraph );
		return subgraph;
	}

	/**
	 * Check to make sure that the java type of the given entity persister is treatable as the given type.  In other
	 * words, is the given type a subclass of the class represented by the persister.
	 *
	 * @param entityPersister The persister to check
	 * @param type The type to check it against
	 *
	 * @return {@code true} indicates it is treatable as such; {@code false} indicates it is not
	 */
	@SuppressWarnings("unchecked")
	private boolean isTreatableAs(EntityPersister entityPersister, Class type) {
		return type.isAssignableFrom( entityPersister.getMappedClass() );
	}

	@SuppressWarnings("unchecked")
	public <T> SubgraphImpl<T> makeKeySubgraph() {
		return (SubgraphImpl<T>) internalMakeKeySubgraph( null );
	}

	@SuppressWarnings("unchecked")
	public <X extends T> SubgraphImpl<X> makeKeySubgraph(Class<X> type) {
		return (SubgraphImpl<X>) internalMakeKeySubgraph( type );
	}

	public SubgraphImpl internalMakeKeySubgraph(Class type) {
		if ( ! attribute.isCollection() ) {
			throw new IllegalArgumentException(
					String.format( "Non-collection attribute [%s] cannot be target of key subgraph", getAttributeName() )
			);
		}

		final PluralAttributeImpl pluralAttribute = (PluralAttributeImpl) attribute;
		if ( pluralAttribute.getCollectionType() != PluralAttribute.CollectionType.MAP ) {
			throw new IllegalArgumentException(
					String.format( "Non-Map attribute [%s] cannot be target of key subgraph", getAttributeName() )
			);
		}

		final AssociationType attributeType = (AssociationType) Helper.resolveType( sessionFactory(), attribute );
		final QueryableCollection collectionPersister = (QueryableCollection) attributeType.getAssociatedJoinable( sessionFactory() );
		final Type indexType = collectionPersister.getIndexType();

		if ( ! indexType.isAssociationType() ) {
			throw new IllegalArgumentException(
					String.format( "Map index [%s] is not an entity; cannot be target of key subgraph", getAttributeName() )
			);
		}

		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<Class, Subgraph>();
		}

		final AssociationType indexAssociationType = (AssociationType) indexType;
		final EntityPersister indexEntityPersister = (EntityPersister) indexAssociationType.getAssociatedJoinable( sessionFactory() );

		if ( type == null ) {
			type = indexEntityPersister.getMappedClass();
		}
		else {
			if ( !isTreatableAs( indexEntityPersister, type ) ) {
				throw new IllegalArgumentException(
						String.format(
								"Map key [%s] cannot be treated as requested type [%s] : %s",
								getAttributeName(),
								type.getName(),
								indexEntityPersister.getMappedClass().getName()
						)
				);
			}
		}

		final SubgraphImpl subgraph = new SubgraphImpl( this.entityManagerFactory, attribute.getDeclaringType(), type );
		keySubgraphMap.put( type, subgraph );
		return subgraph;
	}

	@Override
	public AttributeNodeImpl<T> makeImmutableCopy() {
		return new AttributeNodeImpl<T>(
				this.entityManagerFactory,
				this.attribute,
				makeSafeMapCopy( subgraphMap ),
				makeSafeMapCopy( keySubgraphMap )
		);
	}

	private static Map<Class, Subgraph> makeSafeMapCopy(Map<Class, Subgraph> subgraphMap) {
		if ( subgraphMap == null ) {
			return null;
		}

		final int properSize = CollectionHelper.determineProperSizing( subgraphMap );
		final HashMap<Class,Subgraph> copy = new HashMap<Class,Subgraph>( properSize );
		for ( Map.Entry<Class, Subgraph> subgraphEntry : subgraphMap.entrySet() ) {
			copy.put(
					subgraphEntry.getKey(),
					( ( SubgraphImpl ) subgraphEntry.getValue() ).makeImmutableCopy()
			);
		}
		return copy;
	}

}
