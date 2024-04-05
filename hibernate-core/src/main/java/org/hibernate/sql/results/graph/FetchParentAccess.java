/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.collection.internal.AbstractImmediateCollectionInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides access to information about the owner/parent of a fetch
 * in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface FetchParentAccess extends Initializer {
	/**
	 * Find the first entity access up the fetch parent graph
	 */
	@Nullable FetchParentAccess findFirstEntityDescriptorAccess();

	default @Nullable EntityInitializer findFirstEntityInitializer() {
		final EntityInitializer entityInitializer = this.asEntityInitializer();
		if ( entityInitializer != null ) {
			return entityInitializer;
		}
		final FetchParentAccess entityDescriptorAccess = findFirstEntityDescriptorAccess();
		return entityDescriptorAccess == null ? null : entityDescriptorAccess.asEntityInitializer();
	}

	@Nullable Object getParentKey();

	NavigablePath getNavigablePath();

	/**
	 * Register a listener to be notified when the parent is "resolved"
	 *
	 * @apiNote If already resolved, the callback is triggered immediately
	 */
	void registerResolutionListener(Consumer<Object> resolvedParentConsumer);

	default @Nullable FetchParentAccess getFetchParentAccess() {
		return null;
	}

	@Nullable FetchParentAccess getOwningParent();

	static @Nullable FetchParentAccess determineOwningParent(@Nullable FetchParentAccess parentAccess) {
		if ( parentAccess == null
				|| parentAccess.isEntityInitializer()
				|| parentAccess.isCollectionInitializer()
				|| parentAccess.isEmbeddableInitializer() ) {
			return parentAccess;
		}
		return parentAccess.getOwningParent();
	}

	@Nullable EntityMappingType getOwnedModelPartDeclaringType();

	static @Nullable EntityMappingType determineOwnedModelPartDeclaringType(
			ModelPart modelPart,
			@Nullable FetchParentAccess parentAccess,
			@Nullable FetchParentAccess owningParent) {
		final EntityInitializer entityInitializer = owningParent != null ?
				owningParent.findFirstEntityInitializer() :
				null;
		if ( entityInitializer == null ) {
			return null;
		}

		while ( parentAccess != null && parentAccess != owningParent ) {
			modelPart = parentAccess.getInitializedPart();
			parentAccess = parentAccess.getFetchParentAccess();
		}
		if ( modelPart != null && entityInitializer.getEntityDescriptor().getEntityMetamodel().isPolymorphic() ) {
			return modelPart.asAttributeMapping() != null ?
					modelPart.asAttributeMapping().getDeclaringType().findContainingEntityMapping() :
					modelPart.asEntityMappingType();
		}
		return null;
	}

	default boolean shouldSkipInitializer(RowProcessingState rowProcessingState) {
		if ( isPartOfKey() ) {
			// We can never skip an initializer if it is part of a key
			return false;
		}

		FetchParentAccess owningParent = getOwningParent();
		if ( owningParent != null ) {
			if ( owningParent instanceof AbstractImmediateCollectionInitializer ) {
				final AbstractImmediateCollectionInitializer collectionInitializer = (AbstractImmediateCollectionInitializer) owningParent;
				// If this initializer is owned by an immediate collection initializer,
				// skipping only depends on whether the collection key is resolvable or not
				return collectionInitializer.resolveCollectionKey( rowProcessingState ) == null;
			}
			EntityInitializer entityInitializer = owningParent.asEntityInitializer();
			if ( entityInitializer == null ) {
				final EmbeddableInitializer embeddableInitializer = owningParent.asEmbeddableInitializer();
				assert embeddableInitializer != null;
				final EmbeddableMappingType descriptor = embeddableInitializer.getInitializedPart()
						.getEmbeddableTypeDescriptor();
				if ( descriptor.isPolymorphic() ) {
					// The embeddable is polymorphic, check if the current subtype defines the initialized attribute
					final AttributeMapping attribute = getInitializedPart().asAttributeMapping();
					if ( attribute != null ) {
						embeddableInitializer.resolveKey( rowProcessingState );
						final String embeddableClassName = descriptor.getDiscriminatorMapping()
								.resolveDiscriminatorValue( embeddableInitializer.getDiscriminatorValue() )
								.getIndicatedEntityName();
						if ( !descriptor.declaresAttribute( embeddableClassName, attribute ) ) {
							return true;
						}
					}
				}
				if ( embeddableInitializer.isResultInitializer() ) {
					// We can never skip an initializer if it is part of an embeddable domain result,
					// because that embeddable always has to be materialized with its full state
					return false;
				}
				else if ( ( entityInitializer = embeddableInitializer.findFirstEntityInitializer() ) == null ) {
					return false;
				}
			}
			// We must resolve the key of the parent in order to determine the concrete descriptor
			entityInitializer.resolveKey( rowProcessingState );
			final EntityPersister concreteDescriptor = entityInitializer.getConcreteDescriptor();
			if ( concreteDescriptor == null ) {
				// Skip processing this initializer if the parent owning initializer is missing
				return true;
			}
			// We can skip if the parent is either null or already initialized,
			if ( ( entityInitializer.getEntityKey() == null || entityInitializer.isEntityInitialized() )
					// but only if the query cache put does not depend on the initializer accessing JdbcValues.
					// If result caching is disabled, there are no dependencies
					&& rowProcessingState.getQueryOptions().isResultCachingEnabled() != Boolean.TRUE ) {
				return true;
			}
			final EntityMappingType declaringType = getOwnedModelPartDeclaringType();
			if ( declaringType != null && concreteDescriptor != declaringType ) {
				// Skip the initializer if the declaring type is not a super type
				// of the parent entity initializer's concrete type
				return !declaringType.getSubclassEntityNames().contains( concreteDescriptor.getEntityName() );
			}
		}
		return false;
	}
}
