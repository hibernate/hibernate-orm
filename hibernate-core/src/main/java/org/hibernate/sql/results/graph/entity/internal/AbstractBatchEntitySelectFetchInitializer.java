/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

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
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractBatchEntitySelectFetchInitializer implements EntityInitializer {

	protected final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final FetchParentAccess owningParent;
	private final EntityMappingType ownedModelPartDeclaringType;
	private final boolean isPartOfKey;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> identifierAssembler;
	protected final ToOneAttributeMapping referencedModelPart;
	protected final EntityInitializer firstEntityInitializer;

	protected boolean parentShallowCached;

	// per-row state
	protected Object initializedEntityInstance;
	protected EntityKey entityKey;

	protected State state = State.UNINITIALIZED;

	public AbstractBatchEntitySelectFetchInitializer(
			FetchParentAccess parentAccess,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler<?> identifierAssembler) {
		this.parentAccess = parentAccess;
		this.referencedModelPart = referencedModelPart;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parentAccess );
		this.owningParent = FetchParentAccess.determineOwningParent( parentAccess );
		this.ownedModelPartDeclaringType = FetchParentAccess.determineOwnedModelPartDeclaringType( referencedModelPart, parentAccess, owningParent );
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.firstEntityInitializer = parentAccess.findFirstEntityInitializer();
		assert firstEntityInitializer != null : "This initializer requires parentAccess.findFirstEntityInitializer() to not be null";
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

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
	}

	protected abstract void registerResolutionListener();

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( state != State.UNINITIALIZED ) {
			return;
		}
		if ( parentShallowCached || shouldSkipInitializer( rowProcessingState ) ) {
			state = State.MISSING;
			return;
		}

		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			state = State.MISSING;
		}
		else {
			entityKey = new EntityKey( entityIdentifier, concreteDescriptor );
			state = State.KEY_RESOLVED;
		}
	}

	protected Object getExistingInitializedInstance(RowProcessingState rowProcessingState) {
		assert entityKey != null;
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
	public void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( instance );
		if ( lazyInitializer != null && lazyInitializer.isUninitialized() ) {
			entityKey = new EntityKey( lazyInitializer.getIdentifier(), concreteDescriptor );
			registerToBatchFetchQueue( rowProcessingState );
		}
		state = State.INITIALIZED;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		initializedEntityInstance = null;
		entityKey = null;
		state = State.UNINITIALIZED;
	}

	@Override
	public void markShallowCached() {
		parentShallowCached = true;
	}

	@Override
	public void endLoading(ExecutionContext executionContext) {
		parentShallowCached = false;
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return initializedEntityInstance;
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

	protected AttributeMapping getParentEntityAttribute(String attributeName) {
		final AttributeMapping parentAttribute = firstEntityInitializer.getConcreteDescriptor()
				.findAttributeMapping( attributeName );
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
		return parentAccess;
	}

	@Override
	public @Nullable FetchParentAccess getOwningParent() {
		return owningParent;
	}

	@Override
	public @Nullable EntityMappingType getOwnedModelPartDeclaringType() {
		return ownedModelPartDeclaringType;
	}

	@Override
	public EntityPersister getConcreteDescriptor() {
		return concreteDescriptor;
	}

	protected enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		INITIALIZED
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

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		throw new UnsupportedOperationException(
				"This should never happen, because this initializer has not child initializers" );
	}

}
