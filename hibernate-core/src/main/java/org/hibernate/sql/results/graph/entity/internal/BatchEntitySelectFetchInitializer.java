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
		private Map<EntityKey, List<ParentInfo>> toBatchLoad;

		public BatchEntitySelectFetchInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
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
		return new BatchEntitySelectFetchInitializerData( rowProcessingState );
	}

	@Override
	protected void registerResolutionListener(BatchEntitySelectFetchInitializerData data) {
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		final InitializerData owningData = owningEntityInitializer.getData( rowProcessingState );
		final AttributeMapping parentAttribute;
		if ( owningData.getState() != State.INITIALIZED
				&& ( parentAttribute = parentAttributes[owningEntityInitializer.getConcreteDescriptor( owningData ).getSubclassId()] ) != null ) {
			if ( data.toBatchLoad == null ) {
				data.toBatchLoad = new HashMap<>();
			}
			data.toBatchLoad.computeIfAbsent( data.entityKey, key -> new ArrayList<>() ).add(
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
		if ( data.toBatchLoad != null ) {
			data.toBatchLoad.forEach(
					(entityKey, parentInfos) -> {
						final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
						final Object instance = loadInstance( entityKey, toOneMapping, affectedByFilter, session );
						for ( ParentInfo parentInfo : parentInfos ) {
							final Object parentInstance = parentInfo.parentInstance;
							final EntityEntry entry = session.getPersistenceContext().getEntry( parentInstance );
							referencedModelPartSetter.set( parentInstance, instance );
							final Object[] loadedState = entry.getLoadedState();
							if ( loadedState != null ) {
								loadedState[parentInfo.propertyIndex] = referencedModelPartType.deepCopy(
										instance,
										session.getFactory()
								);
							}
						}
					}
			);
			data.toBatchLoad.clear();
		}
	}

	@Override
	public String toString() {
		return "BatchEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
