/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
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
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

public abstract class AbstractBatchEntitySelectFetchInitializer extends AbstractInitializer implements EntityInitializer {

	protected final InitializerParent parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> identifierAssembler;
	protected final ToOneAttributeMapping referencedModelPart;
	protected final EntityInitializer owningEntityInitializer;

	// per-row state
	protected @Nullable Object initializedEntityInstance;
	protected @Nullable Object entityIdentifier;
	protected @Nullable EntityKey entityKey;

	/**
	 *
	 * @deprecated Use {@link #AbstractBatchEntitySelectFetchInitializer(InitializerParent, ToOneAttributeMapping, NavigablePath, EntityPersister, DomainResultAssembler)} instead.
	 */
	@Deprecated(forRemoval = true)
	public AbstractBatchEntitySelectFetchInitializer(
			FetchParentAccess parent,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		this(
				(InitializerParent) parent,
				referencedModelPart,
				fetchedNavigable,
				concreteDescriptor,
				identifierAssembler
		);
	}

	public AbstractBatchEntitySelectFetchInitializer(
			InitializerParent parent,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		this.parent = parent;
		this.referencedModelPart = referencedModelPart;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.owningEntityInitializer = Initializer.findOwningEntityInitializer( parent );
		assert owningEntityInitializer != null : "This initializer requires an owning parent entity initializer";
	}

	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isResultInitializer() {
		return false;
	}

	protected abstract void registerResolutionListener();

	@Override
	public void resolveKey() {
		if ( state != State.UNINITIALIZED ) {
			return;
		}

		entityKey = null;
		initializedEntityInstance = null;
		final Initializer initializer = identifierAssembler.getInitializer();
		if ( initializer != null ) {
			initializer.resolveKey();
			entityIdentifier = null;
			state = initializer.getState() == State.MISSING
					? State.MISSING
					: State.KEY_RESOLVED;
		}
		else {
			entityIdentifier = identifierAssembler.assemble( rowProcessingState );
			state = entityIdentifier == null
					? State.MISSING
					: State.KEY_RESOLVED;
		}
	}

	@Override
	public void resolveInstance() {
		if ( state != State.KEY_RESOLVED ) {
			return;
		}

		state = State.RESOLVED;
		if ( entityIdentifier == null ) {
			// entityIdentifier can be null if the identifier is based on an initializer
			entityIdentifier = identifierAssembler.assemble( rowProcessingState );
			if ( entityIdentifier == null ) {
				entityKey = null;
				initializedEntityInstance = null;
				state = State.MISSING;
				return;
			}
		}
		entityKey = new EntityKey( entityIdentifier, concreteDescriptor );
		initializedEntityInstance = getExistingInitializedInstance( rowProcessingState );
		if ( initializedEntityInstance == null ) {
			// need to add the key to the batch queue only when the entity has not been already loaded or
			// there isn't another initializer that is loading it
			registerToBatchFetchQueue( rowProcessingState );
		}
	}

	@Override
	public void resolveInstance(Object instance) {
		if ( instance == null ) {
			state = State.MISSING;
			entityKey = null;
			initializedEntityInstance = null;
			return;
		}
		final Initializer initializer = identifierAssembler.getInitializer();
		// Only need to extract the identifier if the identifier has a many to one
		final boolean hasKeyManyToOne = initializer != null;
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final LazyInitializer lazyInitializer = extractLazyInitializer( instance );
		entityKey = null;
		if ( lazyInitializer == null ) {
			// Entity is initialized
			state = State.INITIALIZED;
			if ( hasKeyManyToOne ) {
				entityIdentifier = concreteDescriptor.getIdentifier( instance, session );
			}
			initializedEntityInstance = instance;
		}
		else if ( lazyInitializer.isUninitialized() ) {
			state = State.RESOLVED;
			if ( hasKeyManyToOne ) {
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			// Resolve and potentially create the entity instance
			registerToBatchFetchQueue( rowProcessingState );
		}
		else {
			// Entity is initialized
			state = State.INITIALIZED;
			if ( hasKeyManyToOne ) {
				entityIdentifier = lazyInitializer.getIdentifier();
			}
			initializedEntityInstance = lazyInitializer.getImplementation();
		}
		if ( hasKeyManyToOne ) {
			initializer.resolveInstance( entityIdentifier );
		}
		else if ( !rowProcessingState.isQueryCacheHit() && rowProcessingState.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE ) {
			// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
			identifierAssembler.resolveState( rowProcessingState );
		}
	}

	protected Object getExistingInitializedInstance(RowProcessingState rowProcessingState) {
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityHolder holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null && holder.getEntity() != null && holder.isEventuallyInitialized() ) {
			return holder.getEntity();
		}
		// we need to register a resolution listener only if there is not an already initialized instance
		// or an instance that another initializer is loading
		registerResolutionListener();
		return null;
	}

	protected void registerToBatchFetchQueue(RowProcessingState rowProcessingState) {
		assert entityKey != null;
		rowProcessingState.getSession().getPersistenceContext()
				.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		// No need to initialize these fields
		entityKey = null;
		initializedEntityInstance = null;
		if ( instance == null ) {
			state = State.MISSING;
		}
		else {
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( instance );
			if ( lazyInitializer != null && lazyInitializer.isUninitialized() ) {
				entityKey = new EntityKey( lazyInitializer.getIdentifier(), concreteDescriptor );
				registerToBatchFetchQueue( rowProcessingState );
			}
			state = State.INITIALIZED;
		}
	}

	@Override
	protected <X> void forEachSubInitializer(BiConsumer<Initializer, X> consumer, X arg) {
		final Initializer initializer = identifierAssembler.getInitializer();
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
		return state == State.RESOLVED || state == State.INITIALIZED ? initializedEntityInstance : null;
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

	protected AttributeMapping[] getParentEntityAttributes(String attributeName) {
		final EntityPersister entityDescriptor = owningEntityInitializer.getEntityDescriptor();
		final AttributeMapping[] parentEntityAttributes = new AttributeMapping[
				entityDescriptor.getRootEntityDescriptor()
						.getSubclassEntityNames()
						.size()
				];
		parentEntityAttributes[entityDescriptor.getSubclassId()] = getParentEntityAttribute(
				entityDescriptor,
				referencedModelPart,
				attributeName
		);
		for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
			parentEntityAttributes[subMappingType.getSubclassId()] = getParentEntityAttribute(
					subMappingType,
					referencedModelPart,
					attributeName
			);
		}
		return parentEntityAttributes;
	}

	protected static AttributeMapping getParentEntityAttribute(
			EntityMappingType subMappingType,
			ToOneAttributeMapping referencedModelPart,
			String attributeName) {
		final AttributeMapping parentAttribute = subMappingType.findAttributeMapping( attributeName );
		if ( parentAttribute != null && parentAttribute.getDeclaringType() == referencedModelPart.getDeclaringType()
				.findContainingEntityMapping() ) {
			// These checks are needed to avoid setting the instance using the wrong (child's) model part or
			// setting it multiple times in case parent and child share the same attribute name for the association.
			return parentAttribute;
		}
		return null;
	}

	@Override
	public FetchParentAccess getFetchParentAccess() {
		return (FetchParentAccess) parent;
	}

	@Override
	public InitializerParent getParent() {
		return parent;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
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
