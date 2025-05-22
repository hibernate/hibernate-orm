/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.*;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
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

			batchDisabled = rowProcessingState.isScrollResult()
					|| !rowProcessingState.getLoadQueryInfluencers().effectivelyBatchLoadable( initializer.toOneMapping.getEntityMappingType().getEntityPersister() );
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
		this.owningEntityInitializer = (EntityInitializer<InitializerData>) Initializer.findOwningEntityInitializer( parent );
		assert owningEntityInitializer != null : "This initializer requires an owning parent entity initializer";
	}

	protected abstract void registerResolutionListener(Data data);

	@Override
	public void resolveKey(Data data) {
		if ( data.getState() != State.UNINITIALIZED ) {
			return;
		}

		data.entityKey = null;
		data.setInstance( null );
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		//noinspection unchecked
		final Initializer<InitializerData> initializer = (Initializer<InitializerData>) keyAssembler.getInitializer();
		if ( initializer != null ) {
			final InitializerData subData = initializer.getData( rowProcessingState );
			initializer.resolveKey( subData );
			data.entityIdentifier = null;
			data.setState( subData.getState() == State.MISSING ? State.MISSING : State.KEY_RESOLVED );
		}
		else {
			data.entityIdentifier = keyAssembler.assemble( rowProcessingState );
			data.setState( data.entityIdentifier == null ? State.MISSING : State.KEY_RESOLVED );
		}
	}

	@Override
	public void resolveInstance(Data data) {
		if ( data.getState() != State.KEY_RESOLVED ) {
			return;
		}

		data.setState( State.RESOLVED );
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
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
		if ( instance == null ) {
			data.setState( State.MISSING );
			data.entityKey = null;
			data.setInstance( null );
			return;
		}
		final RowProcessingState rowProcessingState = data.getRowProcessingState();
		// Only need to extract the identifier if the identifier has a many to one
		final LazyInitializer lazyInitializer = extractLazyInitializer( instance );
		data.entityKey = null;
		data.entityIdentifier = null;
		if ( lazyInitializer == null ) {
			// Entity is most probably initialized
			data.setInstance( instance );
			final PersistentAttributeInterceptor interceptor;
			if ( concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()
					&& isPersistentAttributeInterceptable( instance )
					&& ( interceptor = getAttributeInterceptor( instance ) ) instanceof EnhancementAsProxyLazinessInterceptor ) {
				final EnhancementAsProxyLazinessInterceptor enhancementInterceptor = (EnhancementAsProxyLazinessInterceptor) interceptor;
				if ( enhancementInterceptor.isInitialized() ) {
					data.setState( State.INITIALIZED );
				}
				else {
					data.setState( State.RESOLVED );
					data.entityIdentifier = enhancementInterceptor.getIdentifier();
				}
			}
			else {
				// If the entity initializer is null, we know the entity is fully initialized,
				// otherwise it will be initialized by some other initializer
				data.setState( State.RESOLVED );
				data.entityIdentifier = concreteDescriptor.getIdentifier( instance, rowProcessingState.getSession() );
			}
			if ( keyIsEager && data.entityIdentifier == null ) {
				data.entityIdentifier = concreteDescriptor.getIdentifier( instance, rowProcessingState.getSession() );
			}
		}
		else if ( lazyInitializer.isUninitialized() ) {
			data.setState( State.RESOLVED );
			data.entityIdentifier = lazyInitializer.getInternalIdentifier();
		}
		else {
			// Entity is initialized
			data.setState( State.INITIALIZED );
			if ( keyIsEager ) {
				data.entityIdentifier = lazyInitializer.getInternalIdentifier();
			}
			data.setInstance( lazyInitializer.getImplementation() );
		}

		if ( data.getState() == State.RESOLVED ) {
			resolveInstanceFromIdentifier( data );
		}
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

	@Override
	public void initializeInstance(Data data) {
		if ( data.getState() != State.RESOLVED ) {
			return;
		}
		data.setState( State.INITIALIZED );
		if ( data.batchDisabled ) {
			Hibernate.initialize( data.getInstance() );
		}
	}

	protected Object getExistingInitializedInstance(Data data) {
		final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final EntityHolder holder = persistenceContext.getEntityHolder( data.entityKey );
		if ( holder != null && holder.getEntity() != null && holder.isEventuallyInitialized() ) {
			return holder.getEntity();
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
		final AttributeMapping attributeMapping = getInitializedPart().asAttributeMapping();
		final Object instance = attributeMapping != null
				? attributeMapping.getValue( parentInstance )
				: parentInstance;
		// No need to initialize these fields
		data.entityKey = null;
		data.setInstance( null );
		if ( instance == null ) {
			data.setState( State.MISSING );
		}
		else {
			final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( instance );
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
		final Object instance = session.internalLoad(
				entityKey.getEntityName(),
				entityKey.getIdentifier(),
				true,
				toOneMapping.isInternalLoadNullable()
		);
		if ( instance == null ) {
			if ( toOneMapping.getNotFoundAction() != NotFoundAction.IGNORE ) {
				if ( affectedByFilter ) {
					throw new EntityFilterException(
							entityKey.getEntityName(),
							entityKey.getIdentifier(),
							toOneMapping.getNavigableRole().getFullPath()
					);
				}
				if ( toOneMapping.getNotFoundAction() == NotFoundAction.EXCEPTION ) {
					throw new FetchNotFoundException( entityKey.getEntityName(), entityKey.getIdentifier() );
				}
			}
		}
		return instance;
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
				toOneMapping,
				attributeName
		);
		for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
			parentEntityAttributes[subMappingType.getSubclassId()] = getParentEntityAttribute(
					subMappingType,
					toOneMapping,
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

}
