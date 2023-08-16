/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.LoadContexts;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {
	private static final String CONCRETE_NAME = EntitySelectFetchInitializer.class.getSimpleName();

	protected final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final boolean isEnhancedForLazyLoading;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> keyAssembler;
	private final ToOneAttributeMapping toOneMapping;

	protected boolean isInitialized;

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return parentAccess;
	}

	protected Object entityInstance;

	public EntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> keyAssembler) {
		this.parentAccess = parentAccess;
		this.toOneMapping = toOneMapping;
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.keyAssembler = keyAssembler;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	public ModelPart getInitializedPart(){
		return toOneMapping;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		// Defer the select by default to the initialize phase
		// We only need to select in this phase if this is part of an identifier or foreign key
		NavigablePath np = navigablePath.getParent();
		while ( np != null ) {
			if ( np instanceof EntityIdentifierNavigablePath
					|| ForeignKeyDescriptor.PART_NAME.equals( np.getLocalName() )
					|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( np.getLocalName() )) {

				initializeInstance( rowProcessingState );
				return;
			}
			np = np.getParent();
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null || isInitialized ) {
			return;
		}

		final EntityInitializer parentEntityInitializer = parentAccess.findFirstEntityInitializer();
		if ( parentEntityInitializer != null && parentEntityInitializer.isEntityInitialized() ) {
			isInitialized = true;
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			return;
		}

		final Object entityIdentifier = keyAssembler.assemble( rowProcessingState );

		if ( entityIdentifier == null ) {
			isInitialized = true;
			return;
		}

		if ( EntityLoadingLogging.TRACE_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final String entityName = concreteDescriptor.getEntityName();

		final EntityKey entityKey = new EntityKey( entityIdentifier, concreteDescriptor );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		entityInstance = persistenceContext.getEntity( entityKey );
		if ( entityInstance != null && Hibernate.isInitialized( entityInstance )) {
			isInitialized = true;
			return;
		}

		final LoadContexts loadContexts = session.getPersistenceContext().getLoadContexts();
		final LoadingEntityEntry existingLoadingEntry = loadContexts.findLoadingEntityEntry( entityKey );

		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogging.DEBUG_ENABLED ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						CONCRETE_NAME,
						toLoggableString(
								getNavigablePath(),
								entityIdentifier
						)
				);
			}
			this.entityInstance = existingLoadingEntry.getEntityInstance();

			final EntityInitializer entityInitializer = existingLoadingEntry.getEntityInitializer();
			if ( entityInitializer != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogging.DEBUG_ENABLED ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							CONCRETE_NAME,
							toLoggableString( getNavigablePath(), entityIdentifier ),
							entityInitializer
					);
				}

				// EARLY EXIT!!!
				isInitialized = true;
				return;
			}
			else {
				if ( entityInstance == null ) {
					isInitialized = true;
					return;
				}
			}
		}

		if ( EntityLoadingLogging.DEBUG_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Invoking session#internalLoad for entity (%s) : %s",
					CONCRETE_NAME,
					toLoggableString( getNavigablePath(), entityIdentifier ),
					entityIdentifier
			);
		}
		entityInstance = session.internalLoad(
				entityName,
				entityIdentifier,
				true,
				toOneMapping.isInternalLoadNullable()
		);

		if ( entityInstance == null ) {
			if ( toOneMapping.getNotFoundAction() == NotFoundAction.EXCEPTION ) {
				throw new FetchNotFoundException( entityName, entityIdentifier );
			}
			rowProcessingState.getJdbcValuesSourceProcessingState()
					.registerLoadingEntity(
							entityKey,
							new LoadingEntityEntry( this, entityKey, concreteDescriptor, entityInstance )
					);
		}

		if ( EntityLoadingLogging.DEBUG_ENABLED ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
					"(%s) Entity [%s] : %s has being loaded by session.internalLoad.",
					CONCRETE_NAME,
					toLoggableString( getNavigablePath(), entityIdentifier ),
					entityIdentifier
			);
		}

		final boolean unwrapProxy = toOneMapping.isUnwrapProxy() && isEnhancedForLazyLoading;
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entityInstance );
		if ( lazyInitializer != null ) {
			lazyInitializer.setUnwrap( unwrapProxy );
		}

		isInitialized = true;
	}

	protected boolean isAttributeAssignableToConcreteDescriptor() {
		return isAttributeAssignableToConcreteDescriptor( parentAccess, toOneMapping );
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		isInitialized = false;
		clearResolutionListeners();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEntityInitialized() {
		return isInitialized;
	}

	@Override
	public Object getParentKey() {
		return parentAccess.getParentKey();
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
}
