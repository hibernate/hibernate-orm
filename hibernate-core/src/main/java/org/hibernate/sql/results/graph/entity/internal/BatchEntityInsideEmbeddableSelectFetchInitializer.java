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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

public class BatchEntityInsideEmbeddableSelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer {
	private static final String CONCRETE_NAME = BatchEntityInsideEmbeddableSelectFetchInitializer.class.getSimpleName();

	/*
	 Object[0] will contain the parent EntityKey and Object[1] the parent embeddable instance,
	 */
	private Map<EntityKey, List<Object[]>> toBatchLoad = new HashMap<>();

	public BatchEntityInsideEmbeddableSelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler) {
		super( parentAccess, referencedModelPart, fetchedNavigable, concreteDescriptor, identifierAssembler );
	}

	@Override
	protected String getConcreteName() {
		return CONCRETE_NAME;
	}

	@Override
	protected void addParentInfo() {
		final List<Object[]> parents = getBatchInfos();
		parentAccess.registerResolutionListener(
				o ->
						parents.add(
								new Object[] {
										parentAccess.findFirstEntityInitializer().getEntityKey(),
										o
								}
						)
		);
	}

	private List<Object[]> getBatchInfos() {
		List<Object[]> objects = toBatchLoad.get( entityKey );
		if ( objects == null ) {
			objects = new ArrayList<>();
			toBatchLoad.put( entityKey, objects );
		}
		return objects;
	}

	@Override
	public void endLoading(ExecutionContext context) {
		final EntityInitializer entityInitializer = parentAccess.findFirstEntityInitializer();
		final String rootEmbeddablePropertyName = getRootEmbeddablePropertyName();
		final int rootEmbeddablePropertyIndex = getPropertyIndex( parentAccess, rootEmbeddablePropertyName );
		toBatchLoad.forEach(
				(entityKey, parentInfos) -> {
					final SharedSessionContractImplementor session = context.getSession();
					final Object loadedInstance = loadInstance( entityKey, referencedModelPart, session );
					for ( Object[] parentInfo : parentInfos ) {
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						setInstance(
								entityInitializer,
								referencedModelPart,
								rootEmbeddablePropertyName,
								rootEmbeddablePropertyIndex,
								loadedInstance,
								parentInfo[1],
								(EntityKey) parentInfo[0],
								persistenceContext.getEntry( persistenceContext.getEntity( (EntityKey) parentInfo[0] ) ),
								session
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

	protected String getRootEmbeddablePropertyName() {
		final NavigablePath entityPath = parentAccess.findFirstEntityDescriptorAccess().getNavigablePath();
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
