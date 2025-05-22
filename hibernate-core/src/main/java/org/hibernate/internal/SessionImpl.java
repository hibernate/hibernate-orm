/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.JDBCException;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnknownProfileException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EvictEvent;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.event.spi.LoadEventListener.LoadType;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.ResolveNaturalIdEvent;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.jpa.internal.LegacySpecHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.internal.IdentifierLoadAccessImpl;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.loader.internal.NaturalIdLoadAccessImpl;
import org.hibernate.loader.internal.SimpleNaturalIdLoadAccessImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.UnknownSqlResultSetMappingException;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.internal.SessionStatisticsImpl;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.metamodel.Metamodel;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.cfg.AvailableSettings.CRITERIA_COPY_TREE;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_BATCH_FETCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.AvailableSettings.USE_SUBSELECT_FETCH;
import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.event.spi.LoadEventListener.IMMEDIATE_LOAD;
import static org.hibernate.event.spi.LoadEventListener.INTERNAL_LOAD_EAGER;
import static org.hibernate.event.spi.LoadEventListener.INTERNAL_LOAD_LAZY;
import static org.hibernate.event.spi.LoadEventListener.INTERNAL_LOAD_NULLABLE;
import static org.hibernate.jpa.HibernateHints.HINT_BATCH_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_ENABLE_SUBSELECT_FETCH;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_PROFILE;
import static org.hibernate.jpa.HibernateHints.HINT_JDBC_BATCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.jpa.internal.util.CacheModeHelper.interpretCacheMode;
import static org.hibernate.jpa.internal.util.LockOptionsHelper.applyPropertiesToLockOptions;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Concrete implementation of the {@link Session} API.
 * <p>
 * Exposes two interfaces:
 * <ul>
 * <li>{@link Session} to the application, and
 * <li>{@link SessionImplementor} and {@link EventSource} (both SPI interfaces) to other subsystems.
 * </ul>
 * <p>
 * This class is not thread-safe.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Chris Cranford
 * @author Sanne Grinovero
 */
