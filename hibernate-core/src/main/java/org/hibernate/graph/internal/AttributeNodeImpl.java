/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEmbedded;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

import org.jboss.logging.Logger;

/**
 * Hibernate implementation of the JPA AttributeNode contract
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<T> implements AttributeNode<T>, AttributeNodeImplementor<T> {
	private final SessionFactoryImplementor sessionFactory;
	private final PersistentAttribute<?,T> attribute;
	private final ManagedTypeDescriptor managedType;

	private Map<Class, Subgraph> subgraphMap;
	private Map<Class, Subgraph> keySubgraphMap;

	@SuppressWarnings("WeakerAccess")
	public <X> AttributeNodeImpl(
			SessionFactoryImplementor sessionFactory,
			ManagedTypeDescriptor managedType,
			PersistentAttribute<X, T> attribute) {
		this.sessionFactory = sessionFactory;
		this.managedType = managedType;
		this.attribute = attribute;
	}

	private <X> boolean determineCanContainKeySubgraphs(PersistentAttribute<X, T> attribute) {
		final boolean isCollectionIndex = attribute instanceof PluralPersistentAttribute
				&& ( (PluralPersistentAttribute) attribute ).getPersistentCollectionDescriptor().getIndexDescriptor() != null;
		final boolean isCompositeId = attribute instanceof EntityIdentifier;

		return isCollectionIndex || isCompositeId;
	}

	/**
	 * Intended only for use from {@link #makeImmutableCopy()}
	 */
	private AttributeNodeImpl(
			SessionFactoryImplementor sessionFactory,
			ManagedTypeDescriptor managedType,
			PersistentAttribute<?, T> attribute,
			Map<Class, Subgraph> subgraphMap,
			Map<Class, Subgraph> keySubgraphMap) {
		this.sessionFactory = sessionFactory;
		this.managedType = managedType;
		this.attribute = attribute;
		this.subgraphMap = subgraphMap;
		this.keySubgraphMap = keySubgraphMap;
	}

	public SessionFactoryImplementor getFactory() {
		return sessionFactory;
	}

	private SessionFactoryImplementor sessionFactory() {
		return getFactory();
	}

	@Override
	public PersistentAttribute<?,T> getAttribute() {
		return attribute;
	}

	@SuppressWarnings("WeakerAccess")
	public String getRegistrationName() {
		return getAttributeName();
	}

	@Override
	public String getAttributeName() {
		return attribute.getName();
	}

	@Override
	public Map<Class, SubGraphImplementor> subGraphs() {
		return subgraphMap == null ? Collections.emptyMap() : cast( subgraphMap );
	}

	@Override
	public Map<Class, SubGraphImplementor> keySubGraphs() {
		return keySubgraphMap == null ? Collections.emptyMap() : cast( keySubgraphMap ) ;
	}

	@SuppressWarnings("unchecked")
	private Map<Class, SubGraphImplementor> cast(Map keySubgraphMap) {
		return keySubgraphMap;
	}

	@Override
	public Map<Class, Subgraph> getSubgraphs() {
		return subgraphMap == null ? Collections.emptyMap() : subgraphMap;
	}

	@Override
	public Map<Class, Subgraph> getKeySubgraphs() {
		return keySubgraphMap == null ? Collections.emptyMap() : keySubgraphMap;
	}

	private static final Logger log = Logger.getLogger( AttributeNodeImpl.class );


	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> makeSubgraph() {
		return (SubgraphImpl<X>) internalMakeSubgraph( null );
	}

	@SuppressWarnings("WeakerAccess")
	public <X extends T> SubgraphImpl<X> makeSubgraph(Class<X> type) {
		return internalMakeSubgraph( type );
	}

	@SuppressWarnings("unchecked")
	private <X> SubgraphImpl<X> internalMakeSubgraph(Class<X> type) {
		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<>();
		}

		final ManagedTypeDescriptor<X> associatedManagedTypeDescriptor;
		if ( attribute instanceof PluralPersistentAttribute ) {
			final PluralPersistentAttribute pluralAttribute = (PluralPersistentAttribute) attribute;
			final CollectionElement elementDescriptor = pluralAttribute.getPersistentCollectionDescriptor().getElementDescriptor();
			if ( CollectionElementEmbedded.class.isInstance( elementDescriptor ) ) {
				associatedManagedTypeDescriptor = ( (CollectionElementEmbedded) elementDescriptor ).getEmbeddedDescriptor();
			}
			else if ( CollectionElementEntity.class.isInstance( elementDescriptor ) ) {
				associatedManagedTypeDescriptor = ( (CollectionElementEntity) elementDescriptor ).getEntityDescriptor();
			}
			else {
				throw new IllegalArgumentException(
						String.format( "Collection elements [%s] is not of managed type", getAttributeName() )
				);
			}
		}
		else {
			if ( EntityIdentifier.class.isInstance( attribute ) ) {
				throw new IllegalArgumentException(
						"Subgraphs for an entity identifier should be added as a key-subgraph"
				);
			}
			else if ( SingularPersistentAttributeEmbedded.class.isInstance( attribute ) ) {
				associatedManagedTypeDescriptor = ( (SingularPersistentAttributeEmbedded) attribute ).getEmbeddedDescriptor();
			}
			else if ( SingularPersistentAttributeEntity.class.isInstance( attribute ) ) {
				associatedManagedTypeDescriptor = ( (SingularPersistentAttributeEntity) attribute ).getAssociatedEntityDescriptor();
			}
			else {
				throw new IllegalArgumentException(
						String.format( "Attribute [%s] is not of managed type", getAttributeName() )
				);
			}
		}

		if ( !isTreatableAs( associatedManagedTypeDescriptor, type ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Subgraph [%s] cannot be treated as requested type [%s] : %s",
							getAttributeName(),
							type.getName(),
							associatedManagedTypeDescriptor.getNavigableName()
					)
			);
		}

		final SubgraphImpl<X> subgraph = new SubgraphImpl<>( this.sessionFactory, associatedManagedTypeDescriptor, type );
		subgraphMap.put( type, subgraph );
		return subgraph;
	}

	/**
	 * Check to make sure that the java type of the given entity persister is treatable as the given type.  In other
	 * words, is the given type a subclass of the class represented by the persister.
	 *
	 * @param managedType The domain model type descriptor to check
	 * @param javaType The type to check it against
	 *
	 * @return {@code true} indicates it is treatable as such; {@code false} indicates it is not
	 */
	@SuppressWarnings("unchecked")
	private boolean isTreatableAs(ManagedTypeDescriptor managedType, Class javaType) {
		return javaType == null || javaType.isAssignableFrom( managedType.getJavaType() );
	}

	@SuppressWarnings("unchecked")
	public <X> SubgraphImpl<X> makeKeySubgraph() {
		return (SubgraphImpl<X>) internalMakeKeySubgraph( null );
	}

	@SuppressWarnings("WeakerAccess")
	public <X extends T> SubgraphImpl<X> makeKeySubgraph(Class<X> type) {
		return internalMakeKeySubgraph( type );
	}

	@SuppressWarnings("unchecked")
	private <X> SubgraphImpl<X> internalMakeKeySubgraph(Class<X> type) {
		final ManagedTypeDescriptor<X> associatedManagedTypeDescriptor;
		if ( attribute instanceof PluralPersistentAttribute ) {
			final PluralPersistentAttribute pluralAttribute = (PluralPersistentAttribute) attribute;
			final CollectionIndex indexDescriptor = pluralAttribute.getPersistentCollectionDescriptor().getIndexDescriptor();
			if ( CollectionIndexEmbedded.class.isInstance( indexDescriptor ) ) {
				associatedManagedTypeDescriptor = ( (CollectionIndexEmbedded) indexDescriptor ).getEmbeddedDescriptor();
			}
			else if ( CollectionIndexEntity.class.isInstance( indexDescriptor ) ) {
				associatedManagedTypeDescriptor = ( (CollectionIndexEntity) indexDescriptor ).getEntityDescriptor();
			}
			else {
				throw new IllegalArgumentException(
						String.format( "Collection index [%s] is not of managed type", getAttributeName() )
				);
			}
		}
		else {
			if ( !EntityIdentifier.class.isInstance( attribute ) ) {
				throw new IllegalArgumentException(
						"Subgraphs for an entity non-identifier attributes should be added as a non-key subgraph"
				);
			}

			if ( EntityIdentifierSimple.class.isInstance( attribute ) ) {
				throw new IllegalArgumentException(
						String.format( "Entity identifier [%s] is not of managed type", getAttributeName() )
				);
			}
			else if ( EntityIdentifierCompositeAggregated.class.isInstance( attribute ) ) {
				associatedManagedTypeDescriptor = ( (EntityIdentifierCompositeAggregated) attribute ).getEmbeddedDescriptor();
			}
			else if ( EntityIdentifierCompositeNonAggregated.class.isInstance( attribute ) ) {
				associatedManagedTypeDescriptor = ( (EntityIdentifierCompositeNonAggregated) attribute ).getEmbeddedDescriptor();
			}
			else {
				throw new IllegalArgumentException(
						String.format(
								"Entity identifier metadata not of expected type (%s,%s) : %s",
								EntityIdentifierCompositeAggregated.class.getName(),
								EntityIdentifierCompositeNonAggregated.class.getName(),
								attribute.getClass().getName()
						)
				);
			}
		}

		if ( !isTreatableAs( associatedManagedTypeDescriptor, type ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Subgraph [%s] cannot be treated as requested type [%s] : %s",
							getAttributeName(),
							type.getName(),
							associatedManagedTypeDescriptor.getNavigableName()
					)
			);
		}


		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<>();
		}

		final SubgraphImpl<X> subgraph = new SubgraphImpl<>( this.sessionFactory, associatedManagedTypeDescriptor, type );
		keySubgraphMap.put( type, subgraph );
		return subgraph;
	}

	@Override
	public AttributeNodeImpl<T> makeImmutableCopy() {
		return new AttributeNodeImpl<>(
				this.sessionFactory,
				this.managedType,
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
		final HashMap<Class,Subgraph> copy = new HashMap<>( properSize );
		for ( Map.Entry<Class, Subgraph> subgraphEntry : subgraphMap.entrySet() ) {
			copy.put(
					subgraphEntry.getKey(),
					( (SubgraphImpl) subgraphEntry.getValue() ).makeImmutableCopy()
			);
		}
		return copy;
	}

	@Override
	public SubGraphImplementor extractSubGraph(PersistentAttribute<?,T> persistentAttribute) {
		final Map<Class,SubGraphImplementor> subgraphMap = subGraphs();
		if ( subgraphMap.size() == 0 ) {
			return null;
		}
		else if ( subgraphMap.size() == 1 ) {
			return subgraphMap.values().iterator().next();
		}

		return subgraphMap.get( persistentAttribute.getJavaType() );
	}

}
