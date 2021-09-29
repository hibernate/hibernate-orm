/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.HashMap;
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
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreLoadEventListener;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockOptionsHelper;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_JPA_SHARED_CACHE_STORE_MODE;
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
public final class FastSessionServices {

	/**
	 * Default session properties
	 */
	final Map<String, Object> defaultSessionProperties;

	// All session events need to be iterated frequently; CollectionAction and EventAction also need
	// most of these very frequently:
	public final EventListenerGroup<AutoFlushEventListener> eventListenerGroup_AUTO_FLUSH;
	public final EventListenerGroup<ClearEventListener> eventListenerGroup_CLEAR;
	public final EventListenerGroup<DeleteEventListener> eventListenerGroup_DELETE;
	public final EventListenerGroup<DirtyCheckEventListener> eventListenerGroup_DIRTY_CHECK;
	public final EventListenerGroup<EvictEventListener> eventListenerGroup_EVICT;
	public final EventListenerGroup<FlushEntityEventListener> eventListenerGroup_FLUSH_ENTITY;
	public final EventListenerGroup<FlushEventListener> eventListenerGroup_FLUSH;
	public final EventListenerGroup<InitializeCollectionEventListener> eventListenerGroup_INIT_COLLECTION;
	public final EventListenerGroup<LoadEventListener> eventListenerGroup_LOAD;
	public final EventListenerGroup<LockEventListener> eventListenerGroup_LOCK;
	public final EventListenerGroup<MergeEventListener> eventListenerGroup_MERGE;
	public final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST;
	public final EventListenerGroup<PersistEventListener> eventListenerGroup_PERSIST_ONFLUSH;
	public final EventListenerGroup<PostCollectionRecreateEventListener> eventListenerGroup_POST_COLLECTION_RECREATE;
	public final EventListenerGroup<PostCollectionRemoveEventListener> eventListenerGroup_POST_COLLECTION_REMOVE;
	public final EventListenerGroup<PostCollectionUpdateEventListener> eventListenerGroup_POST_COLLECTION_UPDATE;
	public final EventListenerGroup<PostDeleteEventListener> eventListenerGroup_POST_COMMIT_DELETE;
	public final EventListenerGroup<PostDeleteEventListener> eventListenerGroup_POST_DELETE;
	public final EventListenerGroup<PostInsertEventListener> eventListenerGroup_POST_COMMIT_INSERT;
	public final EventListenerGroup<PostInsertEventListener> eventListenerGroup_POST_INSERT;
	public final EventListenerGroup<PostLoadEventListener> eventListenerGroup_POST_LOAD; //Frequently used by 2LC initialization:
	public final EventListenerGroup<PostUpdateEventListener> eventListenerGroup_POST_COMMIT_UPDATE;
	public final EventListenerGroup<PostUpdateEventListener> eventListenerGroup_POST_UPDATE;
	public final EventListenerGroup<PreCollectionRecreateEventListener> eventListenerGroup_PRE_COLLECTION_RECREATE;
	public final EventListenerGroup<PreCollectionRemoveEventListener> eventListenerGroup_PRE_COLLECTION_REMOVE;
	public final EventListenerGroup<PreCollectionUpdateEventListener> eventListenerGroup_PRE_COLLECTION_UPDATE;
	public final EventListenerGroup<PreDeleteEventListener> eventListenerGroup_PRE_DELETE;
	public final EventListenerGroup<PreInsertEventListener> eventListenerGroup_PRE_INSERT;
	public final EventListenerGroup<PreLoadEventListener> eventListenerGroup_PRE_LOAD;
	public final EventListenerGroup<PreUpdateEventListener> eventListenerGroup_PRE_UPDATE;
	public final EventListenerGroup<RefreshEventListener> eventListenerGroup_REFRESH;
	public final EventListenerGroup<ReplicateEventListener> eventListenerGroup_REPLICATE;
	public final EventListenerGroup<ResolveNaturalIdEventListener> eventListenerGroup_RESOLVE_NATURAL_ID;
	public final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_SAVE;
	public final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_SAVE_UPDATE;
	public final EventListenerGroup<SaveOrUpdateEventListener> eventListenerGroup_UPDATE;

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
		this.eventListenerGroup_FLUSH_ENTITY = listeners( eventListenerRegistry, EventType.FLUSH_ENTITY );
		this.eventListenerGroup_INIT_COLLECTION = listeners( eventListenerRegistry, EventType.INIT_COLLECTION );
		this.eventListenerGroup_LOAD = listeners( eventListenerRegistry, EventType.LOAD );
		this.eventListenerGroup_LOCK = listeners( eventListenerRegistry, EventType.LOCK );
		this.eventListenerGroup_MERGE = listeners( eventListenerRegistry, EventType.MERGE );
		this.eventListenerGroup_PERSIST = listeners( eventListenerRegistry, EventType.PERSIST );
		this.eventListenerGroup_PERSIST_ONFLUSH = listeners( eventListenerRegistry, EventType.PERSIST_ONFLUSH );
		this.eventListenerGroup_POST_COLLECTION_RECREATE = listeners( eventListenerRegistry, EventType.POST_COLLECTION_RECREATE );
		this.eventListenerGroup_POST_COLLECTION_REMOVE = listeners( eventListenerRegistry, EventType.POST_COLLECTION_REMOVE );
		this.eventListenerGroup_POST_COLLECTION_UPDATE = listeners( eventListenerRegistry, EventType.POST_COLLECTION_UPDATE );
		this.eventListenerGroup_POST_COMMIT_DELETE = listeners( eventListenerRegistry, EventType.POST_COMMIT_DELETE );
		this.eventListenerGroup_POST_COMMIT_INSERT = listeners( eventListenerRegistry, EventType.POST_COMMIT_INSERT );
		this.eventListenerGroup_POST_COMMIT_UPDATE = listeners( eventListenerRegistry, EventType.POST_COMMIT_UPDATE );
		this.eventListenerGroup_POST_DELETE = listeners( eventListenerRegistry, EventType.POST_DELETE );
		this.eventListenerGroup_POST_INSERT = listeners( eventListenerRegistry, EventType.POST_INSERT );
		this.eventListenerGroup_POST_LOAD = listeners( eventListenerRegistry, EventType.POST_LOAD );
		this.eventListenerGroup_POST_UPDATE = listeners( eventListenerRegistry, EventType.POST_UPDATE );
		this.eventListenerGroup_PRE_COLLECTION_RECREATE = listeners( eventListenerRegistry, EventType.PRE_COLLECTION_RECREATE );
		this.eventListenerGroup_PRE_COLLECTION_REMOVE = listeners( eventListenerRegistry, EventType.PRE_COLLECTION_REMOVE );
		this.eventListenerGroup_PRE_COLLECTION_UPDATE = listeners( eventListenerRegistry, EventType.PRE_COLLECTION_UPDATE );
		this.eventListenerGroup_PRE_DELETE = listeners( eventListenerRegistry, EventType.PRE_DELETE );
		this.eventListenerGroup_PRE_INSERT = listeners( eventListenerRegistry, EventType.PRE_INSERT );
		this.eventListenerGroup_PRE_LOAD = listeners( eventListenerRegistry, EventType.PRE_LOAD );
		this.eventListenerGroup_PRE_UPDATE = listeners( eventListenerRegistry, EventType.PRE_UPDATE );
		this.eventListenerGroup_REFRESH = listeners( eventListenerRegistry, EventType.REFRESH );
		this.eventListenerGroup_REPLICATE = listeners( eventListenerRegistry, EventType.REPLICATE );
		this.eventListenerGroup_RESOLVE_NATURAL_ID = listeners( eventListenerRegistry, EventType.RESOLVE_NATURAL_ID );
		this.eventListenerGroup_SAVE = listeners( eventListenerRegistry, EventType.SAVE );
		this.eventListenerGroup_SAVE_UPDATE = listeners( eventListenerRegistry, EventType.SAVE_UPDATE );
		this.eventListenerGroup_UPDATE = listeners( eventListenerRegistry, EventType.UPDATE );

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

