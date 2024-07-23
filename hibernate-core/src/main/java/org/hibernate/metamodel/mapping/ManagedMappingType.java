/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Mapping-model corollary to {@link jakarta.persistence.metamodel.ManagedType}
 *
 * @author Steve Ebersole
 */
public interface ManagedMappingType extends MappingType, FetchableContainer {
	@Override
	default JavaType<?> getJavaType() {
		return getMappedJavaType();
	}

	@Override
	default MappingType getPartMappingType() {
		return this;
	}

	/**
	 * Get the number of attributes defined on this class and any supers
	 */
	int getNumberOfAttributeMappings();

	/**
	 * Retrieve an attribute by its contributor position
	 */
	AttributeMapping getAttributeMapping(int position);

	/**
	 * Find an attribute by name.
	 *
	 * @return The named attribute, or {@code null} if no match was found
	 */
	default AttributeMapping findAttributeMapping(String name) {
		return null;
	}

	/**
	 * Get access to the attributes defined on this class and any supers
	 */
	AttributeMappingsList getAttributeMappings();

	/**
	 * Visit attributes defined on this class and any supers
	 */
	void forEachAttributeMapping(Consumer<? super AttributeMapping> action);

	/**
	 * Visit attributes defined on this class and any supers
	 */
	default void forEachAttributeMapping(IndexedConsumer<? super AttributeMapping> consumer) {
		getAttributeMappings().indexedForEach( consumer );
	}

	/**
	 * Extract the individual attribute values from the entity instance
	 */
	Object[] getValues(Object instance);

	/**
	 * Extract a specific attribute value from the entity instance, by position
	 */
	default Object getValue(Object instance, int position) {
		return getAttributeMapping( position ).getPropertyAccess().getGetter().get( instance );
	}

	/**
	 * Inject the attribute values into the entity instance
	 */
	void setValues(Object instance, Object[] resolvedValues);

	/**
	 * Inject a specific attribute value into the entity instance, by position
	 */
	default void setValue(Object instance, int position, Object value) {
		getAttributeMapping( position ).getPropertyAccess().getSetter().set( instance, value );
	}

	default boolean anyRequiresAggregateColumnWriter() {
		final int end = getNumberOfAttributeMappings();
		for ( int i = 0; i < end; i++ ) {
			final MappingType mappedType = getAttributeMapping( i ).getMappedType();
			if ( mappedType instanceof EmbeddableMappingType ) {
				if ( ( (EmbeddableMappingType) mappedType ).anyRequiresAggregateColumnWriter() ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	default boolean hasPartitionedSelectionMapping() {
		final AttributeMappingsList attributeMappings = getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping attributeMapping = attributeMappings.get( i );
			if ( attributeMapping.hasPartitionedSelectionMapping() ) {
				return true;
			}
		}
		return false;
	}

	default boolean isAffectedByEnabledFilters(
			Set<ManagedMappingType> visitedTypes,
			LoadQueryInfluencers influencers,
			boolean onlyApplyForLoadByKey) {
		if ( !visitedTypes.add( this ) ) {
			return false;
		}
		// we still need to verify collection fields to be eagerly loaded by join
		final AttributeMappingsList attributeMappings = getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
			if ( mappedFetchOptions.getTiming() == FetchTiming.IMMEDIATE
					&& mappedFetchOptions.getStyle() == FetchStyle.JOIN ) {
				if ( attributeMapping instanceof PluralAttributeMapping ) {
					final CollectionPersister collectionDescriptor = ( (PluralAttributeMapping) attributeMapping ).getCollectionDescriptor();
					if ( collectionDescriptor.isAffectedByEnabledFilters( visitedTypes, influencers, onlyApplyForLoadByKey ) ) {
						return true;
					}
				}
				else if ( attributeMapping instanceof ToOneAttributeMapping ) {
					final EntityMappingType entityMappingType = ( (ToOneAttributeMapping) attributeMapping ).getEntityMappingType();
					if ( entityMappingType.isAffectedByEnabledFilters( visitedTypes, influencers, onlyApplyForLoadByKey ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
