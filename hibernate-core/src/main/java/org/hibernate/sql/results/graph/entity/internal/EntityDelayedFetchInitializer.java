/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.BiConsumer;

import org.hibernate.FetchNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.internal.ManagedTypeHelper;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.internal.AbstractInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.internal.log.LoggingHelper.toLoggableString;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl.determineConcreteEntityDescriptor;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityDelayedFetchInitializer
		extends AbstractInitializer<EntityDelayedFetchInitializer.EntityDelayedFetchInitializerData>
		implements EntityInitializer<EntityDelayedFetchInitializer.EntityDelayedFetchInitializerData> {

	private final InitializerParent<?> parent;
	private final NavigablePath navigablePath;
	private final boolean isPartOfKey;
	private final ToOneAttributeMapping referencedModelPart;
	private final boolean selectByUniqueKey;
	private final DomainResultAssembler<?> identifierAssembler;
	private final @Nullable BasicResultAssembler<?> discriminatorAssembler;
	private final boolean keyIsEager;
	private final boolean hasLazySubInitializer;

	public static class EntityDelayedFetchInitializerData extends InitializerData {
		// per-row state
		protected @Nullable Object entityIdentifier;

		public EntityDelayedFetchInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
		}
	}

	public EntityDelayedFetchInitializer(
			InitializerParent<?> parent,
			NavigablePath fetchedNavigable,
			ToOneAttributeMapping referencedModelPart,
			boolean selectByUniqueKey,
			DomainResult<?> keyResult,
			@Nullable BasicFetch<?> discriminatorResult,
			AssemblerCreationState creationState) {
		super( creationState );
		// associations marked with '@NotFound' are ALWAYS eagerly fetched,
		// unless we're resolving the concrete type
		assert !referencedModelPart.hasNotFoundAction()
			|| referencedModelPart.getEntityMappingType().isConcreteProxy();

		this.parent = parent;
		this.navigablePath = fetchedNavigable;
		this.referencedModelPart = referencedModelPart;
		this.selectByUniqueKey = selectByUniqueKey;

		isPartOfKey = Initializer.isPartOfKey( fetchedNavigable, parent );
		identifierAssembler = keyResult.createResultAssembler( this, creationState );
		discriminatorAssembler =
				discriminatorResult == null
						? null
						: (BasicResultAssembler<?>)
								discriminatorResult.createResultAssembler( this, creationState );

		if ( identifierAssembler == null ) {
			keyIsEager = false;
			hasLazySubInitializer = false;
		}
		else {
			keyIsEager = identifierAssembler.isEager();
			hasLazySubInitializer = identifierAssembler.hasLazySubInitializers();
		}
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new EntityDelayedFetchInitializerData( rowProcessingState );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPart getInitializedPart() {
		return referencedModelPart;
	}

	@Override
	public void resolveFromPreviousRow(EntityDelayedFetchInitializerData data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			if ( data.getInstance() == null ) {
				data.setState( State.MISSING );
			}
			else {
				final var initializer = identifierAssembler.getInitializer();
				if ( initializer != null ) {
					initializer.resolveFromPreviousRow( data.getRowProcessingState() );
				}
				data.setState( State.INITIALIZED );
			}
		}
	}

	@Override
	public void resolveInstance(EntityDelayedFetchInitializerData data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );

			final var rowProcessingState = data.getRowProcessingState();
			data.entityIdentifier = identifierAssembler.assemble( rowProcessingState );

			if ( data.entityIdentifier == null ) {
				data.setInstance( null );
				data.setState( State.MISSING );
			}
			else {
				final var entityPersister = getEntityDescriptor();
				final EntityPersister concreteDescriptor;
				if ( discriminatorAssembler != null ) {
					concreteDescriptor = determineConcreteEntityDescriptor(
							rowProcessingState,
							discriminatorAssembler,
							entityPersister
					);
					if ( concreteDescriptor == null ) {
						// If we find no discriminator, it means there's no entity in the target table
						if ( !referencedModelPart.isOptional() ) {
							throw new FetchNotFoundException( entityPersister.getEntityName(), data.entityIdentifier );
						}
						data.setInstance( null );
						data.setState( State.MISSING );
						return;
					}
				}
				else {
					concreteDescriptor = entityPersister;
				}

				if ( selectByUniqueKey ) {
					data.setInstance( instanceWithUniqueKey( data, concreteDescriptor ) );
				}
				else {
					data.setInstance( instanceWithId( data, concreteDescriptor ) );
				}
			}
		}
	}

	private Object instanceWithId(
			EntityDelayedFetchInitializerData data,
			EntityPersister concreteDescriptor) {
		final var rowProcessingState = data.getRowProcessingState();
		final var session = rowProcessingState.getSession();
		final var persistenceContext = session.getPersistenceContextInternal();

		final var entityKey = new EntityKey( data.entityIdentifier, concreteDescriptor );
		final var holder = persistenceContext.getEntityHolder( entityKey );
		if ( holder != null && holder.getEntity() != null ) {
			return persistenceContext.proxyFor( holder, concreteDescriptor );
		}
		// For primary key-based mappings we only use bytecode-laziness if the attribute is optional,
		// because the non-optionality implies that it is safe to have a proxy
		else if ( referencedModelPart.isOptional() && referencedModelPart.isLazy() ) {
			return UNFETCHED_PROPERTY;
		}
		else {
			final Object instance = session.internalLoad(
					concreteDescriptor.getEntityName(),
					data.entityIdentifier,
					false,
					false
			);
			final var lazyInitializer = extractLazyInitializer( instance );
			if ( lazyInitializer != null ) {
				lazyInitializer.setUnwrap(
						referencedModelPart.isUnwrapProxy()
							&& concreteDescriptor.isInstrumented() );
			}
			return instance;
		}
	}

	private Object instanceWithUniqueKey(
			EntityDelayedFetchInitializerData data,
			EntityPersister concreteDescriptor) {
		final var rowProcessingState = data.getRowProcessingState();
		final var session = rowProcessingState.getSession();
		final var persistenceContext = session.getPersistenceContextInternal();

		final String uniqueKeyPropertyName = referencedModelPart.getReferencedPropertyName();
		final var entityUniqueKey =
				new EntityUniqueKey(
						concreteDescriptor.getEntityName(),
						uniqueKeyPropertyName,
						data.entityIdentifier,
						getUniqueKeyPropertyType( concreteDescriptor, session, uniqueKeyPropertyName ),
						session.getFactory()
				);

		Object instance = persistenceContext.getEntity( entityUniqueKey );
		if ( instance == null ) {
			// For unique-key mappings, we always use bytecode-laziness if possible,
			// because we can't generate a proxy based on the unique key yet
			if ( referencedModelPart.isLazy() ) {
				instance = UNFETCHED_PROPERTY;
			}
			else {
				// Try to load a PersistentAttributeInterceptable. If we get one, we can add the lazy
				// field to the interceptor. If we don't get one, we load the entity by unique key.
				final var persistentAttributeInterceptable =
						getPersistentAttributeInterceptable( rowProcessingState );
				if ( persistentAttributeInterceptable != null ) {
					final var persistentAttributeInterceptor =
							(LazyAttributeLoadingInterceptor)
									persistentAttributeInterceptable.$$_hibernate_getInterceptor();
					persistentAttributeInterceptor.addLazyFieldByGraph( navigablePath.getLocalName() );
					instance = UNFETCHED_PROPERTY;
				}
				else {
					instance = concreteDescriptor.loadByUniqueKey(
							uniqueKeyPropertyName,
							data.entityIdentifier,
							session
					);

					// If the entity was not in the Persistence Context, but was found now,
					// add it to the Persistence Context
					if ( instance != null ) {
						persistenceContext.addEntity( entityUniqueKey, instance );
					}
				}
			}
		}
		if ( instance != null ) {
			instance = persistenceContext.proxyFor( instance );
		}
		return instance;
	}

	private PersistentAttributeInterceptable getPersistentAttributeInterceptable(RowProcessingState rowProcessingState) {
		if ( getParent().isEntityInitializer() && isLazyByGraph( rowProcessingState ) ) {
			final Object resolvedInstance =
					getParent().asEntityInitializer()
							.getResolvedInstance( rowProcessingState );
			return ManagedTypeHelper.asPersistentAttributeInterceptableOrNull( resolvedInstance );
		}
		else {
			return null;
		}
	}

	private Type getUniqueKeyPropertyType(EntityPersister concreteDescriptor, SharedSessionContractImplementor session, String uniqueKeyPropertyName) {
		return referencedModelPart.getReferencedPropertyName() == null
				? concreteDescriptor.getIdentifierType()
				: session.getFactory().getRuntimeMetamodels()
						.getReferencedPropertyType( concreteDescriptor.getEntityName(), uniqueKeyPropertyName );
	}

	private boolean isLazyByGraph(RowProcessingState rowProcessingState) {
		final var appliedGraph = rowProcessingState.getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() == GraphSemantic.FETCH ) {
			final var attributeNode = appliedGraph.getGraph().findAttributeNode( navigablePath.getLocalName() );
			return attributeNode == null
				|| attributeNode.getAttributeDescriptor() != getInitializedPart().asAttributeMapping();
		}
		else {
			return false;
		}
	}

	@Override
	public void resolveInstance(Object instance, EntityDelayedFetchInitializerData data) {
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityIdentifier = null;
			data.setInstance( null );
		}
		else {
			// This initializer is done initializing, since this is only invoked for delayed or select initializers
			data.setState( State.INITIALIZED );
			data.setInstance( instance );
			final var rowProcessingState = data.getRowProcessingState();
			if ( keyIsEager ) {
				data.entityIdentifier = getEntityDescriptor().getIdentifier( instance, rowProcessingState.getSession() );
				final var initializer = identifierAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() ) {
				// Resolve the state of the identifier if result caching is enabled, and this is not a query cache hit
				identifierAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	protected void forEachSubInitializer(BiConsumer<Initializer<?>, RowProcessingState> consumer, InitializerData data) {
		final var initializer = identifierAssembler.getInitializer();
		if ( initializer != null ) {
			consumer.accept( initializer, data.getRowProcessingState() );
		}
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return referencedModelPart.getEntityMappingType().getEntityPersister();
	}

	@Override
	public @Nullable InitializerParent<?> getParent() {
		return parent;
	}

	@Override
	public boolean isPartOfKey() {
		return isPartOfKey;
	}

	@Override
	public boolean isEager() {
		return keyIsEager;
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
	public EntityPersister getConcreteDescriptor(EntityDelayedFetchInitializerData data) {
		return getEntityDescriptor();
	}

	@Override
	public void resolveState(EntityDelayedFetchInitializerData data) {
		final var rowProcessingState = data.getRowProcessingState();
		identifierAssembler.resolveState( rowProcessingState );
		if ( discriminatorAssembler != null ) {
			discriminatorAssembler.resolveState( rowProcessingState );
		}
	}

	@Override
	public @Nullable Object getEntityIdentifier(EntityDelayedFetchInitializerData data) {
		return data.entityIdentifier;
	}

	@Override
	public String toString() {
		return "EntityDelayedFetchInitializer("
				+ toLoggableString( navigablePath ) + ")";
	}

	//#########################
	// For Hibernate Reactive
	//#########################

	protected boolean isSelectByUniqueKey() {
		return selectByUniqueKey;
	}

	protected DomainResultAssembler<?> getIdentifierAssembler() {
		return identifierAssembler;
	}

	protected @Nullable BasicResultAssembler<?> getDiscriminatorAssembler() {
		return discriminatorAssembler;
	}
}
