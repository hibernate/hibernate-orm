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
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * The action for performing entity insertions when entity is using IDENTITY column identifier generation
 *
 * @see EntityInsertAction
 */
public class EntityIdentityInsertAction extends AbstractEntityInsertAction  {

	private final boolean isDelayed;
	private final EntityKey delayedEntityKey;
	private EntityKey entityKey;
	private Object generatedId;

	/**
	 * Constructs an EntityIdentityInsertAction
	 *
	 * @param state The current (extracted) entity state
	 * @param instance The entity instance
	 * @param persister The entity persister
	 * @param isVersionIncrementDisabled Whether version incrementing is disabled
	 * @param session The session
	 * @param isDelayed Are we in a situation which allows the insertion to be delayed?
	 *
	 * @throws HibernateException Indicates an illegal state
	 */
	public EntityIdentityInsertAction(
			Object[] state,
			Object instance,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SharedSessionContractImplementor session,
			boolean isDelayed) {
		super(
				( isDelayed ? generateDelayedPostInsertIdentifier() : null ),
				state,
				instance,
				isVersionIncrementDisabled,
				persister,
				session
		);
		this.isDelayed = isDelayed;
		this.delayedEntityKey = isDelayed ? generateDelayedEntityKey() : null;
	}

	@Override
	public void execute() throws HibernateException {
		nullifyTransientReferencesIfNotAlready();

		final EntityPersister persister = getPersister();
		final SharedSessionContractImplementor session = getSession();
		final Object instance = getInstance();

		setVeto( preInsert() );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !isVeto() ) {
			generatedId = persister.insert( getState(), instance, session );
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( generatedId, instance, getState(), session );
			}
			//need to do that here rather than in the save event listener to let
			//the post insert events to have a id-filled entity when IDENTITY is used (EJB3)
			persister.setIdentifier( instance, generatedId, session );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			persistenceContext.registerInsertedKey( getPersister(), generatedId );
			entityKey = session.generateEntityKey( generatedId, persister );
			persistenceContext.checkUniqueness( entityKey, getInstance() );
		}


		//TODO: this bit actually has to be called after all cascades!
		//      but since identity insert is called *synchronously*,
		//      instead of asynchronously as other actions, it isn't
		/*if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			cacheEntry = new CacheEntry(object, persister, session);
			persister.getCache().insert(generatedId, cacheEntry);
		}*/

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
					Object propertyValue = persister.getPropertyValue(instance, i);
					needsInterceptor = needsInterceptor ||
							ifNeedsInterceptor(propertyValue, propertyNames[i], cascadeStyles[i], initializedLazyFields,
									lazyAttributesMetadata.isLazyAttribute(propertyNames[i]), types[i]) ;
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
					Object propertyValue = persister.getPropertyValue(instance, i);

					if (!proxyAfterInsert(instance, types[i], propertyValue, cascadeStyles[i], session)) {
						continue;
					}

					if (types[i].isCollectionType()) {
						Object entityId = persister.getIdentifier(instance, session);
						final CollectionPersister collectionPersister = session.getFactory().getMetamodel().collectionPersister(((CollectionType) types[i]).getRole());
						PersistentCollection newCollection = ((CollectionType) types[i]).instantiate(session, collectionPersister, entityId);
						if (propertyValue != null) {
							Hibernate.initialize(newCollection);
						}
						newCollection.setOwner(instance);
						persistenceContext.addUninitializedCollection(collectionPersister, newCollection, entityId);
						persister.setPropertyValue(instance, i, newCollection);
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
					if (propertyPersister.getRepresentationStrategy().getProxyFactory() == null
							|| !propertyPersister.canExtractIdOutOfEntity()) {
						continue;
					}
					Object propertyEntityId = propertyPersister.getIdentifier(propertyValue, session);
					Object proxy = null;
					try {
						proxy = propertyPersister.createProxy(propertyEntityId, session);
						if (propertyValue != null) {
							Hibernate.initialize(proxy);
						}
					}
					catch (Exception e) {
						continue;
					}
					final EntityKey keyToLoad = session.generateEntityKey(propertyEntityId, propertyPersister);
					persistenceContext.addProxy(keyToLoad, proxy);
					persister.setPropertyValue(instance, i, proxy);
				}
			}
		}

		postInsert();

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() && !isVeto() ) {
			statistics.insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	private boolean proxyAfterInsert(Object instance, Type type, Object propertyValue, CascadeStyle cascadeStyle, SharedSessionContractImplementor session) {
		if(!type.isAssociationType()
				|| propertyValue instanceof HibernateProxy
				|| cascadeStyle != CascadeStyles.NONE) {
			return false;
		}

		if(type instanceof EntityType && ((EntityType) type).isEager(null)) {
			return false;
		}

		if(type.isCollectionType()) {
			final CollectionPersister collectionPersister = session.getFactory().getMetamodel().collectionPersister( ((CollectionType) type).getRole() );

			if ( !collectionPersister.isLazy()
					|| collectionPersister.getCollectionType().hasHolder()
					|| collectionPersister.getKeyType().isComponentType()
					|| (propertyValue instanceof PersistentCollection && Hibernate.isInitialized(propertyValue))) {
				return false;
			}

			Object entityId = getPersister().getIdentifier(instance, session);
			final CollectionKey collectionKey = new CollectionKey( collectionPersister, entityId );
			PersistentCollection oldCollection = session.getPersistenceContext().getCollection(collectionKey);

			if(oldCollection != null
					&& !Hibernate.isInitialized(oldCollection)
					&& oldCollection == propertyValue) {
				return false;
			}

			return true;
		}

		if(propertyValue == null) {
			return false;
		}

		return true;
	}

	private boolean ifNeedsInterceptor(Object propertyValue, String propertyName, CascadeStyle cascadeStyle, Set<String> initializedLazyFields, boolean lazy, Type type) {
		if(cascadeStyle != CascadeStyles.NONE) {
			initializedLazyFields.add(propertyName);
			return false;
		}
		if(propertyValue == null && type.isCollectionType()) {
			return true;
		}
		else {
			if(propertyValue instanceof PersistentAttributeInterceptable) {
				PersistentAttributeInterceptable interceptableProperty = ((PersistentAttributeInterceptable) propertyValue);
				PersistentAttributeInterceptor propertyInterceptor = interceptableProperty.$$_hibernate_getInterceptor();
				if (propertyInterceptor == null) {
					return true;
				}
				if (lazy) {
					initializedLazyFields.add(propertyName);
				}
			}
			if(type.isCollectionType() && lazy) {
				initializedLazyFields.add(propertyName);
			}
		}
		return false;
	}

	@Override
	public boolean needsAfterTransactionCompletion() {
		//TODO: simply remove this override if we fix the above todos
		return hasPostCommitEventListeners();
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

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
		//TODO: reenable if we also fix the above todo
		/*EntityPersister persister = getEntityPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			persister.getCache().afterInsert( getGeneratedId(), cacheEntry );
		}*/
		postCommitInsert( success );
	}

	protected void postInsert() {
		if ( isDelayed ) {
			eventSource().getPersistenceContextInternal().replaceDelayedEntityIdentityInsertKeys( delayedEntityKey, generatedId );
		}
		getFastSessionServices()
				.eventListenerGroup_POST_INSERT
				.fireLazyEventOnEachListener( this::newPostInsertEvent, PostInsertEventListener::onPostInsert );
	}

	PostInsertEvent newPostInsertEvent() {
		return new PostInsertEvent(
				getInstance(),
				generatedId,
				getState(),
				getPersister(),
				eventSource()
		);
	}

	protected void postCommitInsert(boolean success) {
		getFastSessionServices()
			.eventListenerGroup_POST_COMMIT_INSERT
			.fireLazyEventOnEachListener( this::newPostInsertEvent, success ? PostInsertEventListener::onPostInsert : this::postCommitInsertOnFailure );
	}

	private void postCommitInsertOnFailure(PostInsertEventListener listener, PostInsertEvent event) {
		if ( listener instanceof PostCommitInsertEventListener ) {
			((PostCommitInsertEventListener) listener).onPostInsertCommitFailed( event );
		}
		else {
			//default to the legacy implementation that always fires the event
			listener.onPostInsert( event );
		}
	}

	protected boolean preInsert() {
		final EventListenerGroup<PreInsertEventListener> listenerGroup = getFastSessionServices().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			// NO_VETO
			return false;
		}
		boolean veto = false;
		final PreInsertEvent event = new PreInsertEvent( getInstance(), null, getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	/**
	 * Access to the generated identifier
	 *
	 * @return The generated identifier
	 */
	public final Object getGeneratedId() {
		return generatedId;
	}

	protected void setGeneratedId(Object generatedId) {
		this.generatedId = generatedId;
	}

	/**
	 * Access to the delayed entity key
	 *
	 * @return The delayed entity key
	 *
	 * @deprecated No Hibernate code currently uses this method
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public EntityKey getDelayedEntityKey() {
		return delayedEntityKey;
	}

	@Override
	public boolean isEarlyInsert() {
		return !isDelayed;
	}

	@Override
	protected EntityKey getEntityKey() {
		return entityKey != null ? entityKey : delayedEntityKey;
	}

	protected void setEntityKey(EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	private static DelayedPostInsertIdentifier generateDelayedPostInsertIdentifier() {
		return new DelayedPostInsertIdentifier();
	}

	protected EntityKey generateDelayedEntityKey() {
		if ( !isDelayed ) {
			throw new AssertionFailure( "cannot request delayed entity-key for early-insert post-insert-id generation" );
		}
		return getSession().generateEntityKey( getDelayedId(), getPersister() );
	}
}
