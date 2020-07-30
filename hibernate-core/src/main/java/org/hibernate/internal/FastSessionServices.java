/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.PessimisticLockScope;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.service.spi.EventListenerGroup;
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
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockOptionsHelper;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

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
public final class FastSessionServices {

	private enum DefaultAvailableSetting {

		FLUSH_MODE( org.hibernate.jpa.AvailableSettings.FLUSH_MODE ),
		JPA_LOCK_SCOPE( org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE ),
		JPA_LOCK_TIMEOUT( org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT ),
		JPA_SHARED_CACHE_RETRIEVE_MODE( org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE ),
		JPA_SHARED_CACHE_STORE_MODE( org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE ),
		SPEC_HINT_TIMEOUT( QueryHints.SPEC_HINT_TIMEOUT );

		String key;

		DefaultAvailableSetting(String key) {
			this.key = key;
		}
	}

	/**
	 * Default session properties
	 */
	final EnumMap<DefaultAvailableSetting, Object> defaultSessionProperties;

	// All session events need to be iterated frequently:
	final EventListenerGroup<AutoFlushEventListener> eventListenerGroup_AUTO_FLUSH;
	final EventListenerGroup<ClearEventListener> eventListenerGroup_CLEAR;
	final EventListenerGroup<DeleteEventListener> eventListenerGroup_DELETE;
	final EventListenerGroup<DirtyCheckEventListener> eventListenerGroup_DIRTY_CHECK;
	final EventListenerGroup<EvictEventListener> eventListenerGroup_EVICT;
	final EventListenerGroup<FlushEventListener> eventListenerGroup_FLUSH;
	final EventListenerGroup<InitializeCollectionEventListener> eventListenerGroup_INIT_COLLECTION;
	final EventListenerGroup<LoadEventListener> eventListenerGroup_LOAD;
	final EventListenerGroup<LockEventListener> eventListenerGroup_LOCK;
	final EventListenerGroup<MergeEventListener> eventListenerGroup_MERGE;
	final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST;
	final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST_ONFLUSH;
	final EventListenerGroup<RefreshEventListener> eventListenerGroup_REFRESH;
	final EventListenerGroup<ReplicateEventListener> eventListenerGroup_REPLICATE;
	final EventListenerGroup<ResolveNaturalIdEventListener> eventListenerGroup_RESOLVE_NATURAL_ID;
	final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_SAVE;
	final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_SAVE_UPDATE;
	final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_UPDATE;

	//Frequently used by 2LC initialization:
	final EventListenerGroup<PostLoadEventListener> eventListenerGroup_POST_LOAD;

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
	final FlushMode initialSessionFlushMode;
	final boolean discardOnClose;
	final BaselineSessionEventsListenerBuilder defaultSessionEventListeners;
	final LockOptions defaultLockOptions;

	//Private fields:
	private final Dialect dialect;
	private final CacheStoreMode defaultCacheStoreMode;
	private final CacheRetrieveMode defaultCacheRetrieveMode;
	private final ConnectionObserverStatsBridge defaultJdbcObservers;

	FastSessionServices(SessionFactoryImpl sf) {
		Objects.requireNonNull( sf );
		final ServiceRegistryImplementor sr = sf.getServiceRegistry();
		final JdbcServices jdbcServices = sf.getJdbcServices();
		final SessionFactoryOptions sessionFactoryOptions = sf.getSessionFactoryOptions();

		// Pre-compute all iterators on Event listeners:
		final EventListenerRegistry eventListenerRegistry = sr.getService( EventListenerRegistry.class );
		this.eventListenerGroup_AUTO_FLUSH = listeners( eventListenerRegistry, EventType.AUTO_FLUSH );
		this.eventListenerGroup_CLEAR = listeners( eventListenerRegistry, EventType.CLEAR );
		this.eventListenerGroup_DELETE = listeners( eventListenerRegistry, EventType.DELETE );
		this.eventListenerGroup_DIRTY_CHECK = listeners( eventListenerRegistry, EventType.DIRTY_CHECK );
		this.eventListenerGroup_EVICT = listeners( eventListenerRegistry, EventType.EVICT );
		this.eventListenerGroup_FLUSH = listeners( eventListenerRegistry, EventType.FLUSH );
		this.eventListenerGroup_INIT_COLLECTION = listeners( eventListenerRegistry, EventType.INIT_COLLECTION );
		this.eventListenerGroup_LOAD = listeners( eventListenerRegistry, EventType.LOAD );
		this.eventListenerGroup_LOCK = listeners( eventListenerRegistry, EventType.LOCK );
		this.eventListenerGroup_MERGE = listeners( eventListenerRegistry, EventType.MERGE );
		this.eventListenerGroup_PERSIST = listeners( eventListenerRegistry, EventType.PERSIST );
		this.eventListenerGroup_PERSIST_ONFLUSH = listeners( eventListenerRegistry, EventType.PERSIST_ONFLUSH );
		this.eventListenerGroup_REFRESH = listeners( eventListenerRegistry, EventType.REFRESH );
		this.eventListenerGroup_REPLICATE = listeners( eventListenerRegistry, EventType.REPLICATE );
		this.eventListenerGroup_RESOLVE_NATURAL_ID = listeners( eventListenerRegistry, EventType.RESOLVE_NATURAL_ID );
		this.eventListenerGroup_SAVE = listeners( eventListenerRegistry, EventType.SAVE );
		this.eventListenerGroup_SAVE_UPDATE = listeners( eventListenerRegistry, EventType.SAVE_UPDATE );
		this.eventListenerGroup_UPDATE = listeners( eventListenerRegistry, EventType.UPDATE );
		this.eventListenerGroup_POST_LOAD = listeners( eventListenerRegistry, EventType.POST_LOAD );

		//Other highly useful constants:
		this.dialect = jdbcServices.getJdbcEnvironment().getDialect();
		this.disallowOutOfTransactionUpdateOperations = !sessionFactoryOptions.isAllowOutOfTransactionUpdateOperations();
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
		this.discardOnClose = sessionFactoryOptions.isReleaseResourcesOnCloseEnabled();
		this.defaultJdbcObservers = new ConnectionObserverStatsBridge( sf );
		this.defaultSessionEventListeners = sessionFactoryOptions.getBaselineSessionEventsListenerBuilder();
		this.defaultLockOptions = initializeDefaultLockOptions( defaultSessionProperties );
		this.initialSessionFlushMode = initializeDefaultFlushMode( defaultSessionProperties );
	}

