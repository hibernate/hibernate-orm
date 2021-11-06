/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The action for performing an entity insertion, for entities not defined to use IDENTITY generation.
 *
 * @see EntityIdentityInsertAction
 */
public class EntityInsertAction extends AbstractEntityInsertAction {
	private Object version;
	private Object cacheEntry;

	/**
	 * Constructs an EntityInsertAction.
	 *
	 * @param id The entity identifier
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param version The current entity version value
	 * @param persister The entity's persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled.
	 * @param session The session
	 */
	public EntityInsertAction(
			Serializable id,
			Object[] state,
			Object instance,
			Object version,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SharedSessionContractImplementor session) {
		super( id, state, instance, isVersionIncrementDisabled, persister, session );
		this.version = version;
	}

	public Object getVersion() {
		return version;
	}

	public void setVersion(Object version) {
		this.version = version;
	}

	protected Object getCacheEntry() {
		return cacheEntry;
	}

	protected void setCacheEntry(Object cacheEntry) {
		this.cacheEntry = cacheEntry;
	}

	@Override
	public boolean isEarlyInsert() {
		return false;
	}

	@Override
	protected EntityKey getEntityKey() {
		return getSession().generateEntityKey( getId(), getPersister() );
	}

	@Override
	public void execute() throws HibernateException {
		nullifyTransientReferencesIfNotAlready();

		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();
		final Serializable id = getId();

		final boolean veto = preInsert();

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) {
			
			persister.insert( id, getState(), instance, session );
			PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			final EntityEntry entry = persistenceContext.getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			
			entry.postInsert( getState() );
	
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( id, instance, getState(), session );
				if ( persister.isVersionPropertyGenerated() ) {
					version = Versioning.getVersion( getState(), persister );
				}
				entry.postUpdate( instance, getState(), version );
			}

