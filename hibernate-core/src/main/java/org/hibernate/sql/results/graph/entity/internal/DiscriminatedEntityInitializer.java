/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Initializer for discriminated mappings.
 */
public class DiscriminatedEntityInitializer
		extends AbstractInitializer<DiscriminatedEntityInitializer.DiscriminatedEntityInitializerData>
		implements EntityInitializer<DiscriminatedEntityInitializer.DiscriminatedEntityInitializerData> {

	protected final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;

	private final DomainResultAssembler<?> discriminatorValueAssembler;
	private final DomainResultAssembler<?> keyValueAssembler;
	private final DiscriminatedAssociationModelPart fetchedPart;
	private final boolean eager;
	private final boolean resultInitializer;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;
	protected final boolean isReadOnly;

	public static class DiscriminatedEntityInitializerData extends InitializerData {
		protected EntityPersister concreteDescriptor;
		protected Object entityIdentifier;

		public DiscriminatedEntityInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public DiscriminatedEntityInitializer(
			InitializerParent<?> parent,
			DiscriminatedAssociationModelPart fetchedPart,
			NavigablePath fetchedNavigable,
			Fetch discriminatorFetch,
			Fetch keyFetch,
			boolean eager,
			boolean resultInitializer,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.fetchedPart = fetchedPart;
		this.navigablePath = fetchedNavigable;
		this.eager = eager;
		this.resultInitializer = resultInitializer;

		isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		discriminatorValueAssembler = discriminatorFetch.createAssembler( this, creationState );
		keyValueAssembler = keyFetch.createAssembler( this, creationState );

		keyIsEager = keyValueAssembler.isEager();
		hasLazySubInitializer = keyValueAssembler.hasLazySubInitializers();
		isReadOnly = isReadOnly( fetchedPart );
	}

	private static boolean isReadOnly(DiscriminatedAssociationModelPart fetchedPart) {
		final BasicValuedModelPart keyPart = fetchedPart.getKeyPart();
		final DiscriminatorMapping discriminatorMapping = fetchedPart.getDiscriminatorMapping();
		return !keyPart.isInsertable()
			&& !keyPart.isUpdateable()
			&& !discriminatorMapping.isInsertable()
			&& !discriminatorMapping.isUpdateable();
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new DiscriminatedEntityInitializerData( rowProcessingState );
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
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
	public void resolveKey(DiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			// resolve the key and the discriminator, and then use those to load the indicated entity

			final var rowProcessingState = data.getRowProcessingState();
			final Object discriminatorValue =
					discriminatorValueAssembler.assemble( rowProcessingState );
			if ( discriminatorValue == null ) {
				data.setState( State.MISSING );
				data.concreteDescriptor = null;
				data.entityIdentifier = null;
				data.setInstance( null );
				// null association
				assert keyValueAssembler.assemble( rowProcessingState ) == null;
			}
			else {
				data.setState( State.KEY_RESOLVED );
				data.concreteDescriptor =
						fetchedPart.resolveDiscriminatorValue( discriminatorValue )
								.getEntityPersister();
				data.entityIdentifier = keyValueAssembler.assemble( rowProcessingState );
			}
		}
	}

	@Override
	public void resolveState(DiscriminatedEntityInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		discriminatorValueAssembler.resolveState( rowProcessingState );
		keyValueAssembler.resolveState( rowProcessingState );
	}

	@Override
	public void resolveFromPreviousRow(DiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final var initializer = keyValueAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(DiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			data.setState( State.INITIALIZED );
			final var session = data.getRowProcessingState().getSession();
			final Object identifier = data.entityIdentifier;
			final var concreteDescriptor = data.concreteDescriptor;
			final var entityKey = new EntityKey( identifier, concreteDescriptor );
			final var persistenceContext = session.getPersistenceContextInternal();
			final var holder = persistenceContext.getEntityHolder( entityKey );
			final Object instance;
			if ( holder != null ) {
				instance = holder.getEntity();
				data.setInstance( instance );
			}
			else {
				instance = null;
			}
			if ( !isResolved( holder, instance ) ) {
				data.setInstance( session.internalLoad(
						concreteDescriptor.getEntityName(),
						identifier,
						eager,
						// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
						false
				) );
			}
		}
	}

	private boolean isResolved(EntityHolder holder, Object instance) {
		if ( holder == null ) {
			return false;
		}
		else {
			final var initializer = holder.getEntityInitializer();
			if ( initializer == null ) {
				return instance != null && Hibernate.isInitialized( instance );
			}
			else if ( initializer != this ) {
				// the entity is already being loaded elsewhere
				return true;
			}
			else if ( instance == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				return true;
			}
			else {
				return false;
			}
		}
	}

	protected boolean resolveIdentifier(Object instance, DiscriminatedEntityInitializerData data) {
		final boolean identifierResolved;
		if ( instance == null && !isReadOnly ) {
			data.entityIdentifier = null;
			identifierResolved = true;
		}
		else if ( isReadOnly ) {
			// When the mapping is read-only, we can't trust the state of the persistence context
			resolveKey( data );
			identifierResolved = true;
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final LazyInitializer lazyInitializer = extractLazyInitializer( instance );
			if ( lazyInitializer == null ) {
				data.setState( State.INITIALIZED );
				data.concreteDescriptor = session.getEntityPersister( null, instance );
				data.entityIdentifier = data.concreteDescriptor.getIdentifier( instance, session );
				identifierResolved = false;
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( eager ? State.RESOLVED : State.INITIALIZED );
				// Read the discriminator from the result set if necessary
				final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState );
				data.concreteDescriptor = fetchedPart.resolveDiscriminatorValue( discriminatorValue ).getEntityPersister();
				data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				identifierResolved = true;
			}
			else {
				data.setState( State.INITIALIZED );
				data.concreteDescriptor = session.getEntityPersister( null, lazyInitializer.getImplementation() );
				data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				identifierResolved = false;
			}
			assert data.entityIdentifier != null;
		}
		return identifierResolved;
	}

	@Override
	public void resolveInstance(Object instance, DiscriminatedEntityInitializerData data) {
		final boolean identifierResolved = resolveIdentifier( instance, data );
		if ( data.entityIdentifier == null ) {
			data.setState( Initializer.State.MISSING );
			data.concreteDescriptor = null;
			data.setInstance( null );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var entityKey = new EntityKey( data.entityIdentifier, data.concreteDescriptor );
			final var entityHolder = session.getPersistenceContextInternal().getEntityHolder(
					entityKey
			);

			if ( entityHolder == null || instance == null
				|| entityHolder.getEntity() != instance && entityHolder.getProxy() != instance ) {
				// the existing entity instance is detached or transient
				if ( entityHolder != null ) {
					final var managed = entityHolder.getManagedObject();
					data.setInstance( managed );
					data.entityIdentifier = entityHolder.getEntityKey().getIdentifier();
					data.setState( !eager || entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
				}
				else {
					data.setState( State.RESOLVED );
					initializeInstance( data );
				}
			}
			else {
				data.setInstance( instance );
			}

			if ( keyIsEager && !identifierResolved ) {
				final Initializer<?> initializer = keyValueAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
				if ( rowProcessingState.needsResolveState() ) {
					discriminatorValueAssembler.resolveState( rowProcessingState );
				}
			}
			else if ( rowProcessingState.needsResolveState() && !identifierResolved ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyValueAssembler.resolveState( rowProcessingState );
				discriminatorValueAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance(DiscriminatedEntityInitializerData data) {
		if ( data.getState() == State.RESOLVED ) {
			data.setState( State.INITIALIZED );
			data.setInstance( data.getRowProcessingState().getSession()
					.internalLoad(
							data.concreteDescriptor.getEntityName(),
							data.entityIdentifier,
							eager,
							// should not be null since we checked already.
							// null would indicate bad data (ala, not-found handling)
							false
					) );
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, DiscriminatedEntityInitializerData data) {
		final var attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance =
				attributeMapping != null
						? attributeMapping.getValue( parentInstance )
						: parentInstance;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.setInstance( null );
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
		}
		else {
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			// No need to initialize this
			data.entityIdentifier = null;
			data.concreteDescriptor = null;
			if ( eager ) {
				Hibernate.initialize( instance );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var initializer = keyValueAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		throw new UnsupportedOperationException("Discriminated association has no static entity type");
	}

	@Override
	public EntityPersister getConcreteDescriptor(DiscriminatedEntityInitializerData data) {
		return data.concreteDescriptor;
	}

	@Override
	public @Nullable Object getEntityIdentifier(DiscriminatedEntityInitializerData data) {
		return data.entityIdentifier;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return eager || keyIsEager;
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	@Override
	public boolean isResultInitializer() {
		return resultInitializer;
	}

	@Override
	public String toString() {
		return "DiscriminatedEntityInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}

}