	private static FlushMode initializeDefaultFlushMode(Map<String, Object> defaultSessionProperties) {
		final Object setting = NullnessHelper.coalesceSuppliedValues(
				() -> defaultSessionProperties.get( AvailableSettings.FLUSH_MODE ),
				() -> {
					final Object oldSetting = defaultSessionProperties.get( org.hibernate.jpa.AvailableSettings.FLUSH_MODE );
					if ( oldSetting != null ) {
						DeprecationLogger.DEPRECATION_LOGGER.deprecatedSetting(
								org.hibernate.jpa.AvailableSettings.FLUSH_MODE,
								AvailableSettings.FLUSH_MODE
						);
					}
					return oldSetting;
				}
		);
		return ConfigurationHelper.getFlushMode( setting, FlushMode.AUTO );
	}

	private static LockOptions initializeDefaultLockOptions(final Map<String, Object> defaultSessionProperties) {
		LockOptions def = new LockOptions();
		LockOptionsHelper.applyPropertiesToLockOptions( defaultSessionProperties, () -> def );
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

	private static Map<String, Object> initializeDefaultSessionProperties(SessionFactoryImpl sf) {
		HashMap<String,Object> p = new HashMap<>();

		//Static defaults:
		p.putIfAbsent( AvailableSettings.FLUSH_MODE, FlushMode.AUTO.name() );
		p.putIfAbsent( JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		p.putIfAbsent( JAKARTA_JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		p.putIfAbsent( JPA_LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		p.putIfAbsent( JAKARTA_JPA_LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		p.putIfAbsent( JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		p.putIfAbsent( JAKARTA_JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		p.putIfAbsent( JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );
		p.putIfAbsent( JAKARTA_JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );

		//Defaults defined by SessionFactory configuration:
		final String[] ENTITY_MANAGER_SPECIFIC_PROPERTIES = {
				JPA_LOCK_SCOPE,
				JAKARTA_JPA_LOCK_SCOPE,
				JPA_LOCK_TIMEOUT,
				JAKARTA_JPA_LOCK_TIMEOUT,
				AvailableSettings.FLUSH_MODE,
				JPA_SHARED_CACHE_RETRIEVE_MODE,
				JAKARTA_JPA_SHARED_CACHE_RETRIEVE_MODE,
				JPA_SHARED_CACHE_STORE_MODE,
				JAKARTA_JPA_SHARED_CACHE_STORE_MODE,
				QueryHints.SPEC_HINT_TIMEOUT,
				QueryHints.JAKARTA_SPEC_HINT_TIMEOUT
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
		final CacheRetrieveMode cacheRetrieveMode = (CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		if ( cacheRetrieveMode == null ) {
			return (CacheRetrieveMode) settings.get( JAKARTA_JPA_SHARED_CACHE_RETRIEVE_MODE );
		}
		return cacheRetrieveMode;
	}

	private static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		final CacheStoreMode cacheStoreMode = (CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		if ( cacheStoreMode == null ) {
			return ( CacheStoreMode ) settings.get( JAKARTA_JPA_SHARED_CACHE_STORE_MODE );
		}
		return cacheStoreMode;
	}

	public ConnectionObserverStatsBridge getDefaultJdbcObserver() {
		return defaultJdbcObservers;
	}

	public void firePostLoadEvent(final PostLoadEvent postLoadEvent) {
		eventListenerGroup_POST_LOAD.fireEventOnEachListener( postLoadEvent, PostLoadEventListener::onPostLoad );
	}
}
