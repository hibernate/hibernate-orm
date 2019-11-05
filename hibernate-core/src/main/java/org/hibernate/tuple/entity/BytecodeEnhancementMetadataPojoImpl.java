/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public final class BytecodeEnhancementMetadataPojoImpl implements BytecodeEnhancementMetadata {
	/**
	 * Static constructor
	 */
	public static BytecodeEnhancementMetadata from(
			PersistentClass persistentClass,
			Set<String> identifierAttributeNames,
			CompositeType nonAggregatedCidMapper,
			boolean allowEnhancementAsProxy) {
		final Class mappedClass = persistentClass.getMappedClass();
		final boolean enhancedForLazyLoading = PersistentAttributeInterceptable.class.isAssignableFrom( mappedClass );
		final LazyAttributesMetadata lazyAttributesMetadata = enhancedForLazyLoading
				? LazyAttributesMetadata.from( persistentClass, true, allowEnhancementAsProxy )
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
	private final Class entityClass;
	private final Set<String> identifierAttributeNames;
	private final CompositeType nonAggregatedCidMapper;
	private final boolean enhancedForLazyLoading;
	private final LazyAttributesMetadata lazyAttributesMetadata;

	@SuppressWarnings("WeakerAccess")
	protected BytecodeEnhancementMetadataPojoImpl(
			String entityName,
			Class entityClass,
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

		final BytecodeLazyAttributeInterceptor interceptor = extractLazyInterceptor( entity );
		if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
			return ( (LazyAttributeLoadingInterceptor) interceptor ).hasAnyUninitializedAttributes();
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

		final BytecodeLazyAttributeInterceptor interceptor = extractLazyInterceptor( entity );
		if ( interceptor instanceof LazyAttributeLoadingInterceptor ) {
			return ( (LazyAttributeLoadingInterceptor) interceptor ).isAttributeLoaded( attributeName );
		}

		return true;
	}

	@Override
	public LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
		return (LazyAttributeLoadingInterceptor) extractLazyInterceptor( entity );
	}

	@Override
	public PersistentAttributeInterceptable createEnhancedProxy(EntityKey entityKey, boolean addEmptyEntry, SharedSessionContractImplementor session) {
		final EntityPersister persister = entityKey.getPersister();
		final Serializable identifier = entityKey.getIdentifier();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		// first, instantiate the entity instance to use as the proxy
		final EntityTuplizer entityTuplizer = persister.getEntityTuplizer();
		final PersistentAttributeInterceptable entity = (PersistentAttributeInterceptable) entityTuplizer
				.instantiate( identifier, session );

		// clear the fields that are marked as dirty in the dirtyness tracker
		if ( entity instanceof SelfDirtinessTracker ) {
			( (SelfDirtinessTracker) entity ).$$_hibernate_clearDirtyAttributes();
		}
		// add the entity (proxy) instance to the PC
		persistenceContext.addEnhancedProxy( entityKey, entity );

		// if requested, add the "holder entry" to the PC
		if ( addEmptyEntry ) {
			persistenceContext.addEntry(
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
		}

		// inject the interceptor
		persister.getEntityMetamodel()
				.getBytecodeEnhancementMetadata()
				.injectEnhancedEntityAsProxyInterceptor( entity, entityKey, session );

		return entity;
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
		final LazyAttributeLoadingInterceptor interceptor = new LazyAttributeLoadingInterceptor(
				getEntityName(),
				identifier,
				lazyAttributesMetadata.getLazyAttributeNames(),
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
		injectInterceptor(
				entity,
				new EnhancementAsProxyLazinessInterceptor(
						entityName,
						identifierAttributeNames,
						nonAggregatedCidMapper,
						entityKey,
						session
				),
				session
		);
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

		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( interceptor );
	}

	@Override
	public BytecodeLazyAttributeInterceptor extractLazyInterceptor(Object entity) throws NotInstrumentedException {
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

		final PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) entity ).$$_hibernate_getInterceptor();
		if ( interceptor == null ) {
			return null;
		}

		return (BytecodeLazyAttributeInterceptor) interceptor;
	}

}
