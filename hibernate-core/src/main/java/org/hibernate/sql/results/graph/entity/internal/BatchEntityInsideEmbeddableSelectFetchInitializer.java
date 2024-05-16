/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

public class BatchEntityInsideEmbeddableSelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {
	private Map<EntityKey, List<ParentInfo>> toBatchLoad;
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

		public Object readResolve() {
			return BATCH_PROPERTY;
		}
	};

	/**
	 * @deprecated Use {@link #BatchEntityInsideEmbeddableSelectFetchInitializer(InitializerParent, ToOneAttributeMapping, NavigablePath, EntityPersister, DomainResultAssembler)} instead.
	 */
	@Deprecated(forRemoval = true)
	public BatchEntityInsideEmbeddableSelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		this(
				(InitializerParent) parentAccess,
				referencedModelPart,
				fetchedNavigable,
				concreteDescriptor,
				identifierAssembler
		);
	}

	public BatchEntityInsideEmbeddableSelectFetchInitializer(
			InitializerParent parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );

		this.referencedModelPartSetter = referencedModelPart.getAttributeMetadata().getPropertyAccess().getSetter();
		final String rootEmbeddablePropertyName = getRootEmbeddablePropertyName(
				owningEntityInitializer,
				parentAccess,
				referencedModelPart
		);
		this.rootEmbeddableAttributes = getParentEntityAttributes( rootEmbeddablePropertyName );
		final Getter[] getters = new Getter[rootEmbeddableAttributes.length];
		for ( int i = 0; i < rootEmbeddableAttributes.length; i++ ) {
			if ( rootEmbeddableAttributes[i] != null ) {
				getters[i] = rootEmbeddableAttributes[i].getAttributeMetadata().getPropertyAccess().getGetter();
			}
		}
		this.rootEmbeddableGetters = getters;
		this.rootEmbeddablePropertyTypes = getParentEntityAttributeTypes( rootEmbeddablePropertyName );
	}

	protected Type[] getParentEntityAttributeTypes(String attributeName) {
		final EntityPersister entityDescriptor = owningEntityInitializer.getEntityDescriptor();
		final Type[] attributeTypes = new Type[
				entityDescriptor.getRootEntityDescriptor()
						.getSubclassEntityNames()
						.size()
				];
		initializeAttributeType( attributeTypes, entityDescriptor, attributeName );
		for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
			initializeAttributeType( attributeTypes, subMappingType.getEntityPersister(), attributeName );
		}
		return attributeTypes;
	}

	protected void initializeAttributeType(Type[] attributeTypes, EntityPersister entityDescriptor, String attributeName) {
		if ( rootEmbeddableAttributes[entityDescriptor.getSubclassId()] != null ) {
			attributeTypes[entityDescriptor.getSubclassId()] = entityDescriptor.getPropertyType( attributeName );
		}
	}

	@Override
	protected void registerToBatchFetchQueue(RowProcessingState rowProcessingState) {
		super.registerToBatchFetchQueue( rowProcessingState );
		initializedEntityInstance = BATCH_PROPERTY;
	}

	@Override
	public void initializeInstance() {
		super.initializeInstance();
		// todo: check why this can't be moved to #registerToBatchFetchQueue
		if ( initializedEntityInstance == BATCH_PROPERTY ) {
			final int owningEntitySubclassId = owningEntityInitializer.getConcreteDescriptor().getSubclassId();
			final AttributeMapping rootEmbeddableAttribute = rootEmbeddableAttributes[owningEntitySubclassId];
			if ( rootEmbeddableAttribute != null ) {
				getParentInfos().add( new ParentInfo(
						owningEntityInitializer.getTargetInstance(),
						parent.getInitializedInstance(),
						rootEmbeddableAttribute.getStateArrayPosition(),
						owningEntitySubclassId
				) );
			}
		}
	}

	@Override
	protected void registerResolutionListener() {
	}

	private List<ParentInfo> getParentInfos() {
		if ( toBatchLoad == null ) {
			toBatchLoad = new HashMap<>();
		}
		return toBatchLoad.computeIfAbsent( entityKey, key -> new ArrayList<>() );
	}

	@Override
	public boolean isEntityInitialized() {
		return false;
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
	public void endLoading(ExecutionContext executionContext) {
		super.endLoading( executionContext );
		if ( toBatchLoad != null ) {
			toBatchLoad.forEach(
					(entityKey, parentInfos) -> {
						final SharedSessionContractImplementor session = executionContext.getSession();
						final Object loadedInstance = loadInstance( entityKey, referencedModelPart, session );
						for ( ParentInfo parentInfo : parentInfos ) {
							final PersistenceContext persistenceContext = session.getPersistenceContext();
							final EntityEntry parentEntityEntry = persistenceContext.getEntry( parentInfo.parentEntityInstance );
							referencedModelPartSetter.set( parentInfo.parentInstance, loadedInstance );
							final Object[] loadedState = parentEntityEntry.getLoadedState();
							if ( loadedState != null ) {
								/*
								 E.g.

								 ParentEntity -> RootEmbeddable -> ParentEmbeddable -> toOneAttributeMapping

								 The value of RootEmbeddable is needed to update the ParentEntity loaded state
								 */
								final Object rootEmbeddable = rootEmbeddableGetters[parentInfo.parentEntitySubclassId].get( parentInfo.parentEntityInstance );
								loadedState[parentInfo.propertyIndex] = rootEmbeddablePropertyTypes[parentInfo.parentEntitySubclassId].deepCopy(
										rootEmbeddable,
										session.getFactory()
								);
							}
						}
					}
			);
			toBatchLoad.clear();
		}
	}

	protected static void setInstance(
			EntityInitializer entityInitializer,
			ToOneAttributeMapping referencedModelPart,
			String rootEmbeddablePropertyName,
			int propertyIndex,
			Object loadedInstance,
			Object embeddableParentInstance,
			Object parentEntity,
			EntityEntry parentEntityEntry,
			SharedSessionContractImplementor session) {
		referencedModelPart.getPropertyAccess().getSetter().set( embeddableParentInstance, loadedInstance );
		updateRootEntityLoadedState(
				entityInitializer,
				rootEmbeddablePropertyName,
				propertyIndex,
				parentEntity,
				parentEntityEntry,
				session
		);
	}

	private static void updateRootEntityLoadedState(
			EntityInitializer entityInitializer,
			String rootEmbeddablePropertyName,
			int propertyIndex,
			Object parentEntity,
			EntityEntry parentEntityEntry,
			SharedSessionContractImplementor session) {
		Object[] loadedState = parentEntityEntry.getLoadedState();
		if ( loadedState != null ) {
			/*
			 E.g.

			 ParentEntity -> RootEmbeddable -> ParentEmbeddable -> toOneAttributeMapping

			 The value of RootEmbeddable is needed to update the ParentEntity loaded state
			 */
			final EntityPersister entityDescriptor = entityInitializer.getEntityDescriptor();
			final Object rootEmbeddable = entityDescriptor.getPropertyValue(
					parentEntity,
					rootEmbeddablePropertyName
			);
			loadedState[propertyIndex] = entityDescriptor.getPropertyType( rootEmbeddablePropertyName )
					.deepCopy( rootEmbeddable, session.getFactory() );
		}
	}

	/**
	 * @deprecated Use {@link #getRootEmbeddablePropertyName(EntityInitializer, InitializerParent, ToOneAttributeMapping)} instead.
	 */
	@Deprecated(forRemoval = true)
	protected static String getRootEmbeddablePropertyName(
			EntityInitializer firstEntityInitializer,
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart) {
		return getRootEmbeddablePropertyName(
				firstEntityInitializer,
				(InitializerParent) parentAccess,
				referencedModelPart
		);
	}

	protected static String getRootEmbeddablePropertyName(
			EntityInitializer firstEntityInitializer,
			InitializerParent parent,
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
		return "BatchEntityInsideEmbeddableSelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
