/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

public class BatchEntityInsideEmbeddableSelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer<BatchEntityInsideEmbeddableSelectFetchInitializer.BatchEntityInsideEmbeddableSelectFetchInitializerData> {
	protected final Setter referencedModelPartSetter;
	protected final AttributeMapping[] rootEmbeddableAttributes;
	protected final Getter[] rootEmbeddableGetters;
	protected final Type[] rootEmbeddablePropertyTypes;

	/**
	 * Marker value for batch properties, needed by the EmbeddableInitializer to instantiate the
	 * embeddable instance in case all the other properties are null.
	 */
	public static final Serializable BATCH_PROPERTY = new Serializable() {
		@Override
		public String toString() {
			return "<batch>";
		}
		@Serial
		public Object readResolve() {
			return BATCH_PROPERTY;
		}
	};

	public static class BatchEntityInsideEmbeddableSelectFetchInitializerData extends AbstractBatchEntitySelectFetchInitializerData {
		private HashMap<EntityKey, List<ParentInfo>> toBatchLoad;

		public BatchEntityInsideEmbeddableSelectFetchInitializerData(
				BatchEntityInsideEmbeddableSelectFetchInitializer initializer,
				RowProcessingState rowProcessingState) {
			super( initializer, rowProcessingState );
		}
	}

	public BatchEntityInsideEmbeddableSelectFetchInitializer(
			InitializerParent<?> parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, keyResult, affectedByFilter, creationState );

