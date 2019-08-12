/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.PessimisticLockScope;

import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;

/**
 * Internal component.
 *
 * Collects any components that any Session implementation will likely need
 * for faster access and reduced allocations.
 * Conceptually this acts as an immutable caching intermediary between Session
 * and SessionFactory.
 * Designed to be immutable, and shared across Session instances.
 *
 * Assumes to be created infrequently, possibly only once per SessionFactory.
 *
 * If the Session is requiring to retrieve (or compute) anything from the SessionFactory,
 * and this computation would result in the same outcome for any Session created on
 * this same SessionFactory, then it belongs in a final field of this class.
 *
 * Finally, consider also limiting the size of each Session: some fields could be good
 * candidates to be replaced with access via this object.
 *
 * @author Sanne Grinovero
 */
final class FastSessionServices {

	/**
	 * Default session properties
	 */
	final Map<String, Object> defaultSessionProperties;

	// All session events need to be iterated frequently:

	private final Iterable<ClearEventListener> clearEventListeners;
	private final Iterable<SaveOrUpdateEventListener> saveUpdateEventListeners;
	private final Iterable<EvictEventListener> evictEventListeners;
	private final Iterable<SaveOrUpdateEventListener> saveEventListeners;
	private final Iterable<SaveOrUpdateEventListener> updateEventListeners;
	private final Iterable<LockEventListener> lockEventListeners;
	private final Iterable<PersistEventListener> persistEventListeners;
	private final Iterable<PersistEventListener> persistOnFlushEventListeners;
	private final Iterable<MergeEventListener> mergeEventListeners;
	private final Iterable<DeleteEventListener> deleteEventListeners;
	private final Iterable<LoadEventListener> loadEventListeners;
	private final Iterable<RefreshEventListener> refreshEventListeners;
	private final Iterable<ReplicateEventListener> replicateEventListeners;
	private final Iterable<AutoFlushEventListener> autoFlushEventListeners;
	private final Iterable<DirtyCheckEventListener> dirtyCheckEventListeners;
	private final Iterable<FlushEventListener> flushEventListeners;
	private final Iterable<InitializeCollectionEventListener> initCollectionEventListeners;
	private final Iterable<ResolveNaturalIdEventListener> resolveNaturalIdEventListeners;

	//Intentionally Package private:
	final boolean disallowOutOfTransactionUpdateOperations;
	final boolean useStreamForLobBinding;
	final boolean requiresMultiTenantConnectionProvider;
	final ConnectionProvider connectionProvider;
	final MultiTenantConnectionProvider multiTenantConnectionProvider;
	final ClassLoaderService classLoaderService;
	final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	final JdbcServices jdbcServices;
	final boolean isJtaTransactionAccessible;
	final CacheMode initialSessionCacheMode;

	//Private fields:
	private final Dialect dialect;
	private final CacheStoreMode defaultCacheStoreMode;
	private final CacheRetrieveMode defaultCacheRetrieveMode;

