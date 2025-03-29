/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class CacheHelper {

	private CacheHelper() {
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			EntityPersister persister,
			CachedDomainDataAccess cacheAccess) {
		return fromSharedCache( session, cacheKey, persister, false, cacheAccess );
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			EntityPersister persister,
			boolean isNaturalKey,
			CachedDomainDataAccess cacheAccess) {
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		Object cachedValue = null;
		eventListenerManager.cacheGetStart();
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheAccess.getRegion(),
					persister,
					isNaturalKey,
					cachedValue != null
			);
			eventListenerManager.cacheGetEnd( cachedValue != null );
		}
		return cachedValue;
	}

	public static Object fromSharedCache(
			SharedSessionContractImplementor session,
			Object cacheKey,
			CollectionPersister persister,
			CachedDomainDataAccess cacheAccess) {
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		Object cachedValue = null;
		eventListenerManager.cacheGetStart();
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheAccess.getRegion(),
					persister,
					cachedValue != null
			);
			eventListenerManager.cacheGetEnd( cachedValue != null );
		}
		return cachedValue;
	}
	public static void addBasicValueToCacheKey(
			MutableCacheKeyBuilder cacheKey,
			Object value,
			JdbcMapping jdbcMapping,
			SharedSessionContractImplementor session) {
		final BasicValueConverter converter = jdbcMapping.getValueConverter();
		final Object convertedValue;
		final JavaType javaType;
		if ( converter == null ) {
			javaType = jdbcMapping.getJavaTypeDescriptor();
			convertedValue = value;
		}
		else {
			javaType = converter.getRelationalJavaType();
			convertedValue = converter.toRelationalValue( value );
		}
		if ( convertedValue == null ) {
			cacheKey.addValue( null );
			cacheKey.addHashCode( 0 );
		}
		else {
			cacheKey.addValue( javaType.getMutabilityPlan().disassemble( convertedValue, session ) );
			cacheKey.addHashCode( javaType.extractHashCode( convertedValue ) );
		}
	}
}
