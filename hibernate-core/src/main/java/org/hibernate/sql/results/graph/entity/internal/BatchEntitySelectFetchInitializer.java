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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

public class BatchEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {
	private final Map<EntityKey, List<ParentInfo>> toBatchLoad = new HashMap<>();

	public BatchEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );
	}

	@Override
	protected void registerResolutionListener() {
		final List<ParentInfo> parents = getParentInfos();
		parentAccess.registerResolutionListener(
				o ->
						parents.add(
								new ParentInfo(
										o,
										getPropertyIndex( firstEntityInitializer, referencedModelPart.getPartName() )
								)
						)
		);
	}

	private List<ParentInfo> getParentInfos() {
		List<ParentInfo> objects = toBatchLoad.get( entityKey );
		if ( objects == null ) {
			objects = new ArrayList<>();
			toBatchLoad.put( entityKey, objects );
		}
		return objects;
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
	public void endLoading(ExecutionContext context) {
		toBatchLoad.forEach(
				(entityKey, parentInfos) -> {
					final SharedSessionContractImplementor session = context.getSession();
					final Object instance = loadInstance( entityKey, referencedModelPart, session );
					for ( ParentInfo parentInfo : parentInfos ) {
						final Object parentInstance = parentInfo.parentInstance;
						setInstance(
								firstEntityInitializer,
								referencedModelPart,
								referencedModelPart.getPartName(),
								parentInfo.propertyIndex,
								session,
								instance,
								parentInstance,
								session.getPersistenceContext().getEntry( parentInstance )
						);
					}
				}
		);
		toBatchLoad.clear();
		parentAccess = null;
	}

	protected static void setInstance(
			EntityInitializer entityInitializer,
			ToOneAttributeMapping referencedModelPart,
			String propertyName,
			int propertyIndex,
			SharedSessionContractImplementor session,
			Object instance,
			Object parentInstance,
			EntityEntry entry) {
		referencedModelPart.getPropertyAccess().getSetter().set( parentInstance, instance );
		updateParentEntityLoadedState( entityInitializer, propertyName, propertyIndex, session, instance, entry );
	}

	private static void updateParentEntityLoadedState(
			EntityInitializer entityInitializer,
			String propertyName,
			int propertyIndex,
			SharedSessionContractImplementor session,
			Object instance,
			EntityEntry entry) {
		final Object[] loadedState = entry.getLoadedState();
		if ( loadedState != null ) {
			loadedState[propertyIndex] = entityInitializer.getEntityDescriptor().getPropertyType( propertyName )
					.deepCopy( instance, session.getFactory() );
		}
	}

	@Override
	public String toString() {
		return "BatchEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
