/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.util.Set;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.bytecode.spi.NotInstrumentedException;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Steve Ebersole
 */
public class BytecodeEnhancementMetadataPojoImpl implements BytecodeEnhancementMetadata {
	private final Class entityClass;
	private final boolean enhancedForLazyLoading;

	public BytecodeEnhancementMetadataPojoImpl(Class entityClass) {
		this.entityClass = entityClass;
		this.enhancedForLazyLoading = PersistentAttributeInterceptable.class.isAssignableFrom( entityClass );
	}

	@Override
	public String getEntityName() {
		return entityClass.getName();
	}

	@Override
	public boolean isEnhancedForLazyLoading() {
		return enhancedForLazyLoading;
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
	public LazyAttributeLoadingInterceptor injectInterceptor(
			Object entity,
			Set<String> uninitializedFieldNames,
			SessionImplementor session) throws NotInstrumentedException {
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

		final LazyAttributeLoadingInterceptor interceptor = new LazyAttributeLoadingInterceptor( session, uninitializedFieldNames, getEntityName() );
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( interceptor );
		return interceptor;
	}
}
