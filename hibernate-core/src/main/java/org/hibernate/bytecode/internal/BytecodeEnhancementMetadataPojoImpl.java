/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal;

import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.boot.Metadata;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.CompositeType;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptableType;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.processIfSelfDirtinessTracker;

/**
 * BytecodeEnhancementMetadata implementation for {@link org.hibernate.metamodel.RepresentationMode#POJO POJO} models
 *
 * @author Steve Ebersole
 */
public class BytecodeEnhancementMetadataPojoImpl implements BytecodeEnhancementMetadata {
	/**
	 * Static constructor
	 */
	public static BytecodeEnhancementMetadataPojoImpl from(
			PersistentClass persistentClass,
			Set<String> identifierAttributeNames,
			CompositeType nonAggregatedCidMapper,
			boolean collectionsInDefaultFetchGroupEnabled,
			Metadata metadata) {
		final var mappedClass = persistentClass.getMappedClass();
		final boolean enhancedForLazyLoading = isPersistentAttributeInterceptableType( mappedClass );
		final var lazyAttributesMetadata = enhancedForLazyLoading
				? LazyAttributesMetadata.from( persistentClass, true, collectionsInDefaultFetchGroupEnabled, metadata )
				: LazyAttributesMetadata.nonEnhanced( persistentClass.getEntityName() );

		return new BytecodeEnhancementMetadataPojoImpl(
				persistentClass.getEntityName(),
				mappedClass,
				identifierAttributeNames,
				nonAggregatedCidMapper,
				enhancedForLazyLoading,
				lazyAttributesMetadata
		);
	}

	private final String entityName;
	private final Class<?> entityClass;
	private final Set<String> identifierAttributeNames;
	private final CompositeType nonAggregatedCidMapper;
	private final boolean enhancedForLazyLoading;
	private final LazyAttributesMetadata lazyAttributesMetadata;
	private final LazyAttributeLoadingInterceptor.EntityRelatedState lazyAttributeLoadingInterceptorState;
	private volatile transient EnhancementAsProxyLazinessInterceptor.EntityRelatedState enhancementAsProxyInterceptorState;

	/*
	 * Used by Hibernate Reactive
	 */
	protected BytecodeEnhancementMetadataPojoImpl(
			String entityName,
			Class<?> entityClass,
			Set<String> identifierAttributeNames,
			CompositeType nonAggregatedCidMapper,
			boolean enhancedForLazyLoading,
			LazyAttributesMetadata lazyAttributesMetadata) {
		this.nonAggregatedCidMapper = nonAggregatedCidMapper;
		assert identifierAttributeNames != null;
		assert !identifierAttributeNames.isEmpty();

		this.entityName = entityName;
		this.entityClass = entityClass;
		this.identifierAttributeNames = identifierAttributeNames;
		this.enhancedForLazyLoading = enhancedForLazyLoading;
		this.lazyAttributesMetadata = lazyAttributesMetadata;
		this.lazyAttributeLoadingInterceptorState =
				new LazyAttributeLoadingInterceptor.EntityRelatedState( getEntityName(),
						lazyAttributesMetadata.getLazyAttributeNames() );
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public boolean isEnhancedForLazyLoading() {
		return enhancedForLazyLoading;
	}

	@Override
	public LazyAttributesMetadata getLazyAttributesMetadata() {
		return lazyAttributesMetadata;
	}

	@Override
	public boolean hasUnFetchedAttributes(Object entity) {
		if ( ! enhancedForLazyLoading ) {
			return false;
		}

		final var interceptor = extractLazyInterceptor( entity );
		if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
			return interceptor.hasAnyUninitializedAttributes();
		}

		//noinspection RedundantIfStatement
		if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isAttributeLoaded(Object entity, String attributeName) {
		if ( ! enhancedForLazyLoading ) {
			return true;
		}

		final var interceptor = extractLazyInterceptor( entity );
		if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
			return interceptor.isAttributeLoaded( attributeName );
		}

		return true;
	}

