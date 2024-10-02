/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

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
import org.hibernate.property.access.spi.Setter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

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
		this.parentAttributes = getParentEntityAttributes( referencedModelPart.getAttributeName() );
		this.referencedModelPartSetter = referencedModelPart.getPropertyAccess().getSetter();
		this.referencedModelPartType = referencedModelPart.findContainingEntityMapping().getEntityPersister()
				.getPropertyType( referencedModelPart.getAttributeName() );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new BatchEntitySelectFetchInitializerData( this, rowProcessingState );
	}

	@Override
	protected void registerResolutionListener(BatchEntitySelectFetchInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final InitializerData owningData = owningEntityInitializer.getData( rowProcessingState );
		HashMap<EntityKey, List<ParentInfo>> toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad == null ) {
			toBatchLoad = data.toBatchLoad = new HashMap<>();
		}
		// Always register the entity key for resolution
		final List<ParentInfo> parentInfos = toBatchLoad.computeIfAbsent( data.entityKey, key -> new ArrayList<>() );
		final AttributeMapping parentAttribute;
		// But only add the parent info if the parent entity is not already initialized
		if ( owningData.getState() != State.INITIALIZED
				&& ( parentAttribute = parentAttributes[owningEntityInitializer.getConcreteDescriptor( owningData ).getSubclassId()] ) != null ) {
			parentInfos.add(
					new ParentInfo(
							owningEntityInitializer.getTargetInstance( owningData ),
							parentAttribute.getStateArrayPosition()
					)
			);
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
		final HashMap<EntityKey, List<ParentInfo>> toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad != null ) {
			final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			for ( Map.Entry<EntityKey, List<ParentInfo>> entry : toBatchLoad.entrySet() ) {
				final EntityKey entityKey = entry.getKey();
				final List<ParentInfo> parentInfos = entry.getValue();
				final Object instance = loadInstance( entityKey, toOneMapping, affectedByFilter, session );
				for ( ParentInfo parentInfo : parentInfos ) {
					final Object parentInstance = parentInfo.parentInstance;
					final EntityEntry entityEntry = persistenceContext.getEntry( parentInstance );
					referencedModelPartSetter.set( parentInstance, instance );
					final Object[] loadedState = entityEntry.getLoadedState();
					if ( loadedState != null ) {
						loadedState[parentInfo.propertyIndex] = referencedModelPartType.deepCopy(
								instance,
								session.getFactory()
						);
					}
				}
			}
			data.toBatchLoad = null;
		}
	}

	@Override
	public String toString() {
		return "BatchEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
