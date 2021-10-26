/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.AbstractEntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogger;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

public class BatchEntitySelectFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {
	private static final String CONCRETE_NAME = BatchEntitySelectFetchInitializer.class.getSimpleName();

	private FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler identifierAssembler;
	private final ToOneAttributeMapping referencedModelPart;

	protected Object entityInstance;
	private EntityKey entityKey;

	private Map<EntityKey, Object> toBatchLoad = new LinkedHashMap<>();

	public BatchEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler) {
		this.parentAccess = parentAccess;
		this.referencedModelPart = referencedModelPart;
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
	}

	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {


	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {

	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			return;
		}
		entityKey = new EntityKey( entityIdentifier, concreteDescriptor );

		final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContextInternal();
		entityInstance = persistenceContext.getEntity( entityKey );
		if ( entityInstance != null ) {
			return;
		}
		Initializer initializer = rowProcessingState.getJdbcValuesSourceProcessingState()
				.findInitializer( entityKey );

		if ( initializer != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%s) Found an initializer for entity (%s) : %s",
						CONCRETE_NAME,
						toLoggableString( getNavigablePath(), entityIdentifier ),
						entityIdentifier
				);
			}
			initializer.resolveInstance( rowProcessingState );
			entityInstance = initializer.getInitializedInstance();
			// EARLY EXIT!!!
			return;
		}

		final LoadingEntityEntry existingLoadingEntry = rowProcessingState.getSession()
				.getPersistenceContext()
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		if ( existingLoadingEntry != null ) {
			if ( existingLoadingEntry.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogger.DEBUG_ENABLED ) {
					EntityLoadingLogger.LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							CONCRETE_NAME,
							toLoggableString( getNavigablePath(), entityIdentifier ),
							existingLoadingEntry.getEntityInitializer()
					);
				}
				this.entityInstance = existingLoadingEntry.getEntityInstance();

				// EARLY EXIT!!!
				return;
			}
		}

		persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
		toBatchLoad.put( entityKey, parentAccess.getInitializedInstance() );
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		clearParentResolutionListeners();
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Object getParentKey() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstance != null ) {
			listener.accept( entityInstance );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	@Override
	public void endLoading(ExecutionContext context) {
		toBatchLoad.forEach(
				(entityKey, parentInstance) -> {
					final Object instance = context.getSession().internalLoad(
							entityKey.getEntityName(),
							entityKey.getIdentifier(),
							true,
							referencedModelPart.isNullable()
					);
					if ( instance != null ) {
						( (AbstractEntityPersister) referencedModelPart.getDeclaringType() ).setPropertyValue(
								parentInstance,
								referencedModelPart.getPartName(),
								instance
						);
						final EntityEntry entry = context.getSession()
								.getPersistenceContext()
								.getEntry( parentInstance );
						final int propertyIndex = ( (UniqueKeyLoadable) ( (AbstractEntityInitializer) parentAccess ).getEntityDescriptor() ).getPropertyIndex(
								referencedModelPart.getPartName() );
						if ( entry != null ) {
							final Object[] loadedState = entry.getLoadedState();
							if ( loadedState != null ) {
								loadedState[propertyIndex] = instance;
							}
						}
					}
				}
		);
		toBatchLoad = null;
		parentAccess = null;
	}
}
