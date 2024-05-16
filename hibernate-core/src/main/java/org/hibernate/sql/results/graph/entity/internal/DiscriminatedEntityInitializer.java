/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogging;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Initializer for discriminated mappings.
 */
public class DiscriminatedEntityInitializer extends AbstractInitializer implements EntityInitializer {
	private static final String CONCRETE_NAME = DiscriminatedEntityInitializer.class.getSimpleName();

	protected final InitializerParent parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;

	private final DomainResultAssembler<?> discriminatorValueAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final DiscriminatedAssociationModelPart fetchedPart;
	private final boolean eager;
	private final boolean resultInitializer;

	// per-row state
	protected EntityPersister concreteDescriptor;
	protected Object entityIdentifier;
	protected Object entityInstance;

	/**
	 * @deprecated Use {@link #DiscriminatedEntityInitializer(InitializerParent, DiscriminatedAssociationModelPart, NavigablePath, Fetch, Fetch, boolean, boolean, AssemblerCreationState)} instead.
	 */
	@Deprecated(forRemoval = true)
	public DiscriminatedEntityInitializer(
			FetchParentAccess parent,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			AssemblerCreationState creationState) {
		this(
				(InitializerParent) parent,
				fetchedPart,
				fetchedNavigable,
				discriminatorFetch,
				keyFetch,
				eager,
				resultInitializer,
				creationState
		);
	}

	public DiscriminatedEntityInitializer(
			InitializerParent parent,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			AssemblerCreationState creationState) {
		this.parent = parent;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.discriminatorValueAssembler = discriminatorFetch.createAssembler( (InitializerParent) this, creationState );
		this.keyValueAssembler = keyFetch.createAssembler( (InitializerParent) this, creationState );
		this.eager = eager;
		this.resultInitializer = resultInitializer;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public @Nullable InitializerParent getParent() {
		return parent;
	}

	public ModelPart getInitializedPart(){
		return fetchedPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			return;
		}

		// resolve the key and the discriminator, and then use those to load the indicated entity

		final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );

		if ( discriminatorValue == null ) {
			state = State.MISSING;
			concreteDescriptor = null;
			entityIdentifier = null;
			entityInstance = null;
			// null association
			assert keyValueAssembler.assemble( rowProcessingState ) == null;
		}
		else {
			state = State.KEY_RESOLVED;
			concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
			entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		state = State.INITIALIZED;

		if ( EntityLoadingLogging.ENTITY_LOADING_LOGGER.isTraceEnabled() ) {
			EntityLoadingLogging.ENTITY_LOADING_LOGGER.tracef(
					"(%s) Beginning Initializer#resolveInstance process for entity (%s) : %s",
					StringHelper.collapse( this.getClass().getName() ),
					getNavigablePath(),
					entityIdentifier
			);
		}
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
			entityInstance = holder.getEntity();
			if ( holder.getEntityInitializer() == null ) {
				if ( entityInstance != null && Hibernate.isInitialized( entityInstance ) ) {
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
				return;
			}
			else if ( entityInstance == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				return;
			}
		}

		entityInstance = rowProcessingState.getSession().internalLoad(
				concreteDescriptor.getEntityName(),
				entityIdentifier,
				eager,
				// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
				false
		);
	}

	@Override
	public void resolveInstance(Object instance) {
		if ( instance == null ) {
			state = State.MISSING;
			entityIdentifier = null;
			concreteDescriptor = null;
			entityInstance = null;
		}
		else {
			final SharedSessionContractImplementor session = rowProcessingState.getSession();
			final LazyInitializer lazyInitializer = extractLazyInitializer( entityInstance );
			if ( lazyInitializer == null ) {
				state = State.INITIALIZED;
				concreteDescriptor = session.getEntityPersister( null, instance );
				entityIdentifier = concreteDescriptor.getIdentifier( instance, session );
			}
			else if ( lazyInitializer.isUninitialized() ) {
				state = eager ? State.RESOLVED : State.INITIALIZED;
				// Read the discriminator from the result set if necessary
				final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );
				concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			else {
				state = State.INITIALIZED;
				concreteDescriptor = session.getEntityPersister( null, lazyInitializer.getImplementation() );
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			entityInstance = instance;
			final Initializer initializer = keyValueAssembler.getInitializer();
			if ( initializer != null ) {
				initializer.resolveInstance( entityIdentifier );
			}
			else if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				discriminatorValueAssembler.resolveState( rowProcessingState );
				keyValueAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance() {
		if ( state != State.RESOLVED ) {
			return;
		}
		state = State.INITIALIZED;
		entityInstance = rowProcessingState.getSession().internalLoad(
				concreteDescriptor.getEntityName(),
				entityIdentifier,
				eager,
				// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
				false
		);
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( instance == null ) {
			state = State.MISSING;
			entityInstance = null;
			entityIdentifier = null;
			concreteDescriptor = null;
		}
		else {
			state = State.INITIALIZED;
			entityInstance = instance;
			// No need to initialize this
			entityIdentifier = null;
			concreteDescriptor = null;
			if ( eager ) {
				Hibernate.initialize( instance );
			}
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		final Initializer initializer = keyValueAssembler.getInitializer();
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
		return resultInitializer;
	}

	@Override
	public String toString() {
		return "DiscriminatedEntityInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
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
