/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
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
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer<Data extends EntitySelectFetchInitializer.EntitySelectFetchInitializerData>
		extends AbstractInitializer<Data> implements EntityInitializer<Data> {
	protected final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final boolean isEnhancedForLazyLoading;

	protected final EntityPersister concreteDescriptor;
	protected final DomainResultAssembler<?> keyAssembler;
	protected final ToOneAttributeMapping toOneMapping;
	protected final boolean affectedByFilter;
	protected final boolean keyIsEager;
	protected final boolean hasLazySubInitializer;

	public static class EntitySelectFetchInitializerData extends InitializerData {
		// per-row state
		protected @Nullable Object entityIdentifier;

		public EntitySelectFetchInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}

		/*
		 * Used by Hibernate Reactive
		 */
		@SuppressWarnings( "unused" )
		public EntitySelectFetchInitializerData(EntitySelectFetchInitializerData original) {
			super( original );
			this.entityIdentifier = original.entityIdentifier;
		}
	}

	public EntitySelectFetchInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( creationState );
		this.parent = parent;
		this.toOneMapping = toOneMapping;
		this.navigablePath = fetchedNavigable;
		this.isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		this.concreteDescriptor = concreteDescriptor;
		this.keyAssembler = keyResult.createResultAssembler( this, creationState );
		this.isEnhancedForLazyLoading =
				concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		this.affectedByFilter = affectedByFilter;
		final Initializer<?> initializer = keyAssembler.getInitializer();
		this.keyIsEager = initializer != null && initializer.isEager();
		this.hasLazySubInitializer =
				initializer != null
					&& ( !initializer.isEager() || initializer.hasLazySubInitializers() );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EntitySelectFetchInitializerData( rowProcessingState );
	}

	public ModelPart getInitializedPart(){
		return toOneMapping;
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveFromPreviousRow(Data data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final Initializer<?> initializer = keyAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(Data data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			data.entityIdentifier = keyAssembler.assemble( data.getRowProcessingState() );
			if ( data.entityIdentifier == null ) {
				data.setState( State.MISSING );
				data.setInstance( null );
			}
			else {
				data.setState( State.INITIALIZED );
				initializeIfNecessary( data );
			}
		}
	}

	@Override
	public void resolveInstance(Object instance, Data data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
			if ( lazyInitializer == null ) {
				data.setState( State.INITIALIZED );
				if ( keyIsEager ) {
					data.entityIdentifier = concreteDescriptor.getIdentifier( instance, rowProcessingState.getSession() );
				}
			}
			else {
				data.setState( lazyInitializer.isUninitialized() ? State.RESOLVED : State.INITIALIZED );
				if ( keyIsEager ) {
					data.entityIdentifier = lazyInitializer.getInternalIdentifier();
				}
			}
			data.setInstance( instance );
			if ( keyIsEager ) {
				final Initializer<?> initializer = keyAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance(Data data) {
		if ( data.getState() == State.RESOLVED ) {
			data.setState( State.INITIALIZED );
			Hibernate.initialize( data.getInstance() );
		}
	}

	protected void initializeIfNecessary(EntitySelectFetchInitializerData data) {
		final boolean found = alreadyInitialized( data );
		data.setState( State.INITIALIZED );
		if ( !found ) {
			initialize( data );
		}
	}

	private void initialize(EntitySelectFetchInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		final var session = rowProcessingState.getSession();
		final Object instance = session.internalLoad(
				concreteDescriptor.getEntityName(),
				data.entityIdentifier,
				true,
				toOneMapping.isInternalLoadNullable()
		);
		data.setInstance( instance );

		if ( instance == null ) {
			handleNotFound( data );
			session.getPersistenceContextInternal().claimEntityHolderIfPossible(
					new EntityKey( data.entityIdentifier, concreteDescriptor ),
					instance,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					this
			);
		}

		final LazyInitializer lazyInitializer = extractLazyInitializer( data.getInstance() );
		if ( lazyInitializer != null ) {
			lazyInitializer.setUnwrap( toOneMapping.isUnwrapProxy() && isEnhancedForLazyLoading );
		}
	}

	void handleNotFound(EntitySelectFetchInitializerData data) {
		final NotFoundAction notFoundAction = toOneMapping.getNotFoundAction(); // can be null
		if ( affectedByFilter ) {
			if ( notFoundAction != NotFoundAction.IGNORE ) {
				throw new EntityFilterException( concreteDescriptor.getEntityName(), data.entityIdentifier,
						toOneMapping.getNavigableRole().getFullPath() );
			}
		}
		else {
			if ( notFoundAction == NotFoundAction.EXCEPTION ) {
				throw new FetchNotFoundException( concreteDescriptor.getEntityName(), data.entityIdentifier );
			}
		}
	}

	private boolean alreadyInitialized(EntitySelectFetchInitializerData data) {
		final var persistenceContext =
				data.getRowProcessingState().getSession()
						.getPersistenceContextInternal();
		final EntityHolder holder =
				persistenceContext.getEntityHolder( new EntityKey( data.entityIdentifier, concreteDescriptor ) );
		if ( holder == null ) {
			return false;
		}
		else {
			data.setInstance( persistenceContext.proxyFor( holder, concreteDescriptor ) ); // note side effect
			if ( holder.getEntityInitializer() == null ) {
				return data.getInstance() != null
					&& Hibernate.isInitialized( data.getInstance() );
			}
			else if ( holder.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				return true;
			}
			else if ( data.getInstance() == null ) {
				// todo: maybe mark this as resolved instead?
				assert holder.getProxy() == null : "How to handle this case?";
				return true;
			}
			else {
				return false;
			}
		}
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, Data data) {
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			data.setState( State.INITIALIZED );
			// No need to initialize this
			data.entityIdentifier = null;
			data.setInstance( instance );
			Hibernate.initialize( instance );
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final Initializer<?> initializer = keyAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public EntityPersister getConcreteDescriptor(Data data) {
		return concreteDescriptor;
	}

	@Override
	public @Nullable Object getEntityIdentifier(Data data) {
		return data.entityIdentifier;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return true;
	}

	@Override
	public void resolveState(EntitySelectFetchInitializerData data) {
		keyAssembler.resolveState( data.getRowProcessingState() );
	}

	@Override
	public boolean hasLazySubInitializers() {
		return hasLazySubInitializer;
	}

	@Override
	public boolean isResultInitializer() {
		return false;
	}

	@Override
	public String toString() {
		return "EntitySelectFetchInitializer(" + toLoggableString( getNavigablePath() ) + ")";
	}

	public DomainResultAssembler<?> getKeyAssembler() {
		return keyAssembler;
	}
}
