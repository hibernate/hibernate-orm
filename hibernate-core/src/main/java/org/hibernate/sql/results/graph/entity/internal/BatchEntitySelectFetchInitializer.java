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
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.type.Type;

public class BatchEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {
	protected final AttributeMapping[] parentAttributes;
	protected final Setter referencedModelPartSetter;
	protected final Type referencedModelPartType;

	private Map<EntityKey, List<ParentInfo>> toBatchLoad;

	/**
	 * @deprecated Use {@link #BatchEntitySelectFetchInitializer(InitializerParent, ToOneAttributeMapping, NavigablePath, EntityPersister, DomainResultAssembler)} instead.
	 */
	@Deprecated(forRemoval = true)
	public BatchEntitySelectFetchInitializer(
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

	public BatchEntitySelectFetchInitializer(
			InitializerParent parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );
		this.parentAttributes = getParentEntityAttributes( referencedModelPart.getAttributeName() );
		this.referencedModelPartSetter = referencedModelPart.getPropertyAccess().getSetter();
		this.referencedModelPartType = owningEntityInitializer.getEntityDescriptor().getPropertyType( referencedModelPart.getAttributeName() );
	}

	@Override
	protected void registerResolutionListener() {
		final AttributeMapping parentAttribute;
		if ( !owningEntityInitializer.isEntityInitialized() && ( parentAttribute = parentAttributes[owningEntityInitializer.getConcreteDescriptor().getSubclassId()] ) != null ) {
			getParentInfos().add( new ParentInfo( owningEntityInitializer.getTargetInstance(), parentAttribute.getStateArrayPosition() ) );
		}
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
		private final Object parentInstance;
		private final int propertyIndex;

		public ParentInfo(Object parentInstance, int propertyIndex) {
			this.parentInstance = parentInstance;
			this.propertyIndex = propertyIndex;
		}
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		super.endLoading( executionContext );
		if ( toBatchLoad != null ) {
			toBatchLoad.forEach(
					(entityKey, parentInfos) -> {
						final SharedSessionContractImplementor session = executionContext.getSession();
						final Object instance = loadInstance( entityKey, referencedModelPart, session );
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
			toBatchLoad.clear();
		}
	}

	@Override
	public String toString() {
		return "BatchEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