	@Override
	public @Nullable LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		return (LazyAttributeLoadingInterceptor) extractLazyInterceptor( entity );
	}

	@Override
	public PersistentAttributeInterceptable createEnhancedProxy(EntityKey entityKey, boolean addEmptyEntry, SharedSessionContractImplementor session) {
		final var persister = entityKey.getPersister();
		final Object identifier = entityKey.getIdentifier();
		final var persistenceContext = session.getPersistenceContext();

		// first, instantiate the entity instance to use as the proxy
		final var entity = asPersistentAttributeInterceptable( persister.instantiate( identifier, session ) );

		// clear the fields that are marked as dirty in the dirtiness tracker
		processIfSelfDirtinessTracker( entity, BytecodeEnhancementMetadataPojoImpl::clearDirtyAttributes );
		processIfManagedEntity( entity, BytecodeEnhancementMetadataPojoImpl::useTracker );

		// add the entity (proxy) instance to the PC
		persistenceContext.addEnhancedProxy( entityKey, entity );

		// if requested, add the "holder entry" to the PC
		if ( addEmptyEntry ) {
			final var entityHolder = persistenceContext.getEntityHolder( entityKey );
			final var entityEntry = persistenceContext.addEntry(
					entity,
					Status.MANAGED,
					// loaded state
					null,
					// row-id
					null,
					identifier,
					// version
					null,
					LockMode.NONE,
					// we assume it exists in db
					true,
					persister,
					true
			);
			entityHolder.setEntityEntry( entityEntry );
		}

		// inject the interceptor
		persister.getBytecodeEnhancementMetadata()
				.injectEnhancedEntityAsProxyInterceptor( entity, entityKey, session );

		return entity;
	}

	private static void clearDirtyAttributes(final SelfDirtinessTracker entity) {
		entity.$$_hibernate_clearDirtyAttributes();
	}

	private static void useTracker(final ManagedEntity entity) {
		entity.$$_hibernate_setUseTracker( true );
	}

	@Override
	public LazyAttributeLoadingInterceptor injectInterceptor(
			Object entity,
			Object identifier,
			SharedSessionContractImplementor session) {
		if ( !enhancedForLazyLoading ) {
			throw new NotInstrumentedException( "Entity class [" + entityClass.getName() + "] is not enhanced for lazy loading" );
		}

		if ( !entityClass.isInstance( entity ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Passed entity instance [%s] is not of expected type [%s]",
							entity,
							getEntityName()
					)
			);
		}
		final var interceptor = new LazyAttributeLoadingInterceptor(
				lazyAttributeLoadingInterceptorState,
				identifier,
				session
		);
		injectInterceptor( entity, interceptor, session );
		return interceptor;
	}

	@Override
	public void injectEnhancedEntityAsProxyInterceptor(
			Object entity,
			EntityKey entityKey,
			SharedSessionContractImplementor session) {
		EnhancementAsProxyLazinessInterceptor.EntityRelatedState meta = getEnhancementAsProxyLazinessInterceptorMetastate( session );
		injectInterceptor(
				entity,
				new EnhancementAsProxyLazinessInterceptor(
						meta,
						entityKey,
						session
				),
				session
		);
	}

	/*
	 * Used by Hibernate Reactive
	 */
	//This state object needs to be lazily initialized as it needs access to the Persister, but once
	//initialized it can be reused across multiple sessions.
	public EnhancementAsProxyLazinessInterceptor.EntityRelatedState getEnhancementAsProxyLazinessInterceptorMetastate(SharedSessionContractImplementor session) {
		var state = this.enhancementAsProxyInterceptorState;
		if ( state == null ) {
			final var entityPersister =
					session.getFactory().getMappingMetamodel()
							.getEntityDescriptor( entityName );
			state = new EnhancementAsProxyLazinessInterceptor.EntityRelatedState(
					entityPersister,
					nonAggregatedCidMapper,
					identifierAttributeNames
			);
			this.enhancementAsProxyInterceptorState = state;
		}
		return state;
	}

	@Override
	public void injectInterceptor(
			Object entity,
			PersistentAttributeInterceptor interceptor,
			SharedSessionContractImplementor session) {
		if ( !enhancedForLazyLoading ) {
			throw new NotInstrumentedException( "Entity class [" + entityClass.getName() + "] is not enhanced for lazy loading" );
		}

		if ( !entityClass.isInstance( entity ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Passed entity instance [%s] is not of expected type [%s]",
							entity,
							getEntityName()
					)
			);
		}

		asPersistentAttributeInterceptable( entity ).$$_hibernate_setInterceptor( interceptor );
	}

	@Override
	public @Nullable BytecodeLazyAttributeInterceptor extractLazyInterceptor(Object entity) throws NotInstrumentedException {
		if ( !enhancedForLazyLoading ) {
			throw new NotInstrumentedException( "Entity class [" + entityClass.getName() + "] is not enhanced for lazy loading" );
		}

		if ( !entityClass.isInstance( entity ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Passed entity instance [%s] is not of expected type [%s]",
							entity,
							getEntityName()
					)
			);
		}

		final PersistentAttributeInterceptor interceptor = asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
		if ( interceptor == null ) {
			return null;
		}

		return (BytecodeLazyAttributeInterceptor) interceptor;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public Class<?> getEntityClass() {
		return entityClass;
	}

	/*
	 * Used by Hibernate Reactive
	 */
	public LazyAttributeLoadingInterceptor.EntityRelatedState getLazyAttributeLoadingInterceptorState() {
		return lazyAttributeLoadingInterceptorState;
	}
}