public class SessionImpl
		extends AbstractSharedSessionContract
		implements Serializable, SharedSessionContractImplementor, JdbcSessionOwner, SessionImplementor, EventSource,
				TransactionCoordinatorBuilder.Options, WrapperOptions, LoadAccessContext {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( SessionImpl.class );

	// Defaults to null which means the properties are the default
	// as defined in FastSessionServices#defaultSessionProperties
	private Map<String, Object> properties;

	private transient ActionQueue actionQueue;
	private transient StatefulPersistenceContext persistenceContext;

	private transient LoadQueryInfluencers loadQueryInfluencers;

	private LockOptions lockOptions;

	private boolean autoClear;
	private final boolean autoClose;

	private transient LoadEvent loadEvent; //cached LoadEvent instance

	private transient TransactionObserver transactionObserver;

	// TODO: this is unused and can be removed
	private transient boolean isEnforcingFetchGraph;

	public SessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );

		final HibernateMonitoringEvent sessionOpenEvent = getEventManager().beginSessionOpenEvent();

		persistenceContext = createPersistenceContext();
		actionQueue = createActionQueue();

		autoClear = options.shouldAutoClear();
		autoClose = options.shouldAutoClose();

		if ( options instanceof SharedSessionCreationOptions ) {
			final SharedSessionCreationOptions sharedOptions = (SharedSessionCreationOptions) options;
			final ActionQueue.TransactionCompletionProcesses transactionCompletionProcesses
					= sharedOptions.getTransactionCompletionProcesses();
			if ( sharedOptions.isTransactionCoordinatorShared() && transactionCompletionProcesses != null ) {
				actionQueue.setTransactionCompletionProcesses(
						transactionCompletionProcesses,
						true
				);
			}
		}

		loadQueryInfluencers = new LoadQueryInfluencers( factory, options );

		final StatisticsImplementor statistics = factory.getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.openSession();
		}

		if ( properties != null ) {
			//There might be custom properties for this session that affect the LockOptions state
			applyPropertiesToLockOptions( properties, this::getLockOptionsForWrite );
		}
		setCacheMode( fastSessionServices.initialSessionCacheMode );

		// NOTE : pulse() already handles auto-join-ability correctly
		getTransactionCoordinator().pulse();

		// do not override explicitly set flush mode ( SessionBuilder#flushMode() )
		if ( getHibernateFlushMode() == null ) {
			setHibernateFlushMode( getInitialFlushMode() );
		}

		setUpMultitenancy( factory, loadQueryInfluencers );

		if ( log.isTraceEnabled() ) {
			log.tracef( "Opened Session [%s] at timestamp: %s", getSessionIdentifier(), currentTimeMillis() );
		}

		getEventManager().completeSessionOpenEvent( sessionOpenEvent, this );
	}

	private FlushMode getInitialFlushMode() {
		return properties == null
				? fastSessionServices.initialSessionFlushMode
				: ConfigurationHelper.getFlushMode( getSessionProperty( HINT_FLUSH_MODE ), FlushMode.AUTO );
	}

	protected StatefulPersistenceContext createPersistenceContext() {
		return new StatefulPersistenceContext( this );
	}

	protected ActionQueue createActionQueue() {
		return new ActionQueue( this );
	}

	private LockOptions getLockOptionsForRead() {
		return this.lockOptions == null ? fastSessionServices.defaultLockOptions : this.lockOptions;
	}

	private LockOptions getLockOptionsForWrite() {
		if ( this.lockOptions == null ) {
			this.lockOptions = new LockOptions();
		}
		return this.lockOptions;
	}

	protected void applyQuerySettingsAndHints(SelectionQuery<?> query) {
		applyLockOptionsHint( query );
	}

	protected void applyLockOptionsHint(SelectionQuery<?> query) {
		final LockOptions lockOptionsForRead = getLockOptionsForRead();
		if ( lockOptionsForRead.getLockMode() != LockMode.NONE ) {
			query.setLockMode( getLockMode( lockOptionsForRead.getLockMode() ) );
		}

		final Object specQueryTimeout = LegacySpecHelper.getInteger(
				HINT_SPEC_QUERY_TIMEOUT,
				HINT_JAVAEE_QUERY_TIMEOUT,
				this::getSessionProperty
		);
		if ( specQueryTimeout != null ) {
			query.setHint( HINT_SPEC_QUERY_TIMEOUT, specQueryTimeout );
		}
	}

	protected void applyQuerySettingsAndHints(Query<?> query) {
		applyQuerySettingsAndHints( (SelectionQuery<?>) query );

		final Integer specLockTimeout = LegacySpecHelper.getInteger(
				HINT_SPEC_LOCK_TIMEOUT,
				HINT_JAVAEE_LOCK_TIMEOUT,
				this::getSessionProperty,
				// treat WAIT_FOREVER the same as null
				(value) -> !Integer.valueOf( LockOptions.WAIT_FOREVER ).equals( value )
		);
		if ( specLockTimeout != null ) {
			query.setHint( HINT_SPEC_LOCK_TIMEOUT, specLockTimeout );
		}
	}

	private Object getSessionProperty(final String name) {
		if ( properties == null ) {
			return fastSessionServices.defaultSessionProperties.get( name );
		}
		else {
			return properties.get( name );
		}
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new SharedSessionBuilderImpl( this );
	}

	@Override
	public void clear() {
		checkOpen();

		// Do not call checkTransactionSynchStatus() here -- if a delayed
		// afterCompletion exists, it can cause an infinite loop.
		pulseTransactionCoordinator();

		try {
			internalClear();
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e );
		}
	}

	private void internalClear() {
		persistenceContext.clear();
		actionQueue.clear();

		fastSessionServices.eventListenerGroup_CLEAR
				.fireLazyEventOnEachListener( this::createClearEvent, ClearEventListener::onClear );
	}

	private ClearEvent createClearEvent() {
		return new ClearEvent( this );
	}

	@Override
	public void close() throws HibernateException {
		if ( isClosed() ) {
			if ( getFactory().getSessionFactoryOptions().getJpaCompliance().isJpaClosedComplianceEnabled() ) {
				throw new IllegalStateException( "Illegal call to #close() on already closed Session/EntityManager" );
			}

			log.trace( "Already closed" );
			return;
		}

		closeWithoutOpenChecks();
	}

	public void closeWithoutOpenChecks() throws HibernateException {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Closing session [%s]", getSessionIdentifier() );
		}

		final EventManager eventManager = getEventManager();
		final HibernateMonitoringEvent sessionClosedEvent = eventManager.beginSessionClosedEvent();

		// todo : we want this check if usage is JPA, but not native Hibernate usage
		final SessionFactoryImplementor sessionFactory = getSessionFactory();
		try {
			if ( sessionFactory.getSessionFactoryOptions().isJpaBootstrap() ) {
				// Original hibernate-entitymanager EM#close behavior
				checkSessionFactoryOpen();
				checkOpenOrWaitingForAutoClose();
				if ( fastSessionServices.discardOnClose || !isTransactionInProgressAndNotMarkedForRollback() ) {
					super.close();
				}
				else {
					//Otherwise, session auto-close will be enabled by shouldAutoCloseSession().
					prepareForAutoClose();
				}
			}
			else {
				super.close();
			}
		}
		finally {
			final StatisticsImplementor statistics = sessionFactory.getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.closeSession();
			}

			eventManager.completeSessionClosedEvent( sessionClosedEvent, this );
		}
	}

	private boolean isTransactionInProgressAndNotMarkedForRollback() {
		if ( waitingForAutoClose ) {
			return getSessionFactory().isOpen()
				&& getTransactionCoordinator().isTransactionActive( false );
		}
		else {
			return !isClosed()
				&& getTransactionCoordinator().isTransactionActive( false );
		}
	}

	@Override
	protected boolean shouldCloseJdbcCoordinatorOnClose(boolean isTransactionCoordinatorShared) {
		if ( !isTransactionCoordinatorShared ) {
			return super.shouldCloseJdbcCoordinatorOnClose( isTransactionCoordinatorShared );
		}

		final ActionQueue actionQueue = getActionQueue();
		if ( actionQueue.hasBeforeTransactionActions() || actionQueue.hasAfterTransactionActions() ) {
			log.warn(
					"On close, shared Session had before/after transaction actions that have not yet been processed"
			);
		}
		return false;
	}

	@Override
	public boolean isAutoCloseSessionEnabled() {
		return autoClose;
	}

	@Override
	public boolean isOpen() {
		checkSessionFactoryOpen();
		checkTransactionSynchStatus();
		try {
			return !isClosed();
		}
		catch (HibernateException he) {
			throw getExceptionConverter().convert( he );
		}
	}

	protected void checkSessionFactoryOpen() {
		if ( !getFactory().isOpen() ) {
			log.debug( "Forcing Session/EntityManager closed as SessionFactory/EntityManagerFactory has been closed" );
			setClosed();
		}
	}

	private void managedFlush() {
		if ( isClosed() && !waitingForAutoClose ) {
			log.trace( "Skipping auto-flush due to session closed" );
			return;
		}
		log.trace( "Automatically flushing session" );
		doFlush();
	}

	@Override
	public boolean shouldAutoClose() {
		if ( waitingForAutoClose ) {
			return true;
		}
		else if ( isClosed() ) {
			return false;
		}
		else {
			// JPA technically requires that this be a PersistentUnityTransactionType#JTA to work,
			// but we do not assert that here...
			//return isAutoCloseSessionEnabled() && getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();
			return isAutoCloseSessionEnabled();
		}
	}

	private void managedClose() {
		log.trace( "Automatically closing session" );
		closeWithoutOpenChecks();
	}

	@Override
	public void setAutoClear(boolean enabled) {
		checkOpenOrWaitingForAutoClose();
		autoClear = enabled;
	}

	public void afterOperation(boolean success) {
		if ( !isTransactionInProgress() ) {
			getJdbcCoordinator().afterTransaction();
		}
	}

	@Override
	public void addEventListeners(SessionEventListener... listeners) {
		getEventListenerManager().addListener( listeners );
	}

	/**
	 * clear all the internal collections, just
	 * to help the garbage collector, does not
	 * clear anything that is needed during the
	 * afterTransactionCompletion() phase
	 */
	@Override
	protected void cleanupOnClose() {
		persistenceContext.clear();
	}

	@Override
	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		if ( object == null ) {
			throw new NullPointerException( "null object passed to getCurrentLockMode()" );
		}

		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			object = lazyInitializer.getImplementation( this );
			if ( object == null ) {
				return LockMode.NONE;
			}
		}

		final EntityEntry e = persistenceContext.getEntry( object );
		if ( e == null ) {
			throw new TransientObjectException( "Given object not associated with the session" );
		}

		if ( e.getStatus().isDeletedOrGone() ) {
			throw new ObjectDeletedException(
					"The given object was deleted",
					e.getId(),
					e.getPersister().getEntityName()
			);
		}

		return e.getLockMode();
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		// todo : should this get moved to PersistentContext?
		// logically, is PersistentContext the "thing" to which an interceptor gets attached?
		final Object result = persistenceContext.getEntity( key );
		if ( result == null ) {
			final Object newObject = getInterceptor().getEntity( key.getEntityName(), key.getIdentifier() );
			if ( newObject != null ) {
				lock( newObject, LockMode.NONE );
			}
			return newObject;
		}
		else {
			return result;
		}
	}

	protected void checkNoUnresolvedActionsBeforeOperation() {
		if ( persistenceContext.getCascadeLevel() == 0 && actionQueue.hasUnresolvedEntityInsertActions() ) {
			throw new IllegalStateException( "There are delayed insert actions before operation as cascade level 0." );
		}
	}

	protected void checkNoUnresolvedActionsAfterOperation() {
		if ( persistenceContext.getCascadeLevel() == 0 ) {
			actionQueue.checkNoUnresolvedActionsAfterOperation();
		}
		delayedAfterCompletion();
	}

	@Override
	public void delayedAfterCompletion() {
		TransactionCoordinator coordinator = getTransactionCoordinator();
		if ( coordinator instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) coordinator).getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}

	@Override
	public void pulseTransactionCoordinator() {
		super.pulseTransactionCoordinator();
	}

	@Override
	public void checkOpenOrWaitingForAutoClose() {
		super.checkOpenOrWaitingForAutoClose();
	}

	// saveOrUpdate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @Deprecated
	public void saveOrUpdate(Object object) throws HibernateException {
		saveOrUpdate( null, object );
	}

	@Override @Deprecated
	public void saveOrUpdate(String entityName, Object obj) throws HibernateException {
		fireSaveOrUpdate( new SaveOrUpdateEvent( entityName, obj, this ) );
	}

	private void fireSaveOrUpdate(final SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		fastSessionServices.eventListenerGroup_SAVE_UPDATE
				.fireEventOnEachListener( event, SaveOrUpdateEventListener::onSaveOrUpdate );
		checkNoUnresolvedActionsAfterOperation();
	}

	// save() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @Deprecated
	public Object save(Object obj) throws HibernateException {
		return save( null, obj );
	}

	@Override @Deprecated
	public Object save(String entityName, Object object) throws HibernateException {
		return fireSave( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private Object fireSave(final SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		fastSessionServices.eventListenerGroup_SAVE
				.fireEventOnEachListener( event, SaveOrUpdateEventListener::onSaveOrUpdate );
		checkNoUnresolvedActionsAfterOperation();
		return event.getResultId();
	}


	// update() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @Deprecated
	public void update(Object obj) throws HibernateException {
		update( null, obj );
	}

	@Override @Deprecated
	public void update(String entityName, Object object) throws HibernateException {
		fireUpdate( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private void fireUpdate(SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		fastSessionServices.eventListenerGroup_UPDATE
				.fireEventOnEachListener( event, SaveOrUpdateEventListener::onSaveOrUpdate );
		checkNoUnresolvedActionsAfterOperation();
	}


	// lock() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void lock(Object object, LockOptions lockOptions) {
		fireLock( new LockEvent( object, lockOptions, this ) );
	}

	@Override
	public void lock(String entityName, Object object, LockOptions lockOptions) {
		fireLock( new LockEvent( entityName, object, lockOptions, this ) );
	}

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		fireLock( new LockEvent( entityName, object, lockMode, this ) );
	}

	@Override @Deprecated
	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return new LockRequestImpl( lockOptions );
	}

	@Override
	public void lock(Object object, LockMode lockMode) throws HibernateException {
		fireLock( new LockEvent( object, lockMode, this ) );
	}

	private void fireLock(String entityName, Object object, LockOptions options) {
		fireLock( new LockEvent( entityName, object, options, this ) );
	}

	private void fireLock(Object object, LockOptions options) {
		fireLock( new LockEvent( object, options, this ) );
	}

	private void fireLock(LockEvent event) {
		checkOpen();
		pulseTransactionCoordinator();
		fastSessionServices.eventListenerGroup_LOCK
				.fireEventOnEachListener( event, LockEventListener::onLock );
		delayedAfterCompletion();
	}

	// persist() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void persist(String entityName, Object object) throws HibernateException {
		checkOpen();
		firePersist( new PersistEvent( entityName, object, this ) );
	}

	@Override
	public void persist(Object object) throws HibernateException {
		checkOpen();
		firePersist( new PersistEvent( null, object, this ) );
	}

	@Override
	public void persist(String entityName, Object object, PersistContext copiedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		firePersist( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersist(final PersistEvent event) {
		Throwable originalException = null;
		try {
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();

			fastSessionServices.eventListenerGroup_PERSIST
					.fireEventOnEachListener( event, PersistEventListener::onPersist );
		}
		catch (MappingException e) {
			originalException = getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch (RuntimeException e) {
			originalException = getExceptionConverter().convert( e );
		}
		catch (Throwable t1) {
			originalException = t1;
		}
		finally {
			Throwable suppressed = null;
			try {
				checkNoUnresolvedActionsAfterOperation();
			}
			catch (RuntimeException e) {
				suppressed = getExceptionConverter().convert( e );
			}
			catch (Throwable t2) {
				suppressed = t2;
			}
			if ( suppressed != null ) {
				if ( originalException == null ) {
					originalException = suppressed;
				}
				else {
					originalException.addSuppressed( suppressed );
				}
			}
		}
		if ( originalException != null ) {
			ExceptionHelper.doThrow( originalException );
		}
	}

	private void firePersist(final PersistContext copiedAlready, final PersistEvent event) {
		pulseTransactionCoordinator();

		try {
			//Uses a capturing lambda in this case as we need to carry the additional Map parameter:
			fastSessionServices.eventListenerGroup_PERSIST
					.fireEventOnEachListener( event, copiedAlready, PersistEventListener::onPersist );
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage() ) ) ;
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// persistOnFlush() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void persistOnFlush(String entityName, Object object, PersistContext copiedAlready) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		PersistEvent event = new PersistEvent( entityName, object, this );
		fastSessionServices.eventListenerGroup_PERSIST_ONFLUSH
				.fireEventOnEachListener( event, copiedAlready, PersistEventListener::onPersist );
		delayedAfterCompletion();
	}

	// merge() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @SuppressWarnings("unchecked")
	public <T> T merge(String entityName, T object) throws HibernateException {
		checkOpen();
		return (T) fireMerge( new MergeEvent( entityName, object, this ) );
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T merge(T object) throws HibernateException {
		checkOpen();
		return (T) fireMerge( new MergeEvent( null, object, this ));
	}

	@Override
	public void merge(String entityName, Object object, MergeContext copiedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		fireMerge( copiedAlready, new MergeEvent( entityName, object, this ) );
	}

	private Object fireMerge(MergeEvent event) {
		try {
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();
			fastSessionServices.eventListenerGroup_MERGE
					.fireEventOnEachListener( event, MergeEventListener::onMerge );
			checkNoUnresolvedActionsAfterOperation();
		}
		catch ( ObjectDeletedException sse ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}

		return event.getResult();
	}

	private void fireMerge(final MergeContext mergeContext, final MergeEvent event) {
		try {
			pulseTransactionCoordinator();
			fastSessionServices.eventListenerGroup_MERGE
					.fireEventOnEachListener( event, mergeContext, MergeEventListener::onMerge );
		}
		catch ( ObjectDeletedException sse ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// delete() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @Deprecated
	public void delete(Object object) throws HibernateException {
		checkOpen();
		fireDelete( new DeleteEvent( object, this ) );
	}

	@Override @Deprecated
	public void delete(String entityName, Object object) throws HibernateException {
		checkOpen();
		fireDelete( new DeleteEvent( entityName, object, this ) );
	}

	@Override
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, DeleteContext transientEntities)
			throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		final boolean removingOrphanBeforeUpates = persistenceContext.isRemovingOrphanBeforeUpates();
		final boolean traceEnabled = log.isTraceEnabled();
		if ( traceEnabled && removingOrphanBeforeUpates ) {
			logRemoveOrphanBeforeUpdates( "before continuing", entityName, object );
		}
		fireDelete(
				new DeleteEvent(
						entityName,
						object,
						isCascadeDeleteEnabled,
						removingOrphanBeforeUpates,
						this
				),
				transientEntities
		);
		if ( traceEnabled && removingOrphanBeforeUpates ) {
			logRemoveOrphanBeforeUpdates( "after continuing", entityName, object );
		}
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
		// ordering is improved.
		final boolean traceEnabled = log.isTraceEnabled();
		if ( traceEnabled ) {
			logRemoveOrphanBeforeUpdates( "begin", entityName, child );
		}
		persistenceContext.beginRemoveOrphanBeforeUpdates();
		try {
			checkOpenOrWaitingForAutoClose();
			fireDelete( new DeleteEvent( entityName, child, false, true, this ) );
		}
		finally {
			persistenceContext.endRemoveOrphanBeforeUpdates();
			if ( traceEnabled ) {
				logRemoveOrphanBeforeUpdates( "end", entityName, child );
			}
		}
	}

	private void logRemoveOrphanBeforeUpdates(String timing, String entityName, Object entity) {
		if ( log.isTraceEnabled() ) {
			final EntityEntry entityEntry = persistenceContext.getEntry( entity );
			log.tracef(
					"%s remove orphan before updates: [%s]",
					timing,
					entityEntry == null ? entityName : MessageHelper.infoString( entityName, entityEntry.getId() )
			);
		}
	}

	private void fireDelete(final DeleteEvent event) {
		try{
			pulseTransactionCoordinator();
			fastSessionServices.eventListenerGroup_DELETE
					.fireEventOnEachListener( event, DeleteEventListener::onDelete );
		}
		catch ( ObjectDeletedException sse ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void fireDelete(final DeleteEvent event, final DeleteContext transientEntities) {
		try{
			pulseTransactionCoordinator();
			fastSessionServices.eventListenerGroup_DELETE
					.fireEventOnEachListener( event, transientEntities, DeleteEventListener::onDelete );
		}
		catch ( ObjectDeletedException sse ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// load()/get() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void load(Object object, Object id) throws HibernateException {
		fireLoad( new LoadEvent( id, object, this, getReadOnlyFromLoadQueryInfluencers() ), LoadEventListener.RELOAD );
	}

	@Override @Deprecated
	public <T> T load(Class<T> entityClass, Object id) throws HibernateException {
		return this.byId( entityClass ).getReference( id );
	}

	@Override @Deprecated
	public Object load(String entityName, Object id) throws HibernateException {
		return this.byId( entityName ).getReference( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id) throws HibernateException {
		return this.byId( entityClass ).load( id );
	}

	@Override
	public Object get(String entityName, Object id) throws HibernateException {
		return this.byId( entityName ).load( id );
	}

	/**
	 * Load the data for the object with the specified id into a newly created object.
	 * This is only called when lazily initializing a proxy.
	 * Do NOT return a proxy.
	 */
	@Override
	public Object immediateLoad(String entityName, Object id) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			final EntityPersister persister = getFactory().getMappingMetamodel()
					.getEntityDescriptor( entityName );
			log.debugf( "Initializing proxy: %s", MessageHelper.infoString( persister, id, getFactory() ) );
		}
		LoadEvent event = loadEvent;
		loadEvent = null;
		event = recycleEventInstance( event, id, entityName );
		fireLoadNoChecks( event, IMMEDIATE_LOAD );
		final Object result = event.getResult();
		finishWithEventInstance( event );
		final LazyInitializer lazyInitializer = extractLazyInitializer( result );
		return lazyInitializer != null ? lazyInitializer.getImplementation() : result;
	}

	@Override
	public Object internalLoad(String entityName, Object id, boolean eager, boolean nullable) {
		final LoadType type = internalLoadType( eager, nullable );
		final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		final GraphSemantic semantic = effectiveEntityGraph.getSemantic();
		final RootGraphImplementor<?> graph = effectiveEntityGraph.getGraph();
		boolean clearedEffectiveGraph;
		if ( semantic == null
				|| graph.appliesTo( getFactory().getJpaMetamodel().entity( entityName ) ) ) {
			clearedEffectiveGraph = false;
		}
		else {
			log.debug("Clearing effective entity graph for subsequent-select");
			clearedEffectiveGraph = true;
			effectiveEntityGraph.clear();
		}
		try {
			LoadEvent event = loadEvent;
			loadEvent = null;
			event = recycleEventInstance( event, id, entityName );
			fireLoadNoChecks( event, type );
			final Object result = event.getResult();
			if ( !nullable ) {
				UnresolvableObjectException.throwIfNull( result, id, entityName );
			}
			finishWithEventInstance( event );
			return result;
		}
		finally {
			if ( clearedEffectiveGraph ) {
				effectiveEntityGraph.applyGraph( graph, semantic );
			}
		}
	}

	protected static LoadType internalLoadType(boolean eager, boolean nullable) {
		if ( nullable ) {
			return INTERNAL_LOAD_NULLABLE;
		}
		else {
			return eager ? INTERNAL_LOAD_EAGER : INTERNAL_LOAD_LAZY;
		}
	}

	/**
	 * Helper to avoid creating many new instances of LoadEvent: it's an allocation hot spot.
	 */
	private LoadEvent recycleEventInstance(final LoadEvent event, final Object id, final String entityName) {
		if ( event == null ) {
			return new LoadEvent( id, entityName, true, this, getReadOnlyFromLoadQueryInfluencers() );
		}
		else {
			event.setEntityClassName( entityName );
			event.setEntityId( id );
			event.setInstanceToLoad( null );
			return event;
		}
	}

	private void finishWithEventInstance(LoadEvent event) {
		if ( loadEvent == null ) {
			event.setEntityClassName( null );
			event.setEntityId( null );
			event.setInstanceToLoad( null );
			event.setResult( null );
			loadEvent = event;
		}
	}

	@Override @Deprecated
	public <T> T load(Class<T> entityClass, Object id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	@Override @Deprecated
	public <T> T load(Class<T> entityClass, Object id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).getReference( id );
	}

	@Override @Deprecated
	public Object load(String entityName, Object id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	@Override @Deprecated
	public Object load(String entityName, Object id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).getReference( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).load( id );
	}

	@Override
	public Object get(String entityName, Object id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override
	public Object get(String entityName, Object id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).load( id );
	}

	@Override
	public <T> IdentifierLoadAccessImpl<T> byId(String entityName) {
		return new IdentifierLoadAccessImpl<>( this, requireEntityPersister( entityName ) );
	}

	@Override
	public <T> IdentifierLoadAccessImpl<T> byId(Class<T> entityClass) {
		return new IdentifierLoadAccessImpl<>( this, requireEntityPersister( entityClass ) );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return new MultiIdentifierLoadAccessImpl<>( this, requireEntityPersister( entityClass ) );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(String entityName) {
		return new MultiIdentifierLoadAccessImpl<>( this, requireEntityPersister( entityName ) );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(String entityName) {
		return new NaturalIdLoadAccessImpl<>( this, requireEntityPersister( entityName ) );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return new NaturalIdLoadAccessImpl<>( this, requireEntityPersister( entityClass ) );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(String entityName) {
		return new SimpleNaturalIdLoadAccessImpl<>( this, requireEntityPersister( entityName ) );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return new SimpleNaturalIdLoadAccessImpl<>( this, requireEntityPersister( entityClass ) );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(Class<T> entityClass) {
		return new NaturalIdMultiLoadAccessStandard<>( requireEntityPersister( entityClass ), this );
	}

	@Override
	public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(String entityName) {
		return new NaturalIdMultiLoadAccessStandard<>( requireEntityPersister( entityName ), this );
	}

	@Override
	public void fireLoad(LoadEvent event, LoadType loadType) {
		checkOpenOrWaitingForAutoClose();
		fireLoadNoChecks( event, loadType );
		delayedAfterCompletion();
	}

	//Performance note:
	// This version of #fireLoad is meant to be invoked by internal methods only,
	// so to skip the session open, transaction synch, etc.. checks,
	// which have been proven to be not particularly cheap:
	// it seems they prevent these hot methods from being inlined.
	private void fireLoadNoChecks(final LoadEvent event, final LoadType loadType) {
		pulseTransactionCoordinator();
		fastSessionServices.eventListenerGroup_LOAD
				.fireEventOnEachListener( event, loadType, LoadEventListener::onLoad );
	}

	private void fireResolveNaturalId(final ResolveNaturalIdEvent event) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		fastSessionServices.eventListenerGroup_RESOLVE_NATURAL_ID
				.fireEventOnEachListener( event, ResolveNaturalIdEventListener::onResolveNaturalId );
		delayedAfterCompletion();
	}


	// refresh() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void refresh(Object object) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( null, object, this ) );
	}

	@Override @Deprecated
	public void refresh(String entityName, Object object) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( entityName, object, this ) );
	}

	@Override
	public void refresh(Object object, LockMode lockMode) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( object, lockMode, this ) );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
		checkOpen();
		refresh( null, object, lockOptions );
	}

	@Override @Deprecated
	public void refresh(String entityName, Object object, LockOptions lockOptions) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( entityName, object, lockOptions, this ) );
	}

	@Override
	public void refresh(String entityName, Object object, RefreshContext refreshedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		fireRefresh( refreshedAlready, new RefreshEvent( entityName, object, this ) );
	}

	private void fireRefresh(final RefreshEvent event) {
		try {
			if ( !getSessionFactory().getSessionFactoryOptions().isAllowRefreshDetachedEntity() ) {
				if ( event.getEntityName() != null ) {
					if ( !contains( event.getEntityName(), event.getObject() ) ) {
						throw new IllegalArgumentException( "Entity not managed" );
					}
				}
				else {
					if ( !contains( event.getObject() ) ) {
						throw new IllegalArgumentException( "Entity not managed" );
					}
				}
			}
			pulseTransactionCoordinator();
			fastSessionServices.eventListenerGroup_REFRESH
					.fireEventOnEachListener( event, RefreshEventListener::onRefresh );
		}
		catch (RuntimeException e) {
			if ( !getSessionFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				if ( e instanceof HibernateException ) {
					throw e;
				}
			}
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void fireRefresh(final RefreshContext refreshedAlready, final RefreshEvent event) {
		try {
			pulseTransactionCoordinator();
			fastSessionServices.eventListenerGroup_REFRESH
					.fireEventOnEachListener( event, refreshedAlready, RefreshEventListener::onRefresh );
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// replicate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void replicate(Object obj, ReplicationMode replicationMode) throws HibernateException {
		fireReplicate( new ReplicateEvent( obj, replicationMode, this ) );
	}

	@Override
	public void replicate(String entityName, Object obj, ReplicationMode replicationMode)
			throws HibernateException {
		fireReplicate( new ReplicateEvent( entityName, obj, replicationMode, this ) );
	}

	private void fireReplicate(final ReplicateEvent event) {
		checkOpen();
		pulseTransactionCoordinator();
		fastSessionServices.eventListenerGroup_REPLICATE
				.fireEventOnEachListener( event, ReplicateEventListener::onReplicate );
		delayedAfterCompletion();
	}


	// evict() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * remove any hard references to the entity that are held by the infrastructure
	 * (references held by application or other persistent instances are okay)
	 */
	@Override
	public void evict(Object object) throws HibernateException {
		checkOpen();
		pulseTransactionCoordinator();
		final EvictEvent event = new EvictEvent( object, this );
		fastSessionServices.eventListenerGroup_EVICT
				.fireEventOnEachListener( event, EvictEventListener::onEvict );
		delayedAfterCompletion();
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces) throws HibernateException {
		return autoFlushIfRequired( querySpaces, false );
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush)
			throws HibernateException {
		checkOpen();
		if ( !isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		AutoFlushEvent event = new AutoFlushEvent( querySpaces, skipPreFlush, this );
		fastSessionServices.eventListenerGroup_AUTO_FLUSH
				.fireEventOnEachListener( event, AutoFlushEventListener::onAutoFlush );
		return event.isFlushRequired();
	}

	@Override
	public void autoPreFlush(){
		checkOpen();
		if ( !isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return;
		}
		fastSessionServices.eventListenerGroup_AUTO_FLUSH
				.fireEventOnEachListener( this, AutoFlushEventListener::onAutoPreFlush );
	}



	@Override
	public boolean isDirty() throws HibernateException {
		checkOpen();
		pulseTransactionCoordinator();
		log.debug( "Checking session dirtiness" );
		if ( actionQueue.areInsertionsOrDeletionsQueued() ) {
			log.debug( "Session dirty (scheduled updates and insertions)" );
			return true;
		}
		DirtyCheckEvent event = new DirtyCheckEvent( this );
		fastSessionServices.eventListenerGroup_DIRTY_CHECK
				.fireEventOnEachListener( event, DirtyCheckEventListener::onDirtyCheck );
		delayedAfterCompletion();
		return event.isDirty();
	}

	@Override
	public void flush() throws HibernateException {
		checkOpen();
		doFlush();
	}

	private void doFlush() {
		pulseTransactionCoordinator();
		checkTransactionNeededForUpdateOperation();

		try {
			if ( persistenceContext.getCascadeLevel() > 0 ) {
				throw new HibernateException( "Flush during cascade is dangerous" );
			}

			FlushEvent event = new FlushEvent( this );
			fastSessionServices.eventListenerGroup_FLUSH
					.fireEventOnEachListener( event, FlushEventListener::onFlush );
			delayedAfterCompletion();
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public void setFlushMode(FlushModeType flushModeType) {
		checkOpen();
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
	}

	@Override
	public void forceFlush(EntityEntry entityEntry) throws HibernateException {
		forceFlush( entityEntry.getEntityKey() );
	}

	@Override
	public void forceFlush(EntityKey key) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Flushing to force deletion of re-saved object: %s",
					MessageHelper.infoString( key.getPersister(), key.getIdentifier(), getFactory() )
			);
		}

		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ObjectDeletedException(
					"deleted object would be re-saved by cascade (remove deleted object from associations)",
					key.getIdentifier(),
					key.getPersister().getEntityName()
			);
		}
		checkOpenOrWaitingForAutoClose();
		doFlush();
	}

	@Override
	public Object instantiate(String entityName, Object id) throws HibernateException {
		final EntityPersister persister = getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		return instantiate( persister, id );
	}

	/**
	 * give the interceptor an opportunity to override the default instantiation
	 */
	@Override
	public Object instantiate(EntityPersister persister, Object id) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		Object result = getInterceptor().instantiate(
				persister.getEntityName(),
				persister.getRepresentationStrategy(),
				id
		);
		if ( result == null ) {
			result = persister.instantiate( id, this );
		}
		delayedAfterCompletion();
		return result;
	}

	@Override
	public EntityPersister getEntityPersister(final String entityName, final Object object) {
		checkOpenOrWaitingForAutoClose();
		if ( entityName == null ) {
			return getFactory().getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( guessEntityName( object ) );
		}
		else {
			// try block is a hack around fact that currently tuplizers are not
			// given the opportunity to resolve a subclass entity name.  this
			// allows the (we assume custom) interceptor the ability to
			// influence this decision if we were not able to based on the
			// given entityName
			try {
				return getFactory().getRuntimeMetamodels()
						.getMappingMetamodel()
						.getEntityDescriptor( entityName )
						.getSubclassEntityPersister( object, getFactory() );
			}
			catch ( HibernateException e ) {
				try {
					return getEntityPersister( null, object );
				}
				catch ( HibernateException e2 ) {
					throw e;
				}
			}
		}
	}

	// not for internal use:
	@Override
	public Object getIdentifier(Object object) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.getSession() != this ) {
				throw new TransientObjectException( "The proxy was not associated with this session" );
			}
			return lazyInitializer.getInternalIdentifier();
		}
		else {
			final EntityEntry entry = persistenceContext.getEntry( object );
			if ( entry == null ) {
				throw new TransientObjectException( "The instance was not associated with this session" );
			}
			return entry.getId();
		}
	}

	/**
	 * Get the id value for an object that is actually associated with the session. This
	 * is a bit stricter than getEntityIdentifierIfNotUnsaved().
	 */
	@Override
	public Object getContextEntityIdentifier(Object object) {
		checkOpenOrWaitingForAutoClose();
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		else if ( isPersistentAttributeInterceptable( object ) ) {
			final PersistentAttributeInterceptor interceptor =
					asPersistentAttributeInterceptable( object ).$$_hibernate_getInterceptor();
			if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
				return ( (EnhancementAsProxyLazinessInterceptor) interceptor ).getIdentifier();
			}
		}

		final EntityEntry entry = persistenceContext.getEntry( object );
		return entry != null ? entry.getId() : null;
	}

	@Override
	public boolean contains(Object object) {
		checkOpen();
		pulseTransactionCoordinator();

		if ( object == null ) {
			return false;
		}

		try {
			final LazyInitializer lazyInitializer = extractLazyInitializer( object );
			if ( lazyInitializer != null ) {
				//do not use proxiesByKey, since not all
				//proxies that point to this session's
				//instances are in that collection!
				if ( lazyInitializer.isUninitialized() ) {
					//if it is an uninitialized proxy, pointing
					//with this session, then when it is accessed,
					//the underlying instance will be "contained"
					return lazyInitializer.getSession() == this;
				}
				else {
					//if it is initialized, see if the underlying
					//instance is contained, since we need to
					//account for the fact that it might have been
					//evicted
					object = lazyInitializer.getImplementation();
				}
			}

			// A session is considered to contain an entity only if the entity has
			// an entry in the session's persistence context and the entry reports
			// that the entity has not been removed
			EntityEntry entry = persistenceContext.getEntry( object );
			delayedAfterCompletion();

			if ( entry == null ) {
				if ( lazyInitializer == null && persistenceContext.getEntry( object ) == null ) {
					// check if it is even an entity -> if not throw an exception (per JPA)
					try {
						final String entityName = getEntityNameResolver().resolveEntityName( object );
						if ( entityName == null ) {
							throw new IllegalArgumentException( "Could not resolve entity name for class '" + object.getClass() + "'" );
						}
						getFactory().getRuntimeMetamodels()
								.getMappingMetamodel()
								.getEntityDescriptor( entityName );
					}
					catch ( HibernateException e ) {
						throw new IllegalArgumentException( "Class '" + object.getClass() + "' is not an entity class", e );
					}
				}
				return false;
			}
			else {
				return !entry.getStatus().isDeletedOrGone();
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public boolean contains(String entityName, Object object) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();

		if ( object == null ) {
			return false;
		}

		try {
			final LazyInitializer lazyInitializer = extractLazyInitializer( object );
			if ( lazyInitializer == null && persistenceContext.getEntry( object ) == null ) {
				// check if it is an entity -> if not throw an exception (per JPA)
				try {
					getFactory().getMappingMetamodel()
							.getEntityDescriptor( entityName );
				}
				catch (HibernateException e) {
					throw new IllegalArgumentException( "Not an entity [" + entityName + "] : " + object );
				}
			}

			if ( lazyInitializer != null ) {
				//do not use proxiesByKey, since not all
				//proxies that point to this session's
				//instances are in that collection!
				if ( lazyInitializer.isUninitialized() ) {
					//if it is an uninitialized proxy, pointing
					//with this session, then when it is accessed,
					//the underlying instance will be "contained"
					return lazyInitializer.getSession() == this;
				}
				else {
					//if it is initialized, see if the underlying
					//instance is contained, since we need to
					//account for the fact that it might have been
					//evicted
					object = lazyInitializer.getImplementation();
				}
			}
			// A session is considered to contain an entity only if the entity has
			// an entry in the session's persistence context and the entry reports
			// that the entity has not been removed
			final EntityEntry entry = persistenceContext.getEntry( object );
			delayedAfterCompletion();
			return entry != null && !entry.getStatus().isDeletedOrGone();
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		checkOpen();
//		checkTransactionSynchStatus();
		return super.createStoredProcedureCall( procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		checkOpen();
//		checkTransactionSynchStatus();
		return super.createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
		checkOpen();
//		checkTransactionSynchStatus();
		return super.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
//		checkTransactionSynchStatus();
		return getFactory();
	}

	@Override
	public void initializeCollection(PersistentCollection<?> collection, boolean writing) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		InitializeCollectionEvent event = new InitializeCollectionEvent( collection, this );
		fastSessionServices.eventListenerGroup_INIT_COLLECTION
				.fireEventOnEachListener( event, InitializeCollectionEventListener::onInitializeCollection );
		delayedAfterCompletion();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( lazyInitializer.isUninitialized() ) {
				return lazyInitializer.getEntityName();
			}
			object = lazyInitializer.getImplementation();
		}
		final EntityEntry entry = persistenceContext.getEntry( object );
		if ( entry == null ) {
			return guessEntityName( object );
		}
		else {
			return entry.getPersister().getEntityName();
		}
	}

	@Override
	public String bestGuessEntityName(Object object, EntityEntry entry) {
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( lazyInitializer.isUninitialized() ) {
				return lazyInitializer.getEntityName();
			}
			object = lazyInitializer.getImplementation();
		}
		if ( entry == null ) {
			return guessEntityName( object );
		}
		else {
			return entry.getPersister().getEntityName();
		}
	}

	@Override
	public String getEntityName(Object object) {
		checkOpen();
//		checkTransactionSynchStatus();
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			if ( !persistenceContext.containsProxy( object ) ) {
				throw new TransientObjectException( "proxy was not associated with the session" );
			}
			object = lazyInitializer.getImplementation();
		}

		EntityEntry entry = persistenceContext.getEntry( object );
		if ( entry == null ) {
			throwTransientObjectException( object );
		}
		return entry.getPersister().getEntityName();
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T getReference(T object) {
		checkOpen();
		final LazyInitializer lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			return (T) getReference( lazyInitializer.getPersistentClass(), lazyInitializer.getInternalIdentifier() );
		}
		else {
			EntityPersister persister = getEntityPersister( null, object );
			return (T) getReference( persister.getMappedClass(), persister.getIdentifier(object, this) );
		}
	}

	private void throwTransientObjectException(Object object) throws HibernateException {
		throw new TransientObjectException(
				"object references an unsaved transient instance - save the transient instance before flushing: " +
						guessEntityName( object )
		);
	}

	@Override
	public String guessEntityName(Object object) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		return getEntityNameResolver().resolveEntityName( object );
	}

	@Override
	public void cancelQuery() throws HibernateException {
		checkOpen();
		getJdbcCoordinator().cancelLastQuery();
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder( 500 )
				.append( "SessionImpl(" ).append( System.identityHashCode( this ) );
		if ( !isClosed() ) {
			if ( log.isTraceEnabled() ) {
				string.append( persistenceContext )
					.append( ";" )
					.append( actionQueue );
			}
			else {
				string.append( "<open>" );
			}
		}
		else {
			string.append( "<closed>" );
		}
		return string.append( ')' ).toString();
	}

	@Override
	public ActionQueue getActionQueue() {
		checkOpenOrWaitingForAutoClose();
//		checkTransactionSynchStatus();
		return actionQueue;
	}

	@Override
	public PersistenceContext getPersistenceContext() {
		checkOpenOrWaitingForAutoClose();
//		checkTransactionSynchStatus();
		return persistenceContext;
	}

	@Override
	public PersistenceContext getPersistenceContextInternal() {
		return persistenceContext;
	}

	@Override
	public SessionStatistics getStatistics() {
		pulseTransactionCoordinator();
		return new SessionStatisticsImpl( this );
	}

	@Override
	public boolean isEventSource() {
		pulseTransactionCoordinator();
		return true;
	}

	@Override
	public EventSource asEventSource() {
		return this;
	}

	@Override
	public boolean isDefaultReadOnly() {
		return persistenceContext.isDefaultReadOnly();
	}

	@Override
	public void setDefaultReadOnly(boolean defaultReadOnly) {
		persistenceContext.setDefaultReadOnly( defaultReadOnly );
	}

	@Override
	public boolean isReadOnly(Object entityOrProxy) {
		checkOpen();
//		checkTransactionSynchStatus();
		return persistenceContext.isReadOnly( entityOrProxy );
	}

	@Override
	public void setReadOnly(Object entity, boolean readOnly) {
		checkOpen();
//		checkTransactionSynchStatus();
		persistenceContext.setReadOnly( entity, readOnly );
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getCacheMode().getJpaStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheMode().getJpaRetrieveMode();
	}

	@Override
	public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		setCacheMode( fromJpaModes( getCacheMode().getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		setCacheMode( fromJpaModes( cacheRetrieveMode, getCacheMode().getJpaStoreMode() ) );
	}

	@Override
	public void afterScrollOperation() {
		// nothing to do in a stateful session
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return loadQueryInfluencers.isFetchProfileEnabled( name );
	}

	@Override
	public void enableFetchProfile(String name) throws UnknownProfileException {
		loadQueryInfluencers.enableFetchProfile( name );
	}

	@Override
	public void disableFetchProfile(String name) throws UnknownProfileException {
		loadQueryInfluencers.disableFetchProfile( name );
	}

	@Override
	public void setSubselectFetchingEnabled(boolean enabled) {
		loadQueryInfluencers.setSubselectFetchEnabled( enabled );
	}

	@Override
	public boolean isSubselectFetchingEnabled() {
		return loadQueryInfluencers.getSubselectFetchEnabled();
	}

	@Override
	public void setFetchBatchSize(int batchSize) {
		loadQueryInfluencers.setBatchSize( batchSize );
	}

	@Override
	public int getFetchBatchSize() {
		return loadQueryInfluencers.getBatchSize();
	}

	@Override
	public LobHelper getLobHelper() {
		if ( lobHelper == null ) {
			lobHelper = new LobHelperImpl();
		}
		return lobHelper;
	}

	private transient LobHelperImpl lobHelper;

	@Override
	public void beforeTransactionCompletion() {
		log.trace( "SessionImpl#beforeTransactionCompletion()" );
		flushBeforeTransactionCompletion();
		actionQueue.beforeTransactionCompletion();
		beforeTransactionCompletionEvents();
		super.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "SessionImpl#afterTransactionCompletion(successful=%s, delayed=%s)", successful, delayed );
		}

		if ( !isClosed() || waitingForAutoClose ) {
			if ( autoClear ||!successful ) {
				internalClear();
			}
		}

		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion( successful );

		afterTransactionCompletionEvents( successful );

		if ( !delayed ) {
			if ( shouldAutoClose() && (!isClosed() || waitingForAutoClose) ) {
				managedClose();
			}
		}

		super.afterTransactionCompletion( successful, delayed );
	}

	private static class LobHelperImpl implements LobHelper {

		@Override
		public Blob createBlob(byte[] bytes) {
			return lobCreator().createBlob( bytes );
		}

		private LobCreator lobCreator() {
			// Always use NonContextualLobCreator.  If ContextualLobCreator is
			// used both here and in WrapperOptions,
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public Blob createBlob(InputStream stream, long length) {
			return lobCreator().createBlob( stream, length );
		}

		@Override
		public Clob createClob(String string) {
			return lobCreator().createClob( string );
		}

		@Override
		public Clob createClob(Reader reader, long length) {
			return lobCreator().createClob( reader, length );
		}

		@Override
		public NClob createNClob(String string) {
			return lobCreator().createNClob( string );
		}

		@Override
		public NClob createNClob(Reader reader, long length) {
			return lobCreator().createNClob( reader, length );
		}
	}

	private static class SharedSessionBuilderImpl
			extends SessionFactoryImpl.SessionBuilderImpl
			implements SharedSessionBuilder, SharedSessionCreationOptions {
		private final SessionImpl session;
		private boolean shareTransactionContext;
		private boolean tenantIdChanged;

		private SharedSessionBuilderImpl(SessionImpl session) {
			super( (SessionFactoryImpl) session.getFactory() );
			this.session = session;
			super.tenantIdentifier( session.getTenantIdentifierValue() );
		}

		@Override
		public SessionImpl openSession() {
			if ( session.getSessionFactory().getSessionFactoryOptions().isMultiTenancyEnabled() ) {
				if ( tenantIdChanged && shareTransactionContext ) {
					throw new SessionException( "Cannot redefine the tenant identifier on a child session if the connection is reused" );
				}
			}
			return super.openSession();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SharedSessionBuilder


		@Override @Deprecated
		public SharedSessionBuilderImpl tenantIdentifier(String tenantIdentifier) {
			super.tenantIdentifier( tenantIdentifier );
			tenantIdChanged = true;
			return this;
		}

		@Override
		public SharedSessionBuilderImpl tenantIdentifier(Object tenantIdentifier) {
			super.tenantIdentifier( tenantIdentifier );
			tenantIdChanged = true;
			return this;
		}

		@Override
		public SharedSessionBuilderImpl interceptor() {
			super.interceptor( session.getInterceptor() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl interceptor(Interceptor interceptor) {
			super.interceptor( interceptor );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl noInterceptor() {
			super.noInterceptor();
			return this;
		}

		@Override
		public SharedSessionBuilderImpl connection() {
			this.shareTransactionContext = true;
			return this;
		}

		@Override
		public SharedSessionBuilderImpl connection(Connection connection) {
			super.connection( connection );
			return this;
		}

		@Override
		@Deprecated(since = "6.0")
		public SharedSessionBuilderImpl connectionReleaseMode() {
			final PhysicalConnectionHandlingMode handlingMode = PhysicalConnectionHandlingMode.interpret(
					ConnectionAcquisitionMode.AS_NEEDED,
					session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode()
			);
			connectionHandlingMode( handlingMode );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl connectionHandlingMode() {
			connectionHandlingMode( session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoJoinTransactions() {
			super.autoJoinTransactions( session.isAutoCloseSessionEnabled() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoJoinTransactions(boolean autoJoinTransactions) {
			super.autoJoinTransactions( autoJoinTransactions );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoClose(boolean autoClose) {
			super.autoClose( autoClose );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl flushMode() {
			flushMode( session.getHibernateFlushMode() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoClose() {
			autoClose( session.autoClose );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl jdbcTimeZone(TimeZone timeZone) {
			super.jdbcTimeZone(timeZone);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl clearEventListeners() {
			super.clearEventListeners();
			return this;
		}

		@Override
		public SharedSessionBuilderImpl flushMode(FlushMode flushMode) {
			super.flushMode(flushMode);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoClear(boolean autoClear) {
			super.autoClear(autoClear);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl statementInspector(StatementInspector statementInspector) {
			super.statementInspector(statementInspector);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
			super.connectionHandlingMode(connectionHandlingMode);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl eventListeners(SessionEventListener... listeners) {
			super.eventListeners(listeners);
			return this;
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SharedSessionCreationOptions

		@Override
		public boolean isTransactionCoordinatorShared() {
			return shareTransactionContext;
		}

		@Override
		public TransactionCoordinator getTransactionCoordinator() {
			return shareTransactionContext ? session.getTransactionCoordinator() : null;
		}

		@Override
		public JdbcCoordinator getJdbcCoordinator() {
			return shareTransactionContext ? session.getJdbcCoordinator() : null;
		}

		@Override
		public TransactionImplementor getTransaction() {
			return shareTransactionContext ? session.getCurrentTransaction() : null;
		}

		@Override
		public ActionQueue.TransactionCompletionProcesses getTransactionCompletionProcesses() {
			return shareTransactionContext ?
					session.getActionQueue().getTransactionCompletionProcesses() :
					null;
		}
	}

	@Deprecated
	private class LockRequestImpl implements LockRequest {
		private final LockOptions lockOptions;

		private LockRequestImpl(LockOptions lockOptions) {
			this.lockOptions = new LockOptions();
			LockOptions.copy( lockOptions, this.lockOptions );
		}

		@Override
		public LockMode getLockMode() {
			return lockOptions.getLockMode();
		}

		@Override
		public LockRequest setLockMode(LockMode lockMode) {
			lockOptions.setLockMode( lockMode );
			return this;
		}

		@Override
		public int getTimeOut() {
			return lockOptions.getTimeOut();
		}

		@Override
		public LockRequest setTimeOut(int timeout) {
			lockOptions.setTimeOut( timeout );
			return this;
		}

		@Override @Deprecated @SuppressWarnings("deprecation")
		public boolean getScope() {
			return lockOptions.getScope();
		}

		@Override @Deprecated @SuppressWarnings("deprecation")
		public LockRequest setScope(boolean scope) {
			lockOptions.setScope( scope );
			return this;
		}

		@Override @Deprecated @SuppressWarnings("deprecation")
		public void lock(String entityName, Object object) throws HibernateException {
			fireLock( entityName, object, lockOptions );
		}

		@Override
		public void lock(Object object) throws HibernateException {
			fireLock( object, lockOptions );
		}
	}

	@Override
	protected void addSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
		transactionObserver = new TransactionObserver() {
			@Override
			public void afterBegin() {
			}

			@Override
			public void beforeCompletion() {
				if ( isOpen() && getHibernateFlushMode() !=  FlushMode.MANUAL ) {
					managedFlush();
				}
				actionQueue.beforeTransactionCompletion();
				beforeTransactionCompletionEvents();
			}

			@Override
			public void afterCompletion(boolean successful, boolean delayed) {
				afterTransactionCompletion( successful, delayed );
				if ( !isClosed() && autoClose ) {
					managedClose();
				}
			}
		};
		transactionCoordinator.addObserver( transactionObserver );
	}

	@Override
	protected void removeSharedSessionTransactionObserver(TransactionCoordinator transactionCoordinator) {
		super.removeSharedSessionTransactionObserver( transactionCoordinator );
		transactionCoordinator.removeObserver( transactionObserver );
	}

	private EntityPersister requireEntityPersister(Class<?> entityClass) {
		return getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityClass );
	}

	private EntityPersister requireEntityPersister(String entityName) {
		return getFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
	}

	@Override
	public void startTransactionBoundary() {
		checkOpenOrWaitingForAutoClose();
		super.startTransactionBoundary();
	}

	@Override
	public void afterTransactionBegin() {
		checkOpenOrWaitingForAutoClose();
		afterTransactionBeginEvents();
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		if ( mustFlushBeforeCompletion() ) {
			try {
				managedFlush();
			}
			catch ( RuntimeException re ) {
				throw ExceptionMapperStandardImpl.INSTANCE
						.mapManagedFlushFailure( "error during managed flush", re, this );
			}
		}
	}

	private boolean mustFlushBeforeCompletion() {
		return isTransactionFlushable() && getHibernateFlushMode() != FlushMode.MANUAL;
	}

	private boolean isTransactionFlushable() {
		if ( getCurrentTransaction() == null ) {
			// assume it is flushable - CMT, auto-commit, etc
			return true;
		}
		else {
			final TransactionStatus status = getCurrentTransaction().getStatus();
			return status == TransactionStatus.ACTIVE || status == TransactionStatus.COMMITTING;
		}
	}

	@Override
	public SessionImplementor getSession() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HibernateEntityManagerImplementor impl


//	@Override
//	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties) {
//		LockOptions lockOptions = new LockOptions();
//		if ( this.lockOptions != null ) { //otherwise the default LockOptions constructor is the same as DEFAULT_LOCK_OPTIONS
//			LockOptions.copy( this.lockOptions, lockOptions );
//		}
//		lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
//		if ( properties != null ) {
//			LockOptionsHelper.applyPropertiesToLockOptions( properties, () -> lockOptions );
//		}
//		return lockOptions;
//	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityManager impl

	@Override
	public void remove(Object entity) {
		checkOpen();

		try {
			delete( entity );
		}
		catch (MappingException e) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey) {
		return find( entityClass, primaryKey, null, null );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return find( entityClass, primaryKey, null, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType) {
		return find( entityClass, primaryKey, lockModeType, null );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();

		LockOptions lockOptions = null;
		try {
			if ( lockModeType != null ) {
				checkTransactionNeededForLock( lockModeType );
				lockOptions = buildLockOptions( lockModeType, properties );
			}

			final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
			effectiveEntityGraph.applyConfiguredGraph( properties );
			loadQueryInfluencers.setReadOnly( getReadOnlyHint( properties ) );
			if ( effectiveEntityGraph.getSemantic() == GraphSemantic.FETCH ) {
				setEnforcingFetchGraph( true );
			}

			return byId( entityClass )
					.with( determineAppropriateLocalCacheMode( properties ) )
					.with( lockOptions )
					.load( primaryKey );
		}
		catch ( EntityNotFoundException enfe ) {
			/*
			This may happen if the entity has an associations mapped with @NotFound(action = NotFoundAction.EXCEPTION)
			and this associated entity is not found.
			 */
			if ( enfe instanceof FetchNotFoundException ) {
				throw enfe;
			}
			/*
			This may happen if the entity has an associations which is filtered by a FilterDef
			and this associated entity is not found.
			 */
			if ( enfe instanceof EntityFilterException ) {
				throw enfe;
			}
			// DefaultLoadEventListener#returnNarrowedProxy() may throw ENFE (see HHH-7861 for details),
			// which find() should not throw.  Find() should return null if the entity was not found.
			if ( log.isDebugEnabled() ) {
				String entityName = entityClass != null ? entityClass.getName(): null;
				String identifierValue = primaryKey != null ? primaryKey.toString() : null ;
				log.ignoringEntityNotFound( entityName, identifierValue );
			}
			return null;
		}
		catch ( ObjectDeletedException e ) {
			//the spec is silent about people doing remove() find() on the same PC
			return null;
		}
		catch ( ObjectNotFoundException e ) {
			//should not happen on the entity itself with get
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( MappingException | TypeMismatchException | ClassCastException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( JDBCException e ) {
			if ( accessTransaction().isActive() && accessTransaction().getRollbackOnly() ) {
				// Assume this is similar to the WildFly / IronJacamar "feature" described under HHH-12472.
				// Just log the exception and return null.
				if ( log.isDebugEnabled() ) {
					log.debug( "JDBCException was thrown for a transaction marked for rollback; " +
									"this is probably due to an operation failing fast due to the " +
									"transaction marked for rollback.", e );
				}
				return null;
			}
			else {
				throw getExceptionConverter().convert( e, lockOptions );
			}
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e, lockOptions );
		}
		finally {
			loadQueryInfluencers.getEffectiveEntityGraph().clear();
			loadQueryInfluencers.setReadOnly( null );
			setEnforcingFetchGraph( false );
		}
	}

	private void checkTransactionNeededForLock(LockModeType lockModeType) {
		if ( lockModeType != LockModeType.NONE ) {
			checkTransactionNeededForUpdateOperation();
		}
	}

	private static Boolean getReadOnlyHint(Map<String, Object> properties) {
		return properties == null ? null : (Boolean) properties.get( HINT_READ_ONLY );
	}

	protected CacheMode determineAppropriateLocalCacheMode(Map<String, Object> localProperties) {
		CacheRetrieveMode retrieveMode = null;
		CacheStoreMode storeMode = null;
		if ( localProperties != null ) {
			retrieveMode = determineCacheRetrieveMode( localProperties );
			storeMode = determineCacheStoreMode( localProperties );
		}
		if ( retrieveMode == null ) {
			// use the EM setting
			retrieveMode = fastSessionServices.getCacheRetrieveMode( this.properties );
		}
		if ( storeMode == null ) {
			// use the EM setting
			storeMode = fastSessionServices.getCacheStoreMode( this.properties );
		}
		return interpretCacheMode( storeMode, retrieveMode );
	}

	private static CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		final CacheRetrieveMode cacheRetrieveMode = (CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		return cacheRetrieveMode == null
				? (CacheRetrieveMode) settings.get( JAKARTA_SHARED_CACHE_RETRIEVE_MODE )
				: cacheRetrieveMode;
	}

	private static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		final CacheStoreMode cacheStoreMode = (CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		return cacheStoreMode == null
				? (CacheStoreMode) settings.get( JAKARTA_SHARED_CACHE_STORE_MODE )
				: cacheStoreMode;
	}

	private void checkTransactionNeededForUpdateOperation() {
		checkTransactionNeededForUpdateOperation( "no transaction is in progress" );
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object id) {
		checkOpen();

		try {
			return byId( entityClass ).getReference( id );
		}
		catch ( MappingException | TypeMismatchException | ClassCastException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public Object getReference(String entityName, Object id) {
		checkOpen();

		try {
			return byId( entityName ).getReference( id );
		}
		catch ( MappingException | TypeMismatchException | ClassCastException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType) {
		lock( entity, lockModeType, null );
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		checkTransactionNeededForUpdateOperation();

		if ( !contains( entity ) ) {
			throw new IllegalArgumentException( "entity not in the persistence context" );
		}

		final LockOptions lockOptions = buildLockOptions( lockModeType, properties );
		try {
			buildLockRequest( lockOptions ).lock( entity );
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e, lockOptions );
		}
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		refresh( entity, null, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType) {
		refresh( entity, lockModeType, null );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();

		final CacheMode previousCacheMode = getCacheMode();
		final CacheMode refreshCacheMode = determineAppropriateLocalCacheMode( properties );

		LockOptions lockOptions = null;
		try {
			setCacheMode( refreshCacheMode );

			if ( !contains( entity ) ) {
				throw getExceptionConverter().convert( new IllegalArgumentException( "Entity not managed" ) );
			}

			if ( lockModeType != null ) {
				checkTransactionNeededForLock( lockModeType );
				lockOptions = buildLockOptions( lockModeType, properties );
				refresh( entity, lockOptions );
			}
			else {
				refresh( entity );
			}
		}
		catch ( MappingException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e, lockOptions );
		}
		finally {
			setCacheMode( previousCacheMode );
		}
	}

	private LockOptions buildLockOptions(LockModeType lockModeType, Map<String, Object> properties) {
		LockOptions lockOptions = new LockOptions();
		if ( this.lockOptions != null ) { //otherwise the default LockOptions constructor is the same as DEFAULT_LOCK_OPTIONS
			LockOptions.copy( this.lockOptions, lockOptions );
		}
		lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		if ( properties != null ) {
			applyPropertiesToLockOptions( properties, () -> lockOptions );
		}
		return lockOptions;
	}

	@Override
	public void detach(Object entity) {
		checkOpen();
		try {
			evict( entity );
		}
		catch (RuntimeException e) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		checkOpen();

		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "Call to EntityManager#getLockMode should occur within transaction according to spec" );
		}

		if ( !contains( entity ) ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( "entity not in the persistence context" ) );
		}

		return LockModeTypeHelper.getLockModeType( getCurrentLockMode( entity ) );

	}

	@Override
	public void setProperty(String propertyName, Object value) {
		checkOpen();

		if ( !( value instanceof Serializable ) ) {
			log.warnf( "Property '%s' is not serializable, value won't be set", propertyName );
			return;
		}

		if ( propertyName == null ) {
			log.warn( "Property having key null is illegal, value won't be set" );
			return;
		}

		// store property for future reference:
		if ( properties == null ) {
			properties = computeCurrentSessionProperties();
		}
		properties.put( propertyName, value );

		// now actually update the setting, if it's one which affects this Session
		interpretProperty( propertyName, value );
	}

	private void interpretProperty(String propertyName, Object value) {
		switch ( propertyName ) {
			case HINT_FLUSH_MODE:
				setHibernateFlushMode( ConfigurationHelper.getFlushMode( value, FlushMode.AUTO ) );
				break;
			case JPA_LOCK_SCOPE:
			case JAKARTA_LOCK_SCOPE:
				properties.put( JPA_LOCK_SCOPE, value);
				properties.put( JAKARTA_LOCK_SCOPE, value);
				applyPropertiesToLockOptions( properties, this::getLockOptionsForWrite );
				break;
			case JPA_LOCK_TIMEOUT:
			case JAKARTA_LOCK_TIMEOUT:
				properties.put( JPA_LOCK_TIMEOUT, value );
				properties.put( JAKARTA_LOCK_TIMEOUT, value );
				applyPropertiesToLockOptions( properties, this::getLockOptionsForWrite );
				break;
			case JPA_SHARED_CACHE_RETRIEVE_MODE:
			case JAKARTA_SHARED_CACHE_RETRIEVE_MODE:
				properties.put( JPA_SHARED_CACHE_RETRIEVE_MODE, value );
				properties.put( JAKARTA_SHARED_CACHE_RETRIEVE_MODE, value );
				setCacheMode(
						interpretCacheMode(
								determineCacheStoreMode( properties ),
								(CacheRetrieveMode) value
						)
				);
				break;
			case JPA_SHARED_CACHE_STORE_MODE:
			case JAKARTA_SHARED_CACHE_STORE_MODE:
				properties.put( JPA_SHARED_CACHE_STORE_MODE, value );
				properties.put( JAKARTA_SHARED_CACHE_STORE_MODE, value );
				setCacheMode(
						interpretCacheMode(
								(CacheStoreMode) value,
								determineCacheRetrieveMode( properties )
						)
				);
				break;
			case CRITERIA_COPY_TREE:
				setCriteriaCopyTreeEnabled( parseBoolean( value.toString() ) );
				break;
			case HINT_FETCH_PROFILE:
				enableFetchProfile( (String) value );
				break;
			case USE_SUBSELECT_FETCH:
			case HINT_ENABLE_SUBSELECT_FETCH:
				setSubselectFetchingEnabled( Boolean.parseBoolean( value.toString() ) );
				break;
			case DEFAULT_BATCH_FETCH_SIZE:
			case HINT_BATCH_FETCH_SIZE:
				setFetchBatchSize( Integer.parseInt( value.toString() ) );
				break;
			case STATEMENT_BATCH_SIZE:
			case HINT_JDBC_BATCH_SIZE:
				setJdbcBatchSize( Integer.parseInt( value.toString() ) );
				break;
		}
	}

	private Map<String, Object> computeCurrentSessionProperties() {
		final Map<String, Object> map = new HashMap<>( fastSessionServices.defaultSessionProperties );
		//The FLUSH_MODE is always set at Session creation time,
		//so it needs special treatment to not eagerly initialize this Map:
		map.put( HINT_FLUSH_MODE, getHibernateFlushMode().name() );
		return map;
	}

	@Override
	public Map<String, Object> getProperties() {
		if ( properties == null ) {
			properties = computeCurrentSessionProperties();
		}
		return unmodifiableMap( properties );
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		checkOpen();
		try {
			final NamedCallableQueryMemento memento = getFactory().getQueryEngine()
					.getNamedObjectRepository()
					.getCallableQueryMemento( name );
			if ( memento == null ) {
				throw new IllegalArgumentException( "No @NamedStoredProcedureQuery was found with that name : " + name );
			}
			return memento.makeProcedureCall( this );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName) {
		try {
			return createStoredProcedureCall( procedureName );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		try {
			return createStoredProcedureCall( procedureName, resultClasses );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		try {
			try {
				return createStoredProcedureCall( procedureName, resultSetMappings );
			}
			catch ( UnknownSqlResultSetMappingException e ) {
				throw new IllegalArgumentException( e.getMessage(), e );
			}
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		checkOpen();

		if ( Session.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SessionImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SharedSessionContractImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( EntityManager.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( PersistenceContext.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}

		throw new PersistenceException( "Hibernate cannot unwrap " + clazz );
	}

	@Override
	public Object getDelegate() {
		checkOpen();
		return this;
	}

	@Override
	public SessionFactoryImplementor getEntityManagerFactory() {
		checkOpen();
		return getFactory();
	}

	@Override
	public Metamodel getMetamodel() {
		checkOpen();
		return getFactory().getJpaMetamodel();
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param oos The output stream to which we are being written...
	 *
	 * @throws IOException Indicates a general IO stream exception
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Serializing Session [%s]", getSessionIdentifier() );
		}

		oos.defaultWriteObject();

		persistenceContext.serialize( oos );
		actionQueue.serialize( oos );

		oos.writeObject( loadQueryInfluencers );
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param ois The input stream from which we are being read...
	 *
	 * @throws IOException Indicates a general IO stream exception
	 * @throws ClassNotFoundException Indicates a class resolution issue
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException, SQLException {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Deserializing Session [%s]", getSessionIdentifier() );
		}

		ois.defaultReadObject();

		persistenceContext = StatefulPersistenceContext.deserialize( ois, this );
		actionQueue = ActionQueue.deserialize( ois, this );

		loadQueryInfluencers = (LoadQueryInfluencers) ois.readObject();

		// LoadQueryInfluencers#getEnabledFilters() tries to validate each enabled
		// filter, which will fail when called before FilterImpl#afterDeserialize( factory );
		// Instead lookup the filter by name and then call FilterImpl#afterDeserialize( factory ).
		for ( String filterName : loadQueryInfluencers.getEnabledFilterNames() ) {
			( (FilterImpl) loadQueryInfluencers.getEnabledFilter( filterName ) ).afterDeserialize( getFactory() );
		}
	}

	private Boolean getReadOnlyFromLoadQueryInfluencers() {
		return loadQueryInfluencers != null ? loadQueryInfluencers.getReadOnly() : null;
	}

	@Override @Deprecated(forRemoval = true)
	public boolean isEnforcingFetchGraph() {
		return isEnforcingFetchGraph;
	}

	@Override @Deprecated(forRemoval = true)
	public void setEnforcingFetchGraph(boolean isEnforcingFetchGraph) {
		this.isEnforcingFetchGraph = isEnforcingFetchGraph;
	}

}
