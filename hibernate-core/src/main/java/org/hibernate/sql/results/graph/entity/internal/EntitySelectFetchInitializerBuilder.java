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
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;

import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.BATCH_INITIALIZE;
import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.BATCH_LOAD;
import static org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializerBuilder.BatchMode.NONE;

public class EntitySelectFetchInitializerBuilder {

	public static AbstractFetchParentAccess createInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping fetchedAttribute,
			EntityPersister entityPersister,
			DomainResult<?> keyResult,
			NavigablePath navigablePath,
			boolean selectByUniqueKey,
			AssemblerCreationState creationState) {
		if ( selectByUniqueKey ) {
			return new EntitySelectFetchByUniqueKeyInitializer(
					parentAccess,
					fetchedAttribute,
					navigablePath,
					entityPersister,
					keyResult.createResultAssembler( parentAccess, creationState )
			);
		}
		final BatchMode batchMode = determineBatchMode( entityPersister, parentAccess, creationState );
		switch ( batchMode ) {
			case NONE:
				return new EntitySelectFetchInitializer(
						parentAccess,
						fetchedAttribute,
						navigablePath,
						entityPersister,
						keyResult.createResultAssembler( parentAccess, creationState )
				);
			case BATCH_LOAD:
				if ( parentAccess.isEmbeddableInitializer() ) {
					return new BatchEntityInsideEmbeddableSelectFetchInitializer(
							parentAccess,
							fetchedAttribute,
							navigablePath,
							entityPersister,
							keyResult.createResultAssembler( parentAccess, creationState )
					);
				}
				else {
					return new BatchEntitySelectFetchInitializer(
							parentAccess,
							fetchedAttribute,
							navigablePath,
							entityPersister,
							keyResult.createResultAssembler( parentAccess, creationState )
					);
				}
			case BATCH_INITIALIZE:
				return new BatchInitializeEntitySelectFetchInitializer(
						parentAccess,
						fetchedAttribute,
						navigablePath,
						entityPersister,
						keyResult.createResultAssembler( parentAccess, creationState )
				);
		}
		throw new IllegalStateException( "Should be unreachable" );
	}

	private static BatchMode determineBatchMode(
			EntityPersister entityPersister,
			FetchParentAccess parentAccess,
			AssemblerCreationState creationState) {
		if ( creationState.isScrollResult()
				|| !creationState.getExecutionContext()
						.getSession()
						.getLoadQueryInfluencers()
						.effectivelyBatchLoadable( entityPersister ) ) {
			return NONE;
		}
		else if ( creationState.isDynamicInstantiation() ) {
			return BatchMode.BATCH_INITIALIZE;
		}
		while ( parentAccess.isEmbeddableInitializer() ) {
			final EmbeddableInitializer embeddableInitializer = parentAccess.asEmbeddableInitializer();
			final EmbeddableValuedModelPart initializedPart = embeddableInitializer.getInitializedPart();
			// For entity identifier mappings we can't batch load,
			// because the entity identifier needs the instance in the resolveKey phase,
			// but batch loading is inherently executed out of order
			if ( initializedPart.isEntityIdentifierMapping()
					// todo: check if the virtual check is necessary
					|| initializedPart.isVirtual()
					// If the parent embeddable has a custom instantiator,
					// we can't inject entities later through setValues()
					|| !( initializedPart.getMappedType().getRepresentationStrategy().getInstantiator()
								instanceof StandardEmbeddableInstantiator ) ) {
				return entityPersister.hasSubclasses() ? NONE : BATCH_INITIALIZE;
			}
			parentAccess = parentAccess.getFetchParentAccess();
			if ( parentAccess == null ) {
				break;
			}
		}
		if ( parentAccess != null ) {
			assert parentAccess.getInitializedPart() instanceof EntityValuedModelPart;
			final EntityPersister parentPersister = parentAccess.asEntityInitializer().getEntityDescriptor();
			final EntityDataAccess cacheAccess = parentPersister.getCacheAccessStrategy();
			if ( cacheAccess != null ) {
				// Do batch initialization instead of batch loading if the parent entity is cacheable
				// to avoid putting entity state into the cache at a point when the association is not yet set
				return BATCH_INITIALIZE;
			}
		}
		return BATCH_LOAD;
	}

	enum BatchMode {
		NONE,
		BATCH_LOAD,
		BATCH_INITIALIZE
	}

}
