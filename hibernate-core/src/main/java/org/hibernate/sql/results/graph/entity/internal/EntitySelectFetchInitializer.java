/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer extends AbstractInitializer implements EntityInitializer {
	private static final String CONCRETE_NAME = EntitySelectFetchInitializer.class.getSimpleName();

	protected final InitializerParent parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final boolean isEnhancedForLazyLoading;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> keyAssembler;
	private final ToOneAttributeMapping toOneMapping;

	// per-row state
	protected Object entityIdentifier;
	protected Object entityInstance;

	/**
	 * @deprecated Use {@link #EntitySelectFetchInitializer(InitializerParent, ToOneAttributeMapping, NavigablePath, EntityPersister, DomainResultAssembler)} instead.
	 */
	@Deprecated(forRemoval = true)
	public EntitySelectFetchInitializer(
			FetchParentAccess parent,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> keyAssembler) {
		this( (InitializerParent) parent, toOneMapping, fetchedNavigable, concreteDescriptor, keyAssembler );
	}

	public EntitySelectFetchInitializer(
			InitializerParent parent,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> keyAssembler) {
		this.parent = parent;
		this.toOneMapping = toOneMapping;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.concreteDescriptor = concreteDescriptor;
		this.keyAssembler = keyAssembler;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
	}

	public ModelPart getInitializedPart(){
		return toOneMapping;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public @Nullable InitializerParent getParent() {
		return parent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		entityIdentifier = keyAssembler.assemble( rowProcessingState );

		if ( entityIdentifier == null ) {
			state = State.MISSING;
			entityInstance = null;
			return;
		}
		state = State.INITIALIZED;
		initialize( rowProcessingState );
	}

	@Override
	public void resolveInstance(Object instance) {
		if ( instance == null ) {
			state = State.MISSING;
			entityIdentifier = null;
			entityInstance = null;
		}
		else {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
			if ( lazyInitializer == null ) {
				state = State.INITIALIZED;
				entityIdentifier = concreteDescriptor.getIdentifier( instance, session );
			}
			else if ( lazyInitializer.isUninitialized() ) {
				state = State.RESOLVED;
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			else {
				state = State.INITIALIZED;
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			entityInstance = instance;
			final Initializer initializer = keyAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveInstance( entityIdentifier );
			}
			else if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance() {
		if ( state != State.RESOLVED ) {
			return;
		}
		state = State.INITIALIZED;
		Hibernate.initialize( entityInstance );
	}

	protected void initialize(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final EntityKey entityKey = new EntityKey( entityIdentifier, concreteDescriptor );

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null ) {
			if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
				EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						CONCRETE_NAME,
						toLoggableString(
								getNavigablePath(),
								entityIdentifier
						)
				);
			}
			entityInstance = persistenceContext.proxyFor( holder, concreteDescriptor );
			if ( holder.getEntityInitializer() == null ) {
				if ( entityInstance != null && Hibernate.isInitialized( entityInstance ) ) {
					state = State.INITIALIZED;
					return;
				}
			}
			else if ( holder.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
					EntityLoadingLogging.ENTITY_LOADING_LOGGER.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							CONCRETE_NAME,
							toLoggableString( getNavigablePath(), entityIdentifier ),
							holder.getEntityInitializer()
					);
				}
				state = State.INITIALIZED;
				return;
			}
			else if ( entityInstance == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				state = State.INITIALIZED;
				return;
			}
		}
		state = State.INITIALIZED;
		final String entityName = concreteDescriptor.getEntityName();

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
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
			rowProcessingState.getSession().getPersistenceContextInternal().claimEntityHolderIfPossible(
					new EntityKey( entityIdentifier, concreteDescriptor ),
					entityInstance,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
		}

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isDebugEnabled() ) {
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
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( instance == null ) {
			state = State.MISSING;
			entityIdentifier = null;
			entityInstance = null;
		}
		else {
			state = State.INITIALIZED;
			// No need to initialize this
			entityIdentifier = null;
			entityInstance = instance;
			Hibernate.initialize( instance );
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		final Initializer initializer = keyAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, arg );
		}
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
	public boolean isEntityInitialized() {
		return state == State.INITIALIZED;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isResultInitializer() {
		return false;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

	@Override
	public EntityKey getEntityKey() {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}

	@Override
	public Object getParentKey() {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}
}