		referencedModelPartSetter = referencedModelPart.getAttributeMetadata().getPropertyAccess().getSetter();
		final String rootEmbeddablePropertyName =
				getRootEmbeddablePropertyName( owningEntityInitializer, parentAccess, referencedModelPart );
		rootEmbeddableAttributes = getParentEntityAttributes( rootEmbeddablePropertyName );
		final var getters = new Getter[rootEmbeddableAttributes.length];
		for ( int i = 0; i < rootEmbeddableAttributes.length; i++ ) {
			if ( rootEmbeddableAttributes[i] != null ) {
				getters[i] = rootEmbeddableAttributes[i].getAttributeMetadata().getPropertyAccess().getGetter();
			}
		}
		rootEmbeddableGetters = getters;
		rootEmbeddablePropertyTypes = getParentEntityAttributeTypes( rootEmbeddablePropertyName );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new BatchEntityInsideEmbeddableSelectFetchInitializerData( this, rowProcessingState );
	}

	protected Type[] getParentEntityAttributeTypes(String attributeName) {
		final var entityDescriptor = owningEntityInitializer.getEntityDescriptor();
		final int size =
				entityDescriptor.getRootEntityDescriptor()
						.getSubclassEntityNames().size();
		final var attributeTypes = new Type[size];
		initializeAttributeType( attributeTypes, entityDescriptor, attributeName );
		for ( var subMappingType : entityDescriptor.getSubMappingTypes() ) {
			initializeAttributeType( attributeTypes, subMappingType.getEntityPersister(), attributeName );
		}
		return attributeTypes;
	}

	protected void initializeAttributeType(Type[] attributeTypes, EntityPersister entityDescriptor, String attributeName) {
		final int subclassId = entityDescriptor.getSubclassId();
		if ( rootEmbeddableAttributes[subclassId] != null ) {
			attributeTypes[subclassId] = entityDescriptor.getPropertyType( attributeName );
		}
	}

	@Override
	protected void registerToBatchFetchQueue(BatchEntityInsideEmbeddableSelectFetchInitializerData data) {
		super.registerToBatchFetchQueue( data );
		data.setInstance( BATCH_PROPERTY );
	}

	@Override
	public void initializeInstance(BatchEntityInsideEmbeddableSelectFetchInitializerData data) {
		super.initializeInstance( data );
		// todo: check why this can't be moved to #registerToBatchFetchQueue
		if ( data.getInstance() == BATCH_PROPERTY ) {
			final var rowProcessingState = data.getRowProcessingState();
			final var owningData = owningEntityInitializer.getData( rowProcessingState );
			final int owningEntitySubclassId =
					owningEntityInitializer.getConcreteDescriptor( owningData )
							.getSubclassId();
			final var rootEmbeddableAttribute = rootEmbeddableAttributes[owningEntitySubclassId];
			if ( rootEmbeddableAttribute != null ) {
				var toBatchLoad = data.toBatchLoad;
				if ( toBatchLoad == null ) {
					toBatchLoad = data.toBatchLoad = new HashMap<>();
				}
				toBatchLoad.computeIfAbsent( data.entityKey, key -> new ArrayList<>() )
						.add( new ParentInfo(
								owningEntityInitializer.getTargetInstance( owningData ),
								parent.getResolvedInstance( rowProcessingState ),
								rootEmbeddableAttribute.getStateArrayPosition(),
								owningEntitySubclassId
						) );
			}
		}
	}

	@Override
	protected void registerResolutionListener(BatchEntityInsideEmbeddableSelectFetchInitializerData data) {
	}

	private static class ParentInfo {
		private final Object parentEntityInstance;
		private final Object parentInstance;
		private final int propertyIndex;
		private final int parentEntitySubclassId;

		public ParentInfo(
				Object parentEntityInstance,
				Object parentInstance,
				int propertyIndex,
				int parentEntitySubclassId) {
			this.parentEntityInstance = parentEntityInstance;
			this.parentInstance = parentInstance;
			this.propertyIndex = propertyIndex;
			this.parentEntitySubclassId = parentEntitySubclassId;
		}
	}

	@Override
	public void endLoading(BatchEntityInsideEmbeddableSelectFetchInitializerData data) {
		super.endLoading( data );
		final var toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad != null ) {
			for ( var entry : toBatchLoad.entrySet() ) {
				final var entityKey = entry.getKey();
				final var parentInfos = entry.getValue();
				final var session = data.getRowProcessingState().getSession();
				final var factory = session.getFactory();
				final var persistenceContext = session.getPersistenceContextInternal();
				final Object loadedInstance = loadInstance( entityKey, toOneMapping, affectedByFilter, session );
				for ( ParentInfo parentInfo : parentInfos ) {
					final Object parentEntityInstance = parentInfo.parentEntityInstance;
					final var parentEntityEntry = persistenceContext.getEntry( parentEntityInstance );
					referencedModelPartSetter.set( parentInfo.parentInstance, loadedInstance );
					final var loadedState = parentEntityEntry.getLoadedState();
					if ( loadedState != null ) {
						/*
						E.g.

						ParentEntity -> RootEmbeddable -> ParentEmbeddable -> toOneAttributeMapping

						The value of RootEmbeddable is needed to update the ParentEntity loaded state
						 */
						final int parentEntitySubclassId = parentInfo.parentEntitySubclassId;
						final Object rootEmbeddable =
								rootEmbeddableGetters[parentEntitySubclassId]
										.get( parentEntityInstance );
						loadedState[parentInfo.propertyIndex] =
								rootEmbeddablePropertyTypes[parentEntitySubclassId]
										.deepCopy( rootEmbeddable, factory );
					}
				}
			}
			data.toBatchLoad = null;
		}
	}

	protected static String getRootEmbeddablePropertyName(
			EntityInitializer<?> firstEntityInitializer,
			InitializerParent<?> parent,
			ToOneAttributeMapping referencedModelPart) {
		final NavigablePath entityPath = firstEntityInitializer.getNavigablePath();
		NavigablePath navigablePath = parent.getNavigablePath();
		if ( navigablePath == entityPath ) {
			return referencedModelPart.getPartName();
		}
		while ( navigablePath.getParent() != entityPath ) {
			navigablePath = navigablePath.getParent();
		}
		return navigablePath.getLocalName();
	}

	@Override
	public String toString() {
		return "BatchEntityInsideEmbeddableSelectFetchInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}

}
