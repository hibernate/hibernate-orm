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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

public class BatchEntityInsideEmbeddableSelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {
	private Map<EntityKey, List<ParentInfo>> toBatchLoad;
	private final String rootEmbeddablePropertyName;

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

	public BatchEntityInsideEmbeddableSelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );

		rootEmbeddablePropertyName = getRootEmbeddablePropertyName(
				firstEntityInitializer,
				parentAccess,
				referencedModelPart
		);
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( entityKey == null ) {
			return;
		}

		entityInstance = BATCH_PROPERTY;
	}

	@Override
	protected void registerResolutionListener() {
		parentAccess.registerResolutionListener( parentInstance -> {
			final AttributeMapping parentAttribute = getParentEntityAttribute( rootEmbeddablePropertyName );
			if ( parentAttribute != null ) {
				getParentInfos().add( new ParentInfo(
						firstEntityInitializer.getEntityKey(),
						parentInstance,
						parentAttribute.getStateArrayPosition()
				) );
			}
		} );
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
		private final EntityKey initializerEntityKey;
		private final Object parentInstance;
		private final int propertyIndex;

		public ParentInfo(EntityKey initializerEntityKey, Object parentInstance, int propertyIndex) {
			this.initializerEntityKey = initializerEntityKey;
			this.parentInstance = parentInstance;
			this.propertyIndex = propertyIndex;
		}
	}

	@Override
	public void endLoading(ExecutionContext context) {
		if ( toBatchLoad != null ) {
			toBatchLoad.forEach(
					(entityKey, parentInfos) -> {
						final SharedSessionContractImplementor session = context.getSession();
						final Object loadedInstance = loadInstance( entityKey, referencedModelPart, session );
						for ( ParentInfo parentInfo : parentInfos ) {
							final PersistenceContext persistenceContext = session.getPersistenceContext();
							setInstance(
									firstEntityInitializer,
									referencedModelPart,
									rootEmbeddablePropertyName,
									parentInfo.propertyIndex,
									loadedInstance,
									parentInfo.parentInstance,
									parentInfo.initializerEntityKey,
									persistenceContext.getEntry( persistenceContext.getEntity( parentInfo.initializerEntityKey ) ),
									session
							);
						}
					}
			);
			toBatchLoad.clear();
		}
		parentAccess = null;
	}

	protected static void setInstance(
			EntityInitializer entityInitializer,
			ToOneAttributeMapping referencedModelPart,
			String rootEmbeddablePropertyName,
			int propertyIndex,
			Object loadedInstance,
			Object embeddableParentInstance,
			EntityKey parentEntityKey,
			EntityEntry parentEntityEntry,
			SharedSessionContractImplementor session) {
		referencedModelPart.getPropertyAccess().getSetter().set( embeddableParentInstance, loadedInstance );
		updateRootEntityLoadedState(
				entityInitializer,
				rootEmbeddablePropertyName,
				propertyIndex,
				parentEntityKey,
				parentEntityEntry,
				session
		);
	}

	private static void updateRootEntityLoadedState(
			EntityInitializer entityInitializer,
			String rootEmbeddablePropertyName,
			int propertyIndex,
			EntityKey parentEntityKey,
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
					session.getPersistenceContext().getEntity( parentEntityKey ),
					rootEmbeddablePropertyName
			);
			loadedState[propertyIndex] = entityDescriptor.getPropertyType( rootEmbeddablePropertyName )
					.deepCopy( rootEmbeddable, session.getFactory() );
		}
	}

	protected static String getRootEmbeddablePropertyName(
			EntityInitializer firstEntityInitializer,
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart) {
		final NavigablePath entityPath = firstEntityInitializer.getNavigablePath();
		NavigablePath navigablePath = parentAccess.getNavigablePath();
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