	private static FlushMode initializeDefaultFlushMode(EnumMap<DefaultAvailableSetting, Object> defaultSessionProperties) {
		Object setMode = defaultSessionProperties.get( DefaultAvailableSetting.FLUSH_MODE );
		return ConfigurationHelper.getFlushMode( setMode, FlushMode.AUTO );
	}

	private static LockOptions initializeDefaultLockOptions(EnumMap<DefaultAvailableSetting, Object> defaultSessionProperties) {
		LockOptions def = new LockOptions();
		LockOptionsHelper.applyPropertiesToLockOptions( key -> {
			switch (key) {
				case AvailableSettings.JPA_LOCK_SCOPE:
					return defaultSessionProperties.get( DefaultAvailableSetting.JPA_LOCK_SCOPE );
				case AvailableSettings.JPA_LOCK_TIMEOUT:
					return defaultSessionProperties.get( DefaultAvailableSetting.JPA_LOCK_TIMEOUT );
				default:
					throw new IllegalStateException( "unknown property key: " + key );
			}
		}, () -> def );
		return def;
	}

	private static <T> EventListenerGroup<T> listeners(EventListenerRegistry elr, EventType<T> type) {
		return elr.getEventListenerGroup( type );
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

	private static EnumMap<DefaultAvailableSetting, Object> initializeDefaultSessionProperties(SessionFactoryImpl sf) {
		EnumMap<DefaultAvailableSetting, Object> p = new EnumMap<>( DefaultAvailableSetting.class );

		//Static defaults:
		p.put( DefaultAvailableSetting.FLUSH_MODE, FlushMode.AUTO.name() );
		p.put( DefaultAvailableSetting.JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		p.put( DefaultAvailableSetting.JPA_LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		p.put( DefaultAvailableSetting.JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		p.put( DefaultAvailableSetting.JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );

		//Defaults defined by SessionFactory configuration:
		final Map<String, Object> properties = sf.getProperties();
		for ( DefaultAvailableSetting settingEnum : DefaultAvailableSetting.values() ) {
			if ( properties.containsKey( settingEnum.key ) ) {
				p.put( settingEnum, properties.get( settingEnum.key ) );
			}
		}
		return p;
	}

	/**
	 * @param properties the Session properties
	 * @return either the CacheStoreMode as defined in the Session specific properties, or as defined in the
	 *  properties shared across all sessions (the defaults).
	 */
	CacheStoreMode getCacheStoreMode(EnumMap<DefaultAvailableSetting, Object> properties) {
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
	CacheRetrieveMode getCacheRetrieveMode(EnumMap<DefaultAvailableSetting, Object> properties) {
		if ( properties == null ) {
			return this.defaultCacheRetrieveMode;
		}
		else {
			return determineCacheRetrieveMode( properties );
		}
	}

	private static CacheRetrieveMode determineCacheRetrieveMode(EnumMap<DefaultAvailableSetting, Object> settings) {
		return ( CacheRetrieveMode ) settings.get( DefaultAvailableSetting.JPA_SHARED_CACHE_RETRIEVE_MODE );
	}

	private static CacheStoreMode determineCacheStoreMode(EnumMap<DefaultAvailableSetting, Object> settings) {
		return ( CacheStoreMode ) settings.get( DefaultAvailableSetting.JPA_SHARED_CACHE_STORE_MODE );
	}

	public ConnectionObserverStatsBridge getDefaultJdbcObserver() {
		return defaultJdbcObservers;
	}

	public void firePostLoadEvent(final PostLoadEvent postLoadEvent) {
		eventListenerGroup_POST_LOAD.fireEventOnEachListener( postLoadEvent, PostLoadEventListener::onPostLoad );
	}
}
