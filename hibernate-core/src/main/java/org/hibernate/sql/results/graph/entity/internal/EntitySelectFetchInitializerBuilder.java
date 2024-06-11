/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.metamodel.internal.StandardEmbeddableInstantiator;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.BATCH_INITIALIZE;
import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.BATCH_LOAD;
import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.NONE;

public class EntitySelectFetchInitializerBuilder {

	public static EntityInitializer<?> createInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping fetchedAttribute,
			EntityPersister entityPersister,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		if ( selectByUniqueKey ) {
			return new EntitySelectFetchByUniqueKeyInitializer(
					parent,
					fetchedAttribute,
					navigablePath,
					entityPersister,
					keyResult,
					affectedByFilter,
					creationState
			);
		}
		if ( !parent.isEntityInitializer() && parent.findOwningEntityInitializer() == null ) {
			// Batch initializers require an owning parent initializer
			return new EntitySelectFetchInitializer<>(
					parent,
					fetchedAttribute,
					navigablePath,
					entityPersister,
					keyResult,
					affectedByFilter,
					creationState
			);
		}
		final BatchMode batchMode = determineBatchMode( entityPersister, parent, creationState );
		switch ( batchMode ) {
			case NONE:
				return new EntitySelectFetchInitializer<>(
						parent,
						fetchedAttribute,
						navigablePath,
						entityPersister,
						keyResult,
						affectedByFilter,
						creationState
				);
			case BATCH_LOAD:
				if ( parent.isEmbeddableInitializer() ) {
					return new BatchEntityInsideEmbeddableSelectFetchInitializer(
							parent,
							fetchedAttribute,
							navigablePath,
							entityPersister,
							keyResult,
							affectedByFilter,
							creationState
					);
				}
				else {
					return new BatchEntitySelectFetchInitializer(
							parent,
							fetchedAttribute,
							navigablePath,
							entityPersister,
							keyResult,
							affectedByFilter,
							creationState
					);
				}
			case BATCH_INITIALIZE:
				return new BatchInitializeEntitySelectFetchInitializer(
						parent,
						fetchedAttribute,
						navigablePath,
						entityPersister,
						keyResult,
						affectedByFilter,
						creationState
				);
		}
		throw new IllegalStateException( "Should be unreachable" );
	}

	private static BatchMode determineBatchMode(
			EntityPersister entityPersister,
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		if ( !entityPersister.isBatchLoadable() ) {
			return NONE;
		}
		if ( creationState.isDynamicInstantiation() ) {
			if ( canBatchInitializeBeUsed( entityPersister ) ) {
				return BatchMode.BATCH_INITIALIZE;
			}
			return NONE;
		}
		while ( parent.isEmbeddableInitializer() ) {
			final EmbeddableInitializer<?> embeddableInitializer = parent.asEmbeddableInitializer();
			final EmbeddableValuedModelPart initializedPart = embeddableInitializer.getInitializedPart();
			// For entity identifier mappings we can't batch load,
			// because the entity identifier needs the instance in the resolveKey phase,
			// but batch loading is inherently executed out of order
			if ( initializedPart.isEntityIdentifierMapping()
					// todo: check if the virtual check is necessary
					|| initializedPart.isVirtual()
					|| initializedPart.getMappedType().isPolymorphic()
					// If the parent embeddable has a custom instantiator,
					// we can't inject entities later through setValues()
					|| !( initializedPart.getMappedType().getRepresentationStrategy().getInstantiator()
								instanceof StandardEmbeddableInstantiator ) ) {
				return entityPersister.hasSubclasses() ? NONE : BATCH_INITIALIZE;
			}
			parent = parent.getParent();
			if ( parent == null ) {
				break;
			}
		}
		if ( parent != null ) {
			assert parent.getInitializedPart() instanceof EntityValuedModelPart;
			final EntityPersister parentPersister = parent.asEntityInitializer().getEntityDescriptor();
			final EntityDataAccess cacheAccess = parentPersister.getCacheAccessStrategy();
			if ( cacheAccess != null ) {
				// Do batch initialization instead of batch loading if the parent entity is cacheable
				// to avoid putting entity state into the cache at a point when the association is not yet set
				if ( canBatchInitializeBeUsed( entityPersister ) ) {
					return BATCH_INITIALIZE;
				}
				return NONE;
			}
		}
		return BATCH_LOAD;
	}

	private static boolean canBatchInitializeBeUsed(EntityPersister entityPersister) {
		//  we need to create a Proxy in order to use batch initialize
		return entityPersister.getRepresentationStrategy().getProxyFactory() != null;
	}

	enum BatchMode {
		NONE,
		BATCH_LOAD,
		BATCH_INITIALIZE
	}

}
