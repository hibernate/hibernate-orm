/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogger;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {
	private static final String CONCRETE_NAME = EntitySelectFetchInitializer.class.getSimpleName();

	private FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final boolean isEnhancedForLazyLoading;
	private final boolean nullable;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler identifierAssembler;
	private final ToOneAttributeMapping referencedModelPart;

	protected Object entityInstance;

	public EntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler,
			boolean nullable) {
		this.parentAccess = parentAccess;
		this.referencedModelPart = referencedModelPart;
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.nullable = nullable;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	public ModelPart getInitializedPart(){
		return referencedModelPart;
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
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		List<AttributeMapping> attributeMappings;
		if ( parentAccess instanceof EmbeddableInitializer ) {
			attributeMappings = ( (EmbeddableInitializer) parentAccess ).getInitializedPart()
					.getEmbeddableTypeDescriptor()
					.getAttributeMappings();
		}
		else {
			attributeMappings = ( (EntityInitializer) parentAccess ).getConcreteDescriptor().getAttributeMappings();
		}

		if ( !attributeMappings.contains( referencedModelPart ) ) {
			return;
		}

		if ( entityInstance != null ) {
			return;
		}

		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );

		if ( entityIdentifier == null ) {
			return;
		}

		if ( EntityLoadingLogger.TRACE_ENABLED ) {
			EntityLoadingLogger.LOGGER.tracef(
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
		if ( entityInstance != null ) {
			return;
		}

		Initializer initializer = rowProcessingState.getJdbcValuesSourceProcessingState().findInitializer(
				entityKey );

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
			return;
		}

		final LoadingEntityEntry existingLoadingEntry = session
				.getPersistenceContext()
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						CONCRETE_NAME,
						toLoggableString(
								getNavigablePath(),
								entityIdentifier
						)
				);
			}
			this.entityInstance = existingLoadingEntry.getEntityInstance();

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

				// EARLY EXIT!!!
				return;
			}
		}

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.LOGGER.debugf(
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
				nullable
		);

		if ( EntityLoadingLogger.DEBUG_ENABLED ) {
			EntityLoadingLogger.LOGGER.debugf(
					"(%s) Entity [%s] : %s has being loaded by session.internalLoad.",
					CONCRETE_NAME,
					toLoggableString( getNavigablePath(), entityIdentifier ),
					entityIdentifier
			);
		}

		final boolean unwrapProxy = referencedModelPart.isUnwrapProxy() && isEnhancedForLazyLoading;
		if ( entityInstance instanceof HibernateProxy ) {
			( (HibernateProxy) entityInstance ).getHibernateLazyInitializer().setUnwrap( unwrapProxy );
		}
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
		throw new NotYetImplementedFor6Exception( getClass() );
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
}