	FastSessionServices(SessionFactoryImpl sf) {
		Objects.requireNonNull( sf );
		final ServiceRegistryImplementor sr = sf.getServiceRegistry();
		final JdbcServices jdbcServices = sf.getJdbcServices();

		// Pre-compute all iterators on Event listeners:
		final EventListenerRegistry eventListenerRegistry = sr.getService( EventListenerRegistry.class );
		this.clearEventListeners = listeners( eventListenerRegistry, EventType.CLEAR );
		this.saveUpdateEventListeners = listeners( eventListenerRegistry, EventType.SAVE_UPDATE );
		this.evictEventListeners = listeners( eventListenerRegistry, EventType.EVICT );
		this.saveEventListeners = listeners( eventListenerRegistry, EventType.SAVE );
		this.updateEventListeners = listeners( eventListenerRegistry, EventType.UPDATE );
		this.lockEventListeners = listeners( eventListenerRegistry, EventType.LOCK );
		this.persistEventListeners = listeners( eventListenerRegistry, EventType.PERSIST );
		this.persistOnFlushEventListeners = listeners( eventListenerRegistry, EventType.PERSIST_ONFLUSH );
		this.mergeEventListeners = listeners( eventListenerRegistry, EventType.MERGE );
		this.deleteEventListeners = listeners( eventListenerRegistry, EventType.DELETE );
		this.loadEventListeners = listeners( eventListenerRegistry, EventType.LOAD );
		this.refreshEventListeners = listeners( eventListenerRegistry, EventType.REFRESH );
		this.replicateEventListeners = listeners( eventListenerRegistry, EventType.REPLICATE );
		this.autoFlushEventListeners = listeners( eventListenerRegistry, EventType.AUTO_FLUSH );
		this.dirtyCheckEventListeners = listeners( eventListenerRegistry, EventType.DIRTY_CHECK );
		this.flushEventListeners = listeners( eventListenerRegistry, EventType.FLUSH );
		this.initCollectionEventListeners = listeners( eventListenerRegistry, EventType.INIT_COLLECTION );
		this.resolveNaturalIdEventListeners = listeners( eventListenerRegistry, EventType.RESOLVE_NATURAL_ID );

		//Other highly useful constants:
		this.dialect = jdbcServices.getJdbcEnvironment().getDialect();
		this.disallowOutOfTransactionUpdateOperations = !sf.getSessionFactoryOptions().isAllowOutOfTransactionUpdateOperations();
		this.useStreamForLobBinding = Environment.useStreamsForBinary() || dialect.useInputStreamToInsertBlob();
		this.requiresMultiTenantConnectionProvider = sf.getSettings().getMultiTenancyStrategy().requiresMultiTenantConnectionProvider();

		//Some "hot" services:
		this.connectionProvider = requiresMultiTenantConnectionProvider ? null : sr.getService( ConnectionProvider.class );
		this.multiTenantConnectionProvider = requiresMultiTenantConnectionProvider ? sr.getService( MultiTenantConnectionProvider.class ) : null;
		this.classLoaderService = sr.getService( ClassLoaderService.class );
		this.transactionCoordinatorBuilder = sr.getService( TransactionCoordinatorBuilder.class );
		this.jdbcServices = sr.getService( JdbcServices.class );

		this.isJtaTransactionAccessible = isTransactionAccessible( sf, transactionCoordinatorBuilder );

		this.defaultSessionProperties = initializeDefaultSessionProperties( sf );
		this.defaultCacheStoreMode = determineCacheStoreMode( defaultSessionProperties );
		this.defaultCacheRetrieveMode = determineCacheRetrieveMode( defaultSessionProperties );
		this.initialSessionCacheMode = CacheModeHelper.interpretCacheMode( defaultCacheStoreMode, defaultCacheRetrieveMode );
	}

	Iterable<ClearEventListener> getClearEventListeners() {
		return clearEventListeners;
	}

	Iterable<EvictEventListener> getEvictEventListeners() {
		return this.evictEventListeners;
	}

	Iterable<DirtyCheckEventListener> getDirtyCheckEventListeners() {
		return this.dirtyCheckEventListeners;
	}

	Iterable<SaveOrUpdateEventListener> getSaveUpdateEventListeners() {
		return this.saveUpdateEventListeners;
	}

	Iterable<SaveOrUpdateEventListener> getSaveEventListeners() {
		return this.saveEventListeners;
	}

	Iterable<SaveOrUpdateEventListener> getUpdateEventListeners() {
		return this.updateEventListeners;
	}

	Iterable<LockEventListener> getLockEventListeners() {
		return this.lockEventListeners;
	}

	Iterable<PersistEventListener> getPersistEventListeners() {
		return persistEventListeners;
	}

	Iterable<FlushEventListener> getFlushEventListeners() {
		return flushEventListeners;
	}

	Iterable<PersistEventListener> getPersistOnFlushEventListeners() {
		return persistOnFlushEventListeners;
	}

	Iterable<InitializeCollectionEventListener> getInitCollectionEventListeners() {
		return initCollectionEventListeners;
	}

	Iterable<LoadEventListener> getLoadEventListeners() {
		return loadEventListeners;
	}

