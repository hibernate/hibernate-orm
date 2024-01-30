/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
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
		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent cacheGetEvent = eventManager.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventManager.completeCacheGetEvent(
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
		final EventManager eventManager = session.getEventManager();
		final HibernateMonitoringEvent cacheGetEvent = eventManager.beginCacheGetEvent();
		try {
			cachedValue = cacheAccess.get( session, cacheKey );
		}
		finally {
			eventManager.completeCacheGetEvent(
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
