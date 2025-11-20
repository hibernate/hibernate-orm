/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;
import static org.hibernate.sql.results.graph.entity.internal.EntityInitializerImpl.getAttributeInterceptor;

public abstract class AbstractBatchEntitySelectFetchInitializer<Data extends AbstractBatchEntitySelectFetchInitializer.AbstractBatchEntitySelectFetchInitializerData>
		extends EntitySelectFetchInitializer<Data> implements EntityInitializer<Data> {

	protected final EntityInitializer<InitializerData> owningEntityInitializer;

	public static abstract class AbstractBatchEntitySelectFetchInitializerData extends EntitySelectFetchInitializerData {
		final boolean batchDisabled;

		// per-row state
		protected @Nullable EntityKey entityKey;

		public AbstractBatchEntitySelectFetchInitializerData(
				AbstractBatchEntitySelectFetchInitializer<?> initializer,
				RowProcessingState rowProcessingState) {
			super( initializer, rowProcessingState );
			batchDisabled = isBatchDisabled( initializer, rowProcessingState );
		}

		private static boolean isBatchDisabled(
				AbstractBatchEntitySelectFetchInitializer<?> initializer,
				RowProcessingState rowProcessingState) {
			return rowProcessingState.isScrollResult()
				|| !rowProcessingState.getLoadQueryInfluencers()
					.effectivelyBatchLoadable( initializer.toOneMapping.getEntityMappingType().getEntityPersister() );
		}
	}

	public AbstractBatchEntitySelectFetchInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping toOneMapping,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( parent, toOneMapping, fetchedNavigable, concreteDescriptor, keyResult, affectedByFilter, creationState );
		//noinspection unchecked
		owningEntityInitializer =
				(EntityInitializer<InitializerData>)
						Initializer.findOwningEntityInitializer( parent );
		assert owningEntityInitializer != null : "This initializer requires an owning parent entity initializer";
	}

	protected abstract void registerResolutionListener(Data data);

	@Override
	public void resolveKey(Data data) {
		if ( data.getState() == State.UNINITIALIZED ) {
			data.entityKey = null;
			data.setInstance( null );
			final var rowProcessingState = data.getRowProcessingState();
			//noinspection unchecked
			final var initializer = (Initializer<InitializerData>) keyAssembler.getInitializer();
			if ( initializer != null ) {
				final var subData = initializer.getData( rowProcessingState );
				initializer.resolveKey( subData );
				data.entityIdentifier = null;
				data.setState( subData.getState() == State.MISSING ? State.MISSING : State.KEY_RESOLVED );
			}
			else {
				data.entityIdentifier = keyAssembler.assemble( rowProcessingState );
				data.setState( data.entityIdentifier == null ? State.MISSING : State.KEY_RESOLVED );
			}
		}
	}

	@Override
	public void resolveInstance(Data data) {
		if ( data.getState() == State.KEY_RESOLVED ) {
			data.setState( State.RESOLVED );
			final var rowProcessingState = data.getRowProcessingState();
			if ( data.entityIdentifier == null ) {
				// entityIdentifier can be null if the identifier is based on an initializer
				data.entityIdentifier = keyAssembler.assemble( rowProcessingState );
				if ( data.entityIdentifier == null ) {
					data.entityKey = null;
					data.setInstance( null );
					data.setState( State.MISSING );
					return;
				}
			}
			resolveInstanceFromIdentifier( data );
		}
	}

	protected void resolveInstanceFromIdentifier(Data data) {
		if ( data.batchDisabled ) {
			initialize( data );
		}
		else {
			data.entityKey = new EntityKey( data.entityIdentifier, concreteDescriptor );
			data.setInstance( getExistingInitializedInstance( data ) );
			if ( data.getInstance() == null ) {
				// need to add the key to the batch queue only when the entity has not been already loaded or
				// there isn't another initializer that is loading it
				registerToBatchFetchQueue( data );
			}
		}
	}

	@Override
	public void resolveInstance(Object instance, Data data) {
		final boolean identifierResolved = resolveIdentifier( instance, data );
		if ( data.entityIdentifier == null ) {
			data.setState( State.MISSING );
			data.entityKey = null;
			data.setInstance( null );
		}
		else {
			final var rowProcessingState = data.getRowProcessingState();
			final var session = rowProcessingState.getSession();
			final var persistenceContext = session.getPersistenceContextInternal();
			final var lazyInitializer = extractLazyInitializer( instance );
			if ( lazyInitializer == null ) {
				// Entity is most probably initialized
				if ( concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
					&& isPersistentAttributeInterceptable( instance )
					&& getAttributeInterceptor( instance )
							instanceof EnhancementAsProxyLazinessInterceptor enhancementInterceptor ) {
					if ( enhancementInterceptor.isInitialized() ) {
						data.setState( State.INITIALIZED );
					}
					else {
						data.setState( State.RESOLVED );
					}
				}
				else {
					// If the entity initializer is null, we know the entity is fully initialized,
					// otherwise it will be initialized by some other initializer
					data.setState( State.RESOLVED );
				}
			}
			else if ( lazyInitializer.isUninitialized() ) {
				data.setState( State.RESOLVED );
			}
			else {
				// Entity is initialized
				data.setState( State.INITIALIZED );
			}

			data.entityKey = new EntityKey( data.entityIdentifier, concreteDescriptor );
			final var entityHolder = persistenceContext.getEntityHolder( data.entityKey );

			if ( entityHolder == null || instance == null
					|| entityHolder.getEntity() != instance && entityHolder.getProxy() != instance ) {
				// the existing entity instance is detached or transient
				if ( entityHolder != null ) {
					final var managed = entityHolder.getManagedObject();
					data.setInstance( managed );
					data.entityKey = entityHolder.getEntityKey();
					data.entityIdentifier = data.entityKey.getIdentifier();
					data.setState( entityHolder.isInitialized() ? State.INITIALIZED : State.RESOLVED );
				}
				else {
					data.setState( State.RESOLVED );
				}
			}
			else {
				data.setInstance( instance );
			}

			if ( data.getState() == State.RESOLVED ) {
				// similar to resolveInstanceFromIdentifier, but we already have the holder here
				if ( data.batchDisabled ) {
					initialize( data, entityHolder, session, persistenceContext );
				}
				else if ( entityHolder == null || !entityHolder.isEventuallyInitialized() ) {
					// need to add the key to the batch queue only when the entity has not been already loaded or
					// there isn't another initializer that is loading it
					registerResolutionListener( data );
					registerToBatchFetchQueue( data );
				}
			}

			if ( keyIsEager && !identifierResolved ) {
				final var initializer = keyAssembler.getInitializer();
				assert initializer != null;
				initializer.resolveInstance( data.entityIdentifier, rowProcessingState );
			}
			else if ( rowProcessingState.needsResolveState() && !identifierResolved ) {
				// Resolve the state of the identifier if result caching is enabled and this is not a query cache hit
				keyAssembler.resolveState( rowProcessingState );
			}
		}
	}

	@Override
	public void initializeInstance(Data data) {
		if ( data.getState() == State.RESOLVED ) {
			data.setState( State.INITIALIZED );
			if ( data.batchDisabled ) {
				Hibernate.initialize( data.getInstance() );
			}
		}
	}

	protected Object getExistingInitializedInstance(Data data) {
		final var session = data.getRowProcessingState().getSession();
		final var persistenceContext = session.getPersistenceContextInternal();
		final var holder = persistenceContext.getEntityHolder( data.entityKey );
		if ( holder != null ) {
			final Object entity = holder.getEntity();
			if ( entity != null && holder.isEventuallyInitialized() ) {
				return entity;
			}
		}
		// we need to register a resolution listener only if there is not an already initialized instance
		// or an instance that another initializer is loading
		registerResolutionListener( data );
		return null;
	}

	protected void registerToBatchFetchQueue(Data data) {
		assert data.entityKey != null;
		data.getRowProcessingState().getSession().getPersistenceContextInternal()
				.getBatchFetchQueue().addBatchLoadableEntityKey( data.entityKey );
	}

	@Override
	public void initializeInstanceFromParent(Object parentInstance, Data data) {
		final var attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance =
				attributeMapping != null
						? attributeMapping.getValue( parentInstance )
						: parentInstance;
		// No need to initialize these fields
		data.entityKey = null;
		data.entityIdentifier = null;
		data.setInstance( null );
		if ( instance == null ) {
			data.setState( State.MISSING );
		}
		else {
			final var lazyInitializer = extractLazyInitializer( instance );
			if ( lazyInitializer != null && lazyInitializer.isUninitialized() ) {
				data.entityKey = new EntityKey( lazyInitializer.getInternalIdentifier(), concreteDescriptor );
				registerToBatchFetchQueue( data );
			}
			data.setState( State.INITIALIZED );
		}
	}

	protected static Object loadInstance(
			EntityKey entityKey,
			ToOneAttributeMapping toOneMapping,
			boolean affectedByFilter,
			SharedSessionContractImplementor session) {
		final String entityName = entityKey.getEntityName();
		final Object identifier = entityKey.getIdentifier();
		final Object instance =
				session.internalLoad( entityName, identifier, true,
						toOneMapping.isInternalLoadNullable() );
		if ( instance == null ) {
			checkNotFound( toOneMapping, affectedByFilter, entityName, identifier );
		}
		return instance;
	}

	protected AttributeMapping[] getParentEntityAttributes(String attributeName) {
		final var entityDescriptor = owningEntityInitializer.getEntityDescriptor();
		final int size =
				entityDescriptor.getRootEntityDescriptor()
						.getSubclassEntityNames().size();
		final var parentEntityAttributes = new AttributeMapping[size];
		parentEntityAttributes[ entityDescriptor.getSubclassId() ] =
				getParentEntityAttribute( entityDescriptor, toOneMapping, attributeName );
		for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
			parentEntityAttributes[ subMappingType.getSubclassId() ] =
					getParentEntityAttribute( subMappingType, toOneMapping, attributeName );
		}
		return parentEntityAttributes;
	}

	protected static AttributeMapping getParentEntityAttribute(
			EntityMappingType subMappingType,
			ToOneAttributeMapping referencedModelPart,
			String attributeName) {
		final var parentAttribute = subMappingType.findAttributeMapping( attributeName );
		// These checks are needed to avoid setting the instance using the wrong (child's) model part or
		// setting it multiple times in case parent and child share the same attribute name for the association.
		return parentAttribute != null
			&& parentAttribute.getDeclaringType()
					== referencedModelPart.getDeclaringType().findContainingEntityMapping()
				? parentAttribute
				: null;
	}

}