	Iterable<MergeEventListener> getMergeEventListeners() {
		return mergeEventListeners;
	}

	Iterable<RefreshEventListener> getRefreshEventListeners() {
		return refreshEventListeners;
	}

	Iterable<DeleteEventListener> getDeleteEventListeners() {
		return deleteEventListeners;
	}

	Iterable<AutoFlushEventListener> getAutoFlushEventListeners() {
		return autoFlushEventListeners;
	}

	Iterable<ReplicateEventListener> getReplicateEventListeners() {
		return replicateEventListeners;
	}

	Iterable<ResolveNaturalIdEventListener> getResolveNaturalIdEventListeners() {
		return resolveNaturalIdEventListeners;
	}

	private static <T> Iterable<T> listeners(EventListenerRegistry elr, EventType<T> type) {
		return elr.getEventListenerGroup( type ).listeners();
	}

	SqlTypeDescriptor remapSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		if ( !sqlTypeDescriptor.canBeRemapped() ) {
			return sqlTypeDescriptor;
		}

		final SqlTypeDescriptor remapped = dialect.remapSqlTypeDescriptor( sqlTypeDescriptor );
		return remapped == null ? sqlTypeDescriptor : remapped;
	}

	private static boolean isTransactionAccessible(SessionFactoryImpl sf, TransactionCoordinatorBuilder transactionCoordinatorBuilder) {
		// JPA requires that access not be provided to the transaction when using JTA.
		// This is overridden when SessionFactoryOptions isJtaTransactionAccessEnabled() is true.
		if ( sf.getSessionFactoryOptions().getJpaCompliance().isJpaTransactionComplianceEnabled() &&
				transactionCoordinatorBuilder.isJta() &&
				!sf.getSessionFactoryOptions().isJtaTransactionAccessEnabled() ) {
			return false;
		}
		return true;
	}

	private static Map<String, Object> initializeDefaultSessionProperties(SessionFactoryImpl sf) {
		HashMap<String,Object> p = new HashMap<>();

		//Static defaults:
		p.putIfAbsent( AvailableSettings.FLUSH_MODE, FlushMode.AUTO.name() );
		p.putIfAbsent( JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		p.putIfAbsent( JPA_LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		p.putIfAbsent( JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		p.putIfAbsent( JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );

		//Defaults defined by SessionFactory configuration:
		final String[] ENTITY_MANAGER_SPECIFIC_PROPERTIES = {
				JPA_LOCK_SCOPE,
				JPA_LOCK_TIMEOUT,
				AvailableSettings.FLUSH_MODE,
				JPA_SHARED_CACHE_RETRIEVE_MODE,
				JPA_SHARED_CACHE_STORE_MODE,
				QueryHints.SPEC_HINT_TIMEOUT
		};
		final Map<String, Object> properties = sf.getProperties();
		for ( String key : ENTITY_MANAGER_SPECIFIC_PROPERTIES ) {
			if ( properties.containsKey( key ) ) {
				p.put( key, properties.get( key ) );
			}
		}
		return Collections.unmodifiableMap( p );
	}

	/**
	 * @param properties the Session properties
	 * @return either the CacheStoreMode as defined in the Session specific properties, or as defined in the
	 *  properties shared across all sessions (the defaults).
	 */
	CacheStoreMode getCacheStoreMode(final Map<String, Object> properties) {
		if ( properties == null ) {
			return this.defaultCacheStoreMode;
		}
		else {
			return determineCacheStoreMode( properties );
		}
	}

	/**
	 * @param properties the Session properties
	 * @return either the CacheRetrieveMode as defined in the Session specific properties, or as defined in the
	 *  properties shared across all sessions (the defaults).
	 */
	CacheRetrieveMode getCacheRetrieveMode(Map<String, Object> properties) {
		if ( properties == null ) {
			return this.defaultCacheRetrieveMode;
		}
		else {
			return determineCacheRetrieveMode( properties );
		}
	}

	private static CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		return ( CacheRetrieveMode ) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
	}

	private static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		return ( CacheStoreMode ) settings.get( JPA_SHARED_CACHE_STORE_MODE );
	}
}
