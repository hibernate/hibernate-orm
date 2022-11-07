/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

public abstract class AbstractBatchEntitySelectFetchInitializer extends AbstractFetchParentAccess
		implements EntityInitializer {

	protected FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler identifierAssembler;
	protected final ToOneAttributeMapping referencedModelPart;

	protected Object entityInstance;
	protected EntityKey entityKey;


	private boolean isInitialized;

	public AbstractBatchEntitySelectFetchInitializer(
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
		if ( isInitialized ) {
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			return;
		}

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
			if ( EntityLoadingLogging.DEBUG_ENABLED ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Found an initializer for entity (%s) : %s",
						getConcreteName(),
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
				if ( EntityLoadingLogging.DEBUG_ENABLED ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							getConcreteName(),
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
		addParentInfo();

		isInitialized = true;
	}

	protected abstract String getConcreteName();

	protected abstract void addParentInfo();

	protected boolean isAttributeAssignableToConcreteDescriptor() {
		if ( parentAccess instanceof EntityInitializer ) {
			final AbstractEntityPersister concreteDescriptor = (AbstractEntityPersister) ( (EntityInitializer) parentAccess ).getConcreteDescriptor();
			if ( concreteDescriptor.isPolymorphic() ) {
				final AbstractEntityPersister declaringType = (AbstractEntityPersister) referencedModelPart.getDeclaringType();
				if ( concreteDescriptor != declaringType ) {
					if ( !declaringType.getSubclassEntityNames().contains( concreteDescriptor.getName() ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		clearResolutionListeners();
		isInitialized = false;
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

	protected static Object loadInstance(
			EntityKey entityKey,
			ToOneAttributeMapping referencedModelPart,
			SharedSessionContractImplementor session) {
		return session.internalLoad(
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				true,
				referencedModelPart.isInternalLoadNullable()
		);
	}

	protected static int getPropertyIndex(FetchParentAccess parentAccess, String propertyName) {
		return parentAccess.findFirstEntityInitializer().getEntityDescriptor().getPropertyIndex( propertyName );
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

}
