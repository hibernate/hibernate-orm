/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Steve Ebersole
 */
public class BytecodeEnhancementMetadataPojoImpl implements BytecodeEnhancementMetadata {
	public static BytecodeEnhancementMetadata from(PersistentClass persistentClass) {
		final Class mappedClass = persistentClass.getMappedClass();
		final boolean enhancedForLazyLoading = PersistentAttributeInterceptable.class.isAssignableFrom( mappedClass );
		final LazyAttributesMetadata lazyAttributesMetadata = enhancedForLazyLoading
				? LazyAttributesMetadata.from( persistentClass )
				: LazyAttributesMetadata.nonEnhanced( persistentClass.getEntityName() );

		return new BytecodeEnhancementMetadataPojoImpl(
				persistentClass.getEntityName(),
				mappedClass,
				enhancedForLazyLoading,
				lazyAttributesMetadata
		);
	}

	private final String entityName;
	private final Class entityClass;
	private final boolean enhancedForLazyLoading;
	private final LazyAttributesMetadata lazyAttributesMetadata;

	public BytecodeEnhancementMetadataPojoImpl(
			String entityName,
			Class entityClass,
			boolean enhancedForLazyLoading,
			LazyAttributesMetadata lazyAttributesMetadata) {
		this.entityName = entityName;
		this.entityClass = entityClass;
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
		LazyAttributeLoadingInterceptor interceptor = enhancedForLazyLoading ? extractInterceptor( entity ) : null;
		return interceptor != null && interceptor.hasAnyUninitializedAttributes();
	}

	@Override
	public boolean isAttributeLoaded(Object entity, String attributeName) {
		LazyAttributeLoadingInterceptor interceptor = enhancedForLazyLoading ? extractInterceptor( entity ) : null;
		return interceptor == null || interceptor.isAttributeLoaded( attributeName );
	}

	@Override
	public LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException {
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

		return (LazyAttributeLoadingInterceptor) interceptor;
	}

	@Override
	public LazyAttributeLoadingInterceptor injectInterceptor(Object entity, SharedSessionContractImplementor session) {
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
				lazyAttributesMetadata.getLazyAttributeNames(),
				session
		);
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( interceptor );
		return interceptor;
	}
}
