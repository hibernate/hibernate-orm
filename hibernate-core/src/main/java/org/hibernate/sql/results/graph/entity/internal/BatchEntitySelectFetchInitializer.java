/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

public class BatchEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer<BatchEntitySelectFetchInitializer.BatchEntitySelectFetchInitializerData> {
	protected final AttributeMapping[] parentAttributes;
	protected final Setter referencedModelPartSetter;
	protected final Type referencedModelPartType;

	public static class BatchEntitySelectFetchInitializerData extends AbstractBatchEntitySelectFetchInitializerData {
		private HashMap<EntityKey, List<ParentInfo>> toBatchLoad;

		public BatchEntitySelectFetchInitializerData(
				BatchEntitySelectFetchInitializer initializer,
				RowProcessingState rowProcessingState) {
			super( initializer, rowProcessingState );
		}
	}

	public BatchEntitySelectFetchInitializer(
			InitializerParent<?> parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, keyResult, affectedByFilter, creationState );
		parentAttributes = getParentEntityAttributes( referencedModelPart.getAttributeName() );
		referencedModelPartSetter = referencedModelPart.getPropertyAccess().getSetter();
		referencedModelPartType =
				referencedModelPart.findContainingEntityMapping().getEntityPersister()
						.getPropertyType( referencedModelPart.getAttributeName() );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new BatchEntitySelectFetchInitializerData( this, rowProcessingState );
	}

	@Override
	protected void registerResolutionListener(BatchEntitySelectFetchInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final var owningData = owningEntityInitializer.getData( rowProcessingState );
		var toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad == null ) {
			toBatchLoad = data.toBatchLoad = new HashMap<>();
		}
		// Always register the entity key for resolution
		final var parentInfos = toBatchLoad.computeIfAbsent( data.entityKey, key -> new ArrayList<>() );
		// But only add the parent info if the parent entity is not already initialized
		if ( owningData.getState() != State.INITIALIZED ) {
			final var parentAttribute =
					parentAttributes[owningEntityInitializer.getConcreteDescriptor( owningData )
							.getSubclassId()];
			if ( parentAttribute != null ) {
				parentInfos.add( new ParentInfo(
						owningEntityInitializer.getTargetInstance( owningData ),
						parentAttribute.getStateArrayPosition()
				) );
			}
		}
	}

	private static class ParentInfo {
		private final Object parentInstance;
		private final int propertyIndex;

		public ParentInfo(Object parentInstance, int propertyIndex) {
			this.parentInstance = parentInstance;
			this.propertyIndex = propertyIndex;
		}
	}

	@Override
	public void endLoading(BatchEntitySelectFetchInitializerData data) {
		super.endLoading( data );
		final var toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad != null ) {
			final var session = data.getRowProcessingState().getSession();
			final var factory = session.getFactory();
			final var persistenceContext = session.getPersistenceContextInternal();
			for ( var entry : toBatchLoad.entrySet() ) {
				final var entityKey = entry.getKey();
				final var parentInfos = entry.getValue();
				final Object instance = loadInstance( entityKey, toOneMapping, affectedByFilter, session );
				for ( var parentInfo : parentInfos ) {
					final Object parentInstance = parentInfo.parentInstance;
					final var entityEntry = persistenceContext.getEntry( parentInstance );
					referencedModelPartSetter.set( parentInstance, instance );
					final var loadedState = entityEntry.getLoadedState();
					if ( loadedState != null ) {
						loadedState[parentInfo.propertyIndex] =
								referencedModelPartType.deepCopy( instance, factory );
					}
				}
			}
			data.toBatchLoad = null;
		}
	}

	@Override
	public String toString() {
		return "BatchEntitySelectFetchInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}

}