			persistenceContext.registerInsertedKey( persister, getId() );
		}

		final SessionFactoryImplementor factory = session.getFactory();

		final StatisticsImplementor statistics = factory.getStatistics();
		if ( isCachePutEnabled( persister, session ) ) {
			final CacheEntry ce = persister.buildCacheEntry(
					instance,
					getState(),
					version,
					session
			);
			cacheEntry = persister.getCacheEntryStructure().structure( ce );
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey( id, persister, factory, session.getTenantIdentifier() );

			final boolean put = cacheInsert( persister, ck );

			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}

		handleNaturalIdPostSaveNotifications( id );

		EntityMetamodel entityMetamodel = getPersister().getEntityMetamodel();

		if (entityMetamodel.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading()) {
			PersistentAttributeInterceptable interceptable = ((PersistentAttributeInterceptable) instance );
			PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
			final BytecodeEnhancementMetadata enhancementMetadata = entityMetamodel.getBytecodeEnhancementMetadata();
			final LazyAttributesMetadata lazyAttributesMetadata = enhancementMetadata.getLazyAttributesMetadata();
			if (interceptor == null) {
				Type[] types = persister.getPropertyTypes();
				final String[] propertyNames = persister.getPropertyNames();
				final CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
				boolean needsInterceptor = false;
				Set<String> initializedLazyFields = new HashSet<>();
				for ( int i = 0; i < types.length; i++) {
					if(!types[i].isAssociationType()) {
						continue;
					}
					final String propertyName = propertyNames[i];
					CascadeStyle cascadeStyle = cascadeStyles[i];
					if(cascadeStyle != CascadeStyles.NONE) {
						initializedLazyFields.add(propertyName);
						continue;
					}
					Object propertyValue = persister.getEntityTuplizer().getGetter(i).get(instance);
					if(propertyValue == null) {
						if(types[i].isCollectionType()) {
							needsInterceptor = true;
							continue;
						}
					}
					else {
						if(propertyValue instanceof PersistentAttributeInterceptable) {
							PersistentAttributeInterceptable interceptableProperty = ((PersistentAttributeInterceptable) propertyValue );
							PersistentAttributeInterceptor propertyInterceptor = interceptableProperty.$$_hibernate_getInterceptor();
							if(propertyInterceptor == null) {
								needsInterceptor = true;
								continue;
							}
							else {
								if(lazyAttributesMetadata.isLazyAttribute(propertyName)) {
									initializedLazyFields.add(propertyName);
								}
							}
						}
						if(types[i].isCollectionType()) {
							if(lazyAttributesMetadata.isLazyAttribute(propertyName)) {
								initializedLazyFields.add(propertyName);
							}
							continue;
						}
					}
				}
				if (needsInterceptor) {
					persister.getBytecodeEnhancementMetadata().injectEnhancedEntityAsProxyInterceptor(instance, getEntityKey(), session);
					interceptor = interceptable.$$_hibernate_getInterceptor();
					for (String propertyName : initializedLazyFields) {
						interceptor.attributeInitialized(propertyName);
					}
				}
			}
		}
		else {
			if (persister.hasProxy()) {
				Type[] types = persister.getPropertyTypes();
				final CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
				PersistenceContext persistenceContext = session.getPersistenceContextInternal();
				for ( int i = 0; i < types.length; i++) {
					if(!types[i].isAssociationType()) {
						continue;
					}
					
					final AssociationType type = (AssociationType) types[i];
					if(type instanceof EntityType) {
						EntityType entityType = (EntityType) type;
						if (entityType.isEager(null)) {
							continue;
						}
					}

					CascadeStyle cascadeStyle = cascadeStyles[i];
					if(cascadeStyle != CascadeStyles.NONE) {
						continue;
					}
					Object propertyValue = persister.getEntityTuplizer().getGetter(i).get(instance);

					if(types[i].isCollectionType()) {
						final CollectionPersister collectionPersister = session.getFactory().getMetamodel().collectionPersister( ((CollectionType) types[i]).getRole() );

						if ( !collectionPersister.isLazy()) {
							continue;
						}
						if ( collectionPersister.getCollectionType().hasHolder() ) {
							continue;
						}
						if ( collectionPersister.getKeyType().isComponentType()) {
							continue;
						}

						Serializable entityId = persister.getEntityTuplizer().getIdentifier(instance, session);
						final CollectionKey collectionKey = new CollectionKey( collectionPersister, entityId );
						PersistentCollection oldCollection = persistenceContext.getCollection(collectionKey);

						if(oldCollection != null) {
							if (!Hibernate.isInitialized(oldCollection)) {
								if (oldCollection == propertyValue) {
									continue;
								}
							}
						}
						if(propertyValue instanceof PersistentCollection && Hibernate.isInitialized(propertyValue)) {
							continue;
						}

						PersistentCollection newCollection = ((CollectionType)types[i]).instantiate( session, collectionPersister, entityId );
						if(propertyValue != null) {
							Hibernate.initialize(newCollection);
						}
						newCollection.setOwner( instance );
						persistenceContext.addUninitializedCollection( collectionPersister, newCollection, entityId);
						persister.getEntityTuplizer().setPropertyValue(instance, i, newCollection);
						continue;
					}

					if(propertyValue == null) {
						continue;
					}
					else {
						if(propertyValue instanceof HibernateProxy) {
							continue;
						}
						EntityPersister propertyPersister = null;
						try {
							propertyPersister = session.getEntityPersister(null, propertyValue);
						}
						catch (HibernateException he) {
							// dynamic map entity will fail to determine type
							continue;
						}
						if(propertyPersister.getEntityTuplizer().getProxyFactory() == null) {
							continue;
						}
						if(!propertyPersister.canExtractIdOutOfEntity()) {
							continue;
						}
						Serializable entityId = propertyPersister.getEntityTuplizer().getIdentifier(propertyValue, session);
						Object proxy = null;
						try {
							proxy = propertyPersister.createProxy(entityId, session);
							if(propertyValue != null) {
								Hibernate.initialize(proxy);
							}
						}
						catch (Exception e) {
							continue;
						}
						final EntityKey keyToLoad = session.generateEntityKey( entityId, propertyPersister );
						persistenceContext.addProxy(keyToLoad, proxy);
						persister.getEntityTuplizer().setPropertyValue(instance, i, proxy);
					}
				}
			}
		}
		postInsert();

		if ( statistics.isStatisticsEnabled() && !veto ) {
			statistics.insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	protected boolean cacheInsert(EntityPersister persister, Object ck) {
		SharedSessionContractImplementor session = getSession();
		try {
			session.getEventListenerManager().cachePutStart();
			return persister.getCacheAccessStrategy().insert( session, ck, cacheEntry, version);
		}
		finally {
			session.getEventListenerManager().cachePutEnd();
		}
	}

	protected void postInsert() {
		getFastSessionServices()
				.eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	private PostInsertEvent newPostInsertEvent() {
		return new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
	}

	protected void postCommitInsert(boolean success) {
		getFastSessionServices()
				.eventListenerGroup_POST_COMMIT_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, success ? PostInsertEventListener::onPostInsert : this::postCommitOnFailure );
	}

	private void postCommitOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener ) {
			((PostCommitInsertEventListener) listener).onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		boolean veto = false;

		final EventListenerGroup<PreInsertEventListener> listenerGroup = getFastSessionServices().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreInsertEvent event = new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) throws HibernateException {
		final EntityPersister persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final EntityDataAccess cache = persister.getCacheAccessStrategy();
			SessionFactoryImplementor factory = session.getFactory();
			final Object ck = cache.generateCacheKey( getId(), persister, factory, session.getTenantIdentifier() );
			final boolean put = cacheAfterInsert( cache, ck );

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.entityCachePut(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						cache.getRegion().getName()
				);
			}
		}
		postCommitInsert( success );
	}

	protected boolean cacheAfterInsert(EntityDataAccess cache, Object ck) {
		SharedSessionContractImplementor session = getSession();
		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		try {
			eventListenerManager.cachePutStart();
			return cache.afterInsert( session, ck, cacheEntry, version );
		}
		finally {
			eventListenerManager.cachePutEnd();
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		final EventListenerGroup<PostInsertEventListener> group = getFastSessionServices().eventListenerGroup_POST_COMMIT_INSERT;
		for ( PostInsertEventListener listener : group.listeners() ) {
			if ( listener.requiresPostCommitHandling( getPersister() ) ) {
				return true;
			}
		}
		return false;
	}

	protected boolean isCachePutEnabled(EntityPersister persister, SharedSessionContractImplementor session) {
		return persister.canWriteToCache()
				&& !persister.isCacheInvalidationRequired()
				&& session.getCacheMode().isPutEnabled();
	}

}
