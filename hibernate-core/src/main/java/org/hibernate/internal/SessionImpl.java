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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockScope;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TransactionRequiredException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Selection;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionException;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeHelper;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnknownProfileException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.query.spi.FilterQueryPlan;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.ClearEvent;
import org.hibernate.event.spi.ClearEventListener;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.hibernate.event.spi.DirtyCheckEvent;
import org.hibernate.event.spi.DirtyCheckEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
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
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.event.spi.ResolveNaturalIdEvent;
import org.hibernate.event.spi.ResolveNaturalIdEventListener;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.internal.CriteriaImpl.CriterionEntry;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.jpa.spi.CriteriaQueryTupleTransformer;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.internal.compile.CompilableCriteria;
import org.hibernate.query.criteria.internal.compile.CriteriaCompiler;
import org.hibernate.query.criteria.internal.expression.CompoundSelectionImpl;
import org.hibernate.query.internal.CollectionFilterImpl;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.resource.transaction.TransactionRequiredForJoinException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.AfterCompletionAction;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ManagedFlushChecker;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.internal.SessionStatisticsImpl;

import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_SCOPE;
import static org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;

/**
 * Concrete implementation of a Session.
 * <p/>
 * Exposes two interfaces:<ul>
 * <li>{@link org.hibernate.Session} to the application</li>
 * <li>{@link org.hibernate.engine.spi.SessionImplementor} to other Hibernate components (SPI)</li>
 * </ul>
 * <p/>
 * This class is not thread-safe.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Chris Cranford
 */
public final class SessionImpl
		extends AbstractSessionImpl
		implements EventSource, SessionImplementor, HibernateEntityManagerImplementor {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( SessionImpl.class );
	private static final boolean TRACE_ENABLED = log.isTraceEnabled();


	private static final List<String> ENTITY_MANAGER_SPECIFIC_PROPERTIES = new ArrayList<String>();

	static {
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( JPA_LOCK_SCOPE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( JPA_LOCK_TIMEOUT );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.FLUSH_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( JPA_SHARED_CACHE_RETRIEVE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( JPA_SHARED_CACHE_STORE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( QueryHints.SPEC_HINT_TIMEOUT );
	}

	private transient SessionOwner sessionOwner;

	private Map<String, Object> properties = new HashMap<>();

	private transient ActionQueue actionQueue;
	private transient StatefulPersistenceContext persistenceContext;

	private transient LoadQueryInfluencers loadQueryInfluencers;

	// todo : (5.2) HEM always initialized this.  Is that really needed?
	private LockOptions lockOptions = new LockOptions();

	private transient boolean autoClear;
	private transient boolean autoClose;

	private transient int dontFlushFromFind;
	private transient boolean disallowOutOfTransactionUpdateOperations;

	private transient ExceptionMapper exceptionMapper;
	private transient ManagedFlushChecker managedFlushChecker;
	private transient AfterCompletionAction afterCompletionAction;

	private transient LoadEvent loadEvent; //cached LoadEvent instance

	private transient boolean discardOnClose;

	public SessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );

		this.actionQueue = new ActionQueue( this );
		this.persistenceContext = new StatefulPersistenceContext( this );

		this.sessionOwner = options.getSessionOwner();
		initializeFromSessionOwner( sessionOwner );

		this.autoClear = options.shouldAutoClear();
		this.autoClose = options.shouldAutoClose();
		this.disallowOutOfTransactionUpdateOperations = !factory.getSessionFactoryOptions().isAllowOutOfTransactionUpdateOperations();
		this.discardOnClose = getFactory().getSessionFactoryOptions().isReleaseResourcesOnCloseEnabled();

		if ( options instanceof SharedSessionCreationOptions && ( (SharedSessionCreationOptions) options ).isTransactionCoordinatorShared() ) {
			final SharedSessionCreationOptions sharedOptions = (SharedSessionCreationOptions) options;
			if ( sharedOptions.getTransactionCompletionProcesses() != null ) {
				actionQueue.setTransactionCompletionProcesses( sharedOptions.getTransactionCompletionProcesses(), true );
			}
		}

		loadQueryInfluencers = new LoadQueryInfluencers( factory );

		if ( getFactory().getStatistics().isStatisticsEnabled() ) {
			getFactory().getStatistics().openSession();
		}

		// NOTE : pulse() already handles auto-join-ability correctly
		getTransactionCoordinator().pulse();

		setDefaultProperties();
		applyProperties();

		if ( TRACE_ENABLED ) {
			log.tracef( "Opened Session [%s] at timestamp: %s", getSessionIdentifier(), getTimestamp() );
		}
	}

	private void setDefaultProperties() {
		properties.putIfAbsent( AvailableSettings.FLUSH_MODE, getHibernateFlushMode().name() );
		properties.putIfAbsent( JPA_LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		properties.putIfAbsent( JPA_LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		properties.putIfAbsent( JPA_SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		properties.putIfAbsent( JPA_SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );
	}


	private void applyProperties() {
		applyEntityManagerSpecificProperties();
		setHibernateFlushMode( ConfigurationHelper.getFlushMode( properties.get( AvailableSettings.FLUSH_MODE ), FlushMode.AUTO ) );
		setLockOptions( this.properties, this.lockOptions );
		getSession().setCacheMode(
				CacheModeHelper.interpretCacheMode(
						currentCacheStoreMode(),
						currentCacheRetrieveMode()
				)
		);
	}

	private void applyEntityManagerSpecificProperties() {
		for ( String key : ENTITY_MANAGER_SPECIFIC_PROPERTIES ) {
			final Map<String, Object> properties = getFactory().getProperties();
			if ( getFactory().getProperties().containsKey( key ) ) {
				this.properties.put( key, getFactory().getProperties().get( key ) );
			}
		}
	}

	protected void applyQuerySettingsAndHints(Query query) {
		if ( lockOptions.getLockMode() != LockMode.NONE ) {
			query.setLockMode( getLockMode( lockOptions.getLockMode() ) );
		}
		Object queryTimeout;
		if ( (queryTimeout = getProperties().get( QueryHints.SPEC_HINT_TIMEOUT ) ) != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, queryTimeout );
		}
		Object lockTimeout;
		if( (lockTimeout = getProperties().get( JPA_LOCK_TIMEOUT ))!=null){
			query.setHint( JPA_LOCK_TIMEOUT, lockTimeout );
		}
	}

	private CacheRetrieveMode currentCacheRetrieveMode() {
		return determineCacheRetrieveMode( properties );
	}

	private CacheStoreMode currentCacheStoreMode() {
		return determineCacheStoreMode( properties );
	}


	private void initializeFromSessionOwner(SessionOwner sessionOwner) {
		if ( sessionOwner != null ) {
			if ( sessionOwner.getExceptionMapper() != null ) {
				exceptionMapper = sessionOwner.getExceptionMapper();
			}
			else {
				exceptionMapper = ExceptionMapperStandardImpl.INSTANCE;
			}
			if ( sessionOwner.getAfterCompletionAction() != null ) {
				afterCompletionAction = sessionOwner.getAfterCompletionAction();
			}
			else {
				afterCompletionAction = STANDARD_AFTER_COMPLETION_ACTION;
			}
			if ( sessionOwner.getManagedFlushChecker() != null ) {
				managedFlushChecker = sessionOwner.getManagedFlushChecker();
			}
			else {
				managedFlushChecker = STANDARD_MANAGED_FLUSH_CHECKER;
			}
		}
		else {
			exceptionMapper = ExceptionMapperStandardImpl.INSTANCE;
			afterCompletionAction = STANDARD_AFTER_COMPLETION_ACTION;
			managedFlushChecker = STANDARD_MANAGED_FLUSH_CHECKER;
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
			throw exceptionConverter.convert( e );
		}
	}

	private void internalClear() {
		persistenceContext.clear();
		actionQueue.clear();

		final ClearEvent event = new ClearEvent( this );
		for ( ClearEventListener listener : listeners( EventType.CLEAR ) ) {
			listener.onClear( event );
		}
	}



	@Override
	public void close() throws HibernateException {
		log.tracef( "Closing session [%s]", getSessionIdentifier() );

		// todo : we want this check if usage is JPA, but not native Hibernate usage
		if ( getSessionFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
			// Original hibernate-entitymanager EM#close behavior
			checkSessionFactoryOpen();
			checkOpenOrWaitingForAutoClose();
			if ( discardOnClose || !isTransactionInProgress( false ) ) {
				super.close();
			}
			else {
				//Otherwise, session auto-close will be enabled by shouldAutoCloseSession().
				waitingForAutoClose = true;
				closed = true;
			}
		}
		else {

			super.close();

			if ( getFactory().getStatistics().isStatisticsEnabled() ) {
				getFactory().getStatistics().closeSession();
			}
		}
	}

	private boolean isTransactionInProgress(boolean isMarkedRollbackConsideredActive) {
		if ( waitingForAutoClose ) {
			return getSessionFactory().isOpen() &&
					getTransactionCoordinator().isTransactionActive( isMarkedRollbackConsideredActive );
		}
		return !isClosed() &&
				getTransactionCoordinator().isTransactionActive( isMarkedRollbackConsideredActive );
	}

	@Override
	protected boolean shouldCloseJdbcCoordinatorOnClose(boolean isTransactionCoordinatorShared) {
		if ( !isTransactionCoordinatorShared ) {
			return super.shouldCloseJdbcCoordinatorOnClose( isTransactionCoordinatorShared );
		}

		if ( getActionQueue().hasBeforeTransactionActions() || getActionQueue().hasAfterTransactionActions() ) {
			log.warn(
					"On close, shared Session had beforeQuery/afterQuery transaction actions that have not yet been processed"
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
			throw exceptionConverter.convert( he );
		}
	}

	protected void checkSessionFactoryOpen() {
		if ( !getFactory().isOpen() ) {
			log.debug( "Forcing Session/EntityManager closed as SessionFactory/EntityManagerFactory has been closed" );
			setClosed();
		}
	}

	private boolean isFlushModeNever() {
		return FlushMode.isManualFlushMode( getHibernateFlushMode() );
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
		else if ( sessionOwner != null ) {
			return sessionOwner.shouldAutoCloseSession();
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
		close();
	}

	@Override
	public Connection connection() throws HibernateException {
		checkOpen();
		return getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
	}

	@Override
	public Connection disconnect() throws HibernateException {
		checkOpen();
		log.debug( "Disconnecting session" );
		return getJdbcCoordinator().getLogicalConnection().manualDisconnect();
	}

	@Override
	public void reconnect(Connection conn) throws HibernateException {
		checkOpen();
		log.debug( "Reconnecting session" );
		checkTransactionSynchStatus();
		getJdbcCoordinator().getLogicalConnection().manualReconnect( conn );
	}

	@Override
	public void setAutoClear(boolean enabled) {
		checkOpen();
		autoClear = enabled;
	}

	/**
	 * Check if there is a Hibernate or JTA transaction in progress and,
	 * if there is not, flush if necessary, make sure the connection has
	 * been committed (if it is not in autocommit mode) and run the afterQuery
	 * completion processing
	 *
	 * @param success Was the operation a success
	 */
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
		if ( object instanceof HibernateProxy ) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation( this );
			if ( object == null ) {
				return LockMode.NONE;
			}
		}
		EntityEntry e = persistenceContext.getEntry( object );
		if ( e == null ) {
			throw new TransientObjectException( "Given object not associated with the session" );
		}
		if ( e.getStatus() != Status.MANAGED ) {
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

	private void checkNoUnresolvedActionsBeforeOperation() {
		if ( persistenceContext.getCascadeLevel() == 0 && actionQueue.hasUnresolvedEntityInsertActions() ) {
			throw new IllegalStateException( "There are delayed insert actions beforeQuery operation as cascade level 0." );
		}
	}

	private void checkNoUnresolvedActionsAfterOperation() {
		if ( persistenceContext.getCascadeLevel() == 0 ) {
			actionQueue.checkNoUnresolvedActionsAfterOperation();
		}
		delayedAfterCompletion();
	}

	@Override
	protected void delayedAfterCompletion() {
		if ( getTransactionCoordinator() instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) getTransactionCoordinator() ).getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}

	// saveOrUpdate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void saveOrUpdate(Object object) throws HibernateException {
		saveOrUpdate( null, object );
	}

	@Override
	public void saveOrUpdate(String entityName, Object obj) throws HibernateException {
		fireSaveOrUpdate( new SaveOrUpdateEvent( entityName, obj, this ) );
	}

	private void fireSaveOrUpdate(SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.SAVE_UPDATE ) ) {
			listener.onSaveOrUpdate( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}

	private <T> Iterable<T> listeners(EventType<T> type) {
		return eventListenerGroup( type ).listeners();
	}

	private <T> EventListenerGroup<T> eventListenerGroup(EventType<T> type) {
		return getFactory().getServiceRegistry().getService( EventListenerRegistry.class ).getEventListenerGroup( type );
	}


	// save() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Serializable save(Object obj) throws HibernateException {
		return save( null, obj );
	}

	@Override
	public Serializable save(String entityName, Object object) throws HibernateException {
		return fireSave( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private Serializable fireSave(SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.SAVE ) ) {
			listener.onSaveOrUpdate( event );
		}
		checkNoUnresolvedActionsAfterOperation();
		return event.getResultId();
	}


	// update() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void update(Object obj) throws HibernateException {
		update( null, obj );
	}

	@Override
	public void update(String entityName, Object object) throws HibernateException {
		fireUpdate( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private void fireUpdate(SaveOrUpdateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.UPDATE ) ) {
			listener.onSaveOrUpdate( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}


	// lock() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		fireLock( new LockEvent( entityName, object, lockMode, this ) );
	}

	@Override
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
		checkTransactionSynchStatus();
		for ( LockEventListener listener : listeners( EventType.LOCK ) ) {
			listener.onLock( event );
		}
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
	public void persist(String entityName, Object object, Map copiedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		firePersist( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersist(PersistEvent event) {
		try {
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();

			for ( PersistEventListener listener : listeners( EventType.PERSIST ) ) {
				listener.onPersist( event );
			}
		}
		catch (MappingException e) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage() ) );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
		}
		finally {
			try {
				checkNoUnresolvedActionsAfterOperation();
			}
			catch (RuntimeException e) {
				throw exceptionConverter.convert( e );
			}
		}
	}

	private void firePersist(Map copiedAlready, PersistEvent event) {
		checkTransactionSynchStatus();

		try {
			for ( PersistEventListener listener : listeners( EventType.PERSIST ) ) {
				listener.onPersist( event, copiedAlready );
			}
		}
		catch ( MappingException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage() ) ) ;
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// persistOnFlush() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void persistOnFlush(String entityName, Object object)
			throws HibernateException {
		firePersistOnFlush( new PersistEvent( entityName, object, this ) );
	}

	public void persistOnFlush(Object object) throws HibernateException {
		persist( null, object );
	}

	@Override
	public void persistOnFlush(String entityName, Object object, Map copiedAlready)
			throws HibernateException {
		firePersistOnFlush( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersistOnFlush(Map copiedAlready, PersistEvent event) {
		checkOpenOrWaitingForAutoClose();
		checkTransactionSynchStatus();
		for ( PersistEventListener listener : listeners( EventType.PERSIST_ONFLUSH ) ) {
			listener.onPersist( event, copiedAlready );
		}
		delayedAfterCompletion();
	}

	private void firePersistOnFlush(PersistEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( PersistEventListener listener : listeners( EventType.PERSIST_ONFLUSH ) ) {
			listener.onPersist( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}


	// merge() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Object merge(String entityName, Object object) throws HibernateException {
		checkOpen();
		return fireMerge( new MergeEvent( entityName, object, this ) );
	}

	@Override
	public Object merge(Object object) throws HibernateException {
		checkOpen();
		return fireMerge( new MergeEvent( null, object, this ));
	}

	@Override
	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		fireMerge( copiedAlready, new MergeEvent( entityName, object, this ) );
	}

	private Object fireMerge(MergeEvent event) {
		try {
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();
			for ( MergeEventListener listener : listeners( EventType.MERGE ) ) {
				listener.onMerge( event );
			}
			checkNoUnresolvedActionsAfterOperation();
		}
		catch ( ObjectDeletedException sse ) {
			throw exceptionConverter.convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw exceptionConverter.convert( e );
		}

		return event.getResult();
	}

	private void fireMerge(Map copiedAlready, MergeEvent event) {
		try {
			checkTransactionSynchStatus();
			for ( MergeEventListener listener : listeners( EventType.MERGE ) ) {
				listener.onMerge( event, copiedAlready );
			}
		}
		catch ( ObjectDeletedException sse ) {
			throw exceptionConverter.convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw exceptionConverter.convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// delete() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void delete(Object object) throws HibernateException {
		checkOpen();
		fireDelete( new DeleteEvent( object, this ) );
	}

	@Override
	public void delete(String entityName, Object object) throws HibernateException {
		checkOpen();
		fireDelete( new DeleteEvent( entityName, object, this ) );
	}

	@Override
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, Set transientEntities)
			throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		if ( TRACE_ENABLED && persistenceContext.isRemovingOrphanBeforeUpates() ) {
			logRemoveOrphanBeforeUpdates( "beforeQuery continuing", entityName, object );
		}
		fireDelete(
				new DeleteEvent(
						entityName,
						object,
						isCascadeDeleteEnabled,
						persistenceContext.isRemovingOrphanBeforeUpates(),
						this
				),
				transientEntities
		);
		if ( TRACE_ENABLED && persistenceContext.isRemovingOrphanBeforeUpates() ) {
			logRemoveOrphanBeforeUpdates( "afterQuery continuing", entityName, object );
		}
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.  This should be removed once action/task
		// ordering is improved.
		if ( TRACE_ENABLED ) {
			logRemoveOrphanBeforeUpdates( "begin", entityName, child );
		}
		persistenceContext.beginRemoveOrphanBeforeUpdates();
		try {
			checkOpenOrWaitingForAutoClose();
			fireDelete( new DeleteEvent( entityName, child, false, true, this ) );
		}
		finally {
			persistenceContext.endRemoveOrphanBeforeUpdates();
			if ( TRACE_ENABLED ) {
				logRemoveOrphanBeforeUpdates( "end", entityName, child );
			}
		}
	}

	private void logRemoveOrphanBeforeUpdates(String timing, String entityName, Object entity) {
		final EntityEntry entityEntry = persistenceContext.getEntry( entity );
		log.tracef(
				"%s remove orphan beforeQuery updates: [%s]",
				timing,
				entityEntry == null ? entityName : MessageHelper.infoString( entityName, entityEntry.getId() )
		);
	}

	private void fireDelete(DeleteEvent event) {
		try{
		checkTransactionSynchStatus();
		for ( DeleteEventListener listener : listeners( EventType.DELETE ) ) {
			listener.onDelete( event );
		}
		}
		catch ( ObjectDeletedException sse ) {
			throw exceptionConverter.convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw exceptionConverter.convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void fireDelete(DeleteEvent event, Set transientEntities) {
		try{
		checkTransactionSynchStatus();
		for ( DeleteEventListener listener : listeners( EventType.DELETE ) ) {
			listener.onDelete( event, transientEntities );
		}
		}
		catch ( ObjectDeletedException sse ) {
			throw exceptionConverter.convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw exceptionConverter.convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}


	// load()/get() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void load(Object object, Serializable id) throws HibernateException {
		LoadEvent event = loadEvent;
		loadEvent = null;
		if ( event == null ) {
			event = new LoadEvent( id, object, this );
		}
		else {
			event.setEntityClassName( null );
			event.setEntityId( id );
			event.setInstanceToLoad( object );
			event.setLockMode( LoadEvent.DEFAULT_LOCK_MODE );
			event.setLockScope( LoadEvent.DEFAULT_LOCK_OPTIONS.getScope() );
			event.setLockTimeout( LoadEvent.DEFAULT_LOCK_OPTIONS.getTimeOut() );
		}

		fireLoad( event, LoadEventListener.RELOAD );

		if ( loadEvent == null ) {
			event.setEntityClassName( null );
			event.setEntityId( null );
			event.setInstanceToLoad( null );
			event.setResult( null );
			loadEvent = event;
		}
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id) throws HibernateException {
		return this.byId( entityClass ).getReference( id );
	}

	@Override
	public Object load(String entityName, Serializable id) throws HibernateException {
		return this.byId( entityName ).getReference( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Serializable id) throws HibernateException {
		return this.byId( entityClass ).load( id );
	}

	@Override
	public Object get(String entityName, Serializable id) throws HibernateException {
		return this.byId( entityName ).load( id );
	}

	/**
	 * Load the data for the object with the specified id into a newly created object.
	 * This is only called when lazily initializing a proxy.
	 * Do NOT return a proxy.
	 */
	@Override
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			EntityPersister persister = getFactory().getMetamodel().entityPersister( entityName );
			log.debugf( "Initializing proxy: %s", MessageHelper.infoString( persister, id, getFactory() ) );
		}
		LoadEvent event = loadEvent;
		loadEvent = null;
		event = recycleEventInstance( event, id, entityName );
		fireLoad( event, LoadEventListener.IMMEDIATE_LOAD );
		Object result = event.getResult();
		if ( loadEvent == null ) {
			event.setEntityClassName( null );
			event.setEntityId( null );
			event.setInstanceToLoad( null );
			event.setResult( null );
			loadEvent = event;
		}
		return result;
	}

	@Override
	public final Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable)
			throws HibernateException {
		// todo : remove
		LoadEventListener.LoadType type = nullable
				? LoadEventListener.INTERNAL_LOAD_NULLABLE
				: eager
				? LoadEventListener.INTERNAL_LOAD_EAGER
				: LoadEventListener.INTERNAL_LOAD_LAZY;

		LoadEvent event = loadEvent;
		loadEvent = null;
		event = recycleEventInstance( event, id, entityName );
		fireLoad( event, type );
		Object result = event.getResult();
		if ( !nullable ) {
			UnresolvableObjectException.throwIfNull( result, id, entityName );
		}
		if ( loadEvent == null ) {
			event.setEntityClassName( null );
			event.setEntityId( null );
			event.setInstanceToLoad( null );
			event.setResult( null );
			loadEvent = event;
		}
		return result;
	}

	/**
	 * Helper to avoid creating many new instances of LoadEvent: it's an allocation hot spot.
	 */
	private LoadEvent recycleEventInstance(final LoadEvent event, final Serializable id, final String entityName) {
		if ( event == null ) {
			return new LoadEvent( id, entityName, true, this );
		}
		else {
			event.setEntityClassName( entityName );
			event.setEntityId( id );
			event.setInstanceToLoad( null );
			event.setLockMode( LoadEvent.DEFAULT_LOCK_MODE );
			event.setLockScope( LoadEvent.DEFAULT_LOCK_OPTIONS.getScope() );
			event.setLockTimeout( LoadEvent.DEFAULT_LOCK_OPTIONS.getTimeOut() );
			return event;
		}
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).getReference( id );
	}

	@Override
	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	@Override
	public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).getReference( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override
	public <T> T get(Class<T> entityClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).load( id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override
	public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).load( id );
	}

	@Override
	public IdentifierLoadAccessImpl byId(String entityName) {
		return new IdentifierLoadAccessImpl( entityName );
	}

	@Override
	public <T> IdentifierLoadAccessImpl<T> byId(Class<T> entityClass) {
		return new IdentifierLoadAccessImpl<T>( entityClass );
	}

	@Override
	public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
		return new MultiIdentifierLoadAccessImpl<T>( locateEntityPersister( entityClass ) );
	}

	@Override
	public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
		return new MultiIdentifierLoadAccessImpl( locateEntityPersister( entityName ) );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return new NaturalIdLoadAccessImpl( entityName );
	}

	@Override
	public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
		return new NaturalIdLoadAccessImpl<T>( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return new SimpleNaturalIdLoadAccessImpl( entityName );
	}

	@Override
	public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
		return new SimpleNaturalIdLoadAccessImpl<T>( entityClass );
	}

	private void fireLoad(LoadEvent event, LoadType loadType) {
		checkOpen();
		checkTransactionSynchStatus();
		for ( LoadEventListener listener : listeners( EventType.LOAD ) ) {
			listener.onLoad( event, loadType );
		}
		delayedAfterCompletion();
	}

	private void fireResolveNaturalId(ResolveNaturalIdEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		for ( ResolveNaturalIdEventListener listener : listeners( EventType.RESOLVE_NATURAL_ID ) ) {
			listener.onResolveNaturalId( event );
		}
		delayedAfterCompletion();
	}


	// refresh() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void refresh(Object object) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( null, object, this ) );
	}

	@Override
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

	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) throws HibernateException {
		checkOpen();
		fireRefresh( new RefreshEvent( entityName, object, lockOptions, this ) );
	}

	@Override
	public void refresh(String entityName, Object object, Map refreshedAlready) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		fireRefresh( refreshedAlready, new RefreshEvent( entityName, object, this ) );
	}

	private void fireRefresh(RefreshEvent event) {
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
			checkTransactionSynchStatus();
			for ( RefreshEventListener listener : listeners( EventType.REFRESH ) ) {
				listener.onRefresh( event );
			}
		}
		catch (RuntimeException e) {
			if ( !getSessionFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
				if ( e instanceof HibernateException ) {
					throw e;
				}
			}
			//including HibernateException
			throw exceptionConverter.convert( e );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void fireRefresh(Map refreshedAlready, RefreshEvent event) {
		try {
			checkTransactionSynchStatus();
			for ( RefreshEventListener listener : listeners( EventType.REFRESH ) ) {
				listener.onRefresh( event, refreshedAlready );
			}
			delayedAfterCompletion();
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
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

	private void fireReplicate(ReplicateEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		for ( ReplicateEventListener listener : listeners( EventType.REPLICATE ) ) {
			listener.onReplicate( event );
		}
		delayedAfterCompletion();
	}


	// evict() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * remove any hard references to the entity that are held by the infrastructure
	 * (references held by application or other persistent instances are okay)
	 */
	@Override
	public void evict(Object object) throws HibernateException {
		fireEvict( new EvictEvent( object, this ) );
	}

	private void fireEvict(EvictEvent event) {
		checkOpen();
		checkTransactionSynchStatus();
		for ( EvictEventListener listener : listeners( EventType.EVICT ) ) {
			listener.onEvict( event );
		}
		delayedAfterCompletion();
	}

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 */
	protected boolean autoFlushIfRequired(Set querySpaces) throws HibernateException {
		checkOpen();
		if ( !isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		AutoFlushEvent event = new AutoFlushEvent( querySpaces, this );
		listeners( EventType.AUTO_FLUSH );
		for ( AutoFlushEventListener listener : listeners( EventType.AUTO_FLUSH ) ) {
			listener.onAutoFlush( event );
		}
		return event.isFlushRequired();
	}

	@Override
	public boolean isDirty() throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		log.debug( "Checking session dirtiness" );
		if ( actionQueue.areInsertionsOrDeletionsQueued() ) {
			log.debug( "Session dirty (scheduled updates and insertions)" );
			return true;
		}
		DirtyCheckEvent event = new DirtyCheckEvent( this );
		for ( DirtyCheckEventListener listener : listeners( EventType.DIRTY_CHECK ) ) {
			listener.onDirtyCheck( event );
		}
		delayedAfterCompletion();
		return event.isDirty();
	}

	@Override
	public void flush() throws HibernateException {
		checkOpen();
		doFlush();
	}

	private void doFlush() {
		checkTransactionNeeded();
		checkTransactionSynchStatus();

		try {
			if ( persistenceContext.getCascadeLevel() > 0 ) {
				throw new HibernateException( "Flush during cascade is dangerous" );
			}

			FlushEvent flushEvent = new FlushEvent( this );
			for ( FlushEventListener listener : listeners( EventType.FLUSH ) ) {
				listener.onFlush( flushEvent );
			}

			delayedAfterCompletion();
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public void setFlushMode(FlushModeType flushModeType) {
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
	}

	@Override
	public void forceFlush(EntityEntry entityEntry) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Flushing to force deletion of re-saved object: %s",
					MessageHelper.infoString( entityEntry.getPersister(), entityEntry.getId(), getFactory() )
			);
		}

		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ObjectDeletedException(
					"deleted object would be re-saved by cascade (remove deleted object from associations)",
					entityEntry.getId(),
					entityEntry.getPersister().getEntityName()
			);
		}
		checkOpenOrWaitingForAutoClose();
		doFlush();
	}

	@Override
	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();

		HQLQueryPlan plan = queryParameters.getQueryPlan();
		if ( plan == null ) {
			plan = getQueryPlan( query, false );
		}

		autoFlushIfRequired( plan.getQuerySpaces() );

		List results = Collections.EMPTY_LIST;
		boolean success = false;

		dontFlushFromFind++;   //stops flush being called multiple times if this method is recursively called
		try {
			results = plan.performList( queryParameters, this );
			success = true;
		}
		finally {
			dontFlushFromFind--;
			afterOperation( success );
			delayedAfterCompletion();
		}
		return results;
	}

	@Override
	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getQueryPlan( query, false );
		autoFlushIfRequired( plan.getQuerySpaces() );

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
			delayedAfterCompletion();
		}
		return result;
	}

	@Override
	public int executeNativeUpdate(
			NativeSQLQuerySpecification nativeQuerySpecification,
			QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		NativeSQLQueryPlan plan = getNativeQueryPlan( nativeQuerySpecification );


		autoFlushIfRequired( plan.getCustomQuery().getQuerySpaces() );

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation( success );
			delayedAfterCompletion();
		}
		return result;
	}

	@Override
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();

		HQLQueryPlan plan = queryParameters.getQueryPlan();
		if ( plan == null ) {
			plan = getQueryPlan( query, true );
		}

		autoFlushIfRequired( plan.getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return plan.performIterate( queryParameters, this );
		}
		finally {
			delayedAfterCompletion();
			dontFlushFromFind--;
		}
	}

	@Override
	public ScrollableResultsImplementor scroll(String query, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		
		HQLQueryPlan plan = queryParameters.getQueryPlan();
		if ( plan == null ) {
			plan = getQueryPlan( query, false );
		}
		
		autoFlushIfRequired( plan.getQuerySpaces() );

		dontFlushFromFind++;
		try {
			return plan.performScroll( queryParameters, this );
		}
		finally {
			delayedAfterCompletion();
			dontFlushFromFind--;
		}
	}

	@Override
	public org.hibernate.query.Query createFilter(Object collection, String queryString) {
		checkOpen();
		checkTransactionSynchStatus();
		CollectionFilterImpl filter = new CollectionFilterImpl(
				queryString,
				collection,
				this,
				getFilterQueryPlan( collection, queryString, null, false ).getParameterMetadata()
		);
		filter.setComment( queryString );
		delayedAfterCompletion();
		return filter;
	}


	@Override
	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return instantiate( getFactory().getMetamodel().entityPersister( entityName ), id );
	}

	/**
	 * give the interceptor an opportunity to override the default instantiation
	 */
	@Override
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException {
		checkOpenOrWaitingForAutoClose();
		checkTransactionSynchStatus();
		Object result = getInterceptor().instantiate(
				persister.getEntityName(),
				persister.getEntityMetamodel().getEntityMode(),
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
			return getFactory().getMetamodel().entityPersister( guessEntityName( object ) );
		}
		else {
			// try block is a hack around fact that currently tuplizers are not
			// given the opportunity to resolve a subclass entity name.  this
			// allows the (we assume custom) interceptor the ability to
			// influence this decision if we were not able to based on the
			// given entityName
			try {
				return getFactory().getMetamodel().entityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
			}
			catch (HibernateException e) {
				try {
					return getEntityPersister( null, object );
				}
				catch (HibernateException e2) {
					throw e;
				}
			}
		}
	}

	// not for internal use:
	@Override
	public Serializable getIdentifier(Object object) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();
		if ( object instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.getSession() != this ) {
				throw new TransientObjectException( "The proxy was not associated with this session" );
			}
			return li.getIdentifier();
		}
		else {
			EntityEntry entry = persistenceContext.getEntry( object );
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
	public Serializable getContextEntityIdentifier(Object object) {
		checkOpenOrWaitingForAutoClose();
		if ( object instanceof HibernateProxy ) {
			return getProxyIdentifier( object );
		}
		else {
			EntityEntry entry = persistenceContext.getEntry( object );
			return entry != null ? entry.getId() : null;
		}
	}

	private Serializable getProxyIdentifier(Object proxy) {
		return ( (HibernateProxy) proxy ).getHibernateLazyInitializer().getIdentifier();
	}

	private FilterQueryPlan getFilterQueryPlan(
			Object collection,
			String filter,
			QueryParameters parameters,
			boolean shallow) throws HibernateException {
		if ( collection == null ) {
			throw new NullPointerException( "null collection passed to filter" );
		}

		CollectionEntry entry = persistenceContext.getCollectionEntryOrNull( collection );
		final CollectionPersister roleBeforeFlush = ( entry == null ) ? null : entry.getLoadedPersister();

		FilterQueryPlan plan = null;
		if ( roleBeforeFlush == null ) {
			// if it was previously unreferenced, we need to flush in order to
			// get its state into the database in order to execute query
			flush();
			entry = persistenceContext.getCollectionEntryOrNull( collection );
			CollectionPersister roleAfterFlush = ( entry == null ) ? null : entry.getLoadedPersister();
			if ( roleAfterFlush == null ) {
				throw new QueryException( "The collection was unreferenced" );
			}
			plan = getFactory().getQueryPlanCache().getFilterQueryPlan(
					filter,
					roleAfterFlush.getRole(),
					shallow,
					getLoadQueryInfluencers().getEnabledFilters()
			);
		}
		else {
			// otherwise, we only need to flush if there are in-memory changes
			// to the queried tables
			plan = getFactory().getQueryPlanCache().getFilterQueryPlan(
					filter,
					roleBeforeFlush.getRole(),
					shallow,
					getLoadQueryInfluencers().getEnabledFilters()
			);
			if ( autoFlushIfRequired( plan.getQuerySpaces() ) ) {
				// might need to run a different filter entirely afterQuery the flush
				// because the collection role may have changed
				entry = persistenceContext.getCollectionEntryOrNull( collection );
				CollectionPersister roleAfterFlush = ( entry == null ) ? null : entry.getLoadedPersister();
				if ( roleBeforeFlush != roleAfterFlush ) {
					if ( roleAfterFlush == null ) {
						throw new QueryException( "The collection was dereferenced" );
					}
					plan = getFactory().getQueryPlanCache().getFilterQueryPlan(
							filter,
							roleAfterFlush.getRole(),
							shallow,
							getLoadQueryInfluencers().getEnabledFilters()
					);
				}
			}
		}

		if ( parameters != null ) {
			parameters.getPositionalParameterValues()[0] = entry.getLoadedKey();
			parameters.getPositionalParameterTypes()[0] = entry.getLoadedPersister().getKeyType();
		}

		return plan;
	}

	@Override
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) {
		checkOpen();
		checkTransactionSynchStatus();
		FilterQueryPlan plan = getFilterQueryPlan( collection, filter, queryParameters, false );
		List results = Collections.EMPTY_LIST;

		boolean success = false;
		dontFlushFromFind++;   //stops flush being called multiple times if this method is recursively called
		try {
			results = plan.performList( queryParameters, this );
			success = true;
		}
		finally {
			dontFlushFromFind--;
			afterOperation( success );
			delayedAfterCompletion();
		}
		return results;
	}

	@Override
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) {
		checkOpen();
		checkTransactionSynchStatus();
		FilterQueryPlan plan = getFilterQueryPlan( collection, filter, queryParameters, true );
		Iterator itr = plan.performIterate( queryParameters, this );
		delayedAfterCompletion();
		return itr;
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedLegacyCriteria();
		checkOpen();
		checkTransactionSynchStatus();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedLegacyCriteria();
		checkOpen();
		checkTransactionSynchStatus();
		return new CriteriaImpl( entityName, alias, this );
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedLegacyCriteria();
		checkOpen();
		checkTransactionSynchStatus();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		DeprecationLogger.DEPRECATION_LOGGER.deprecatedLegacyCriteria();
		checkOpen();
		checkTransactionSynchStatus();
		return new CriteriaImpl( entityName, this );
	}

	@Override
	public ScrollableResultsImplementor scroll(Criteria criteria, ScrollMode scrollMode) {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;

		checkOpen();
		checkTransactionSynchStatus();

		String entityName = criteriaImpl.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable( entityName ),
				getFactory(),
				criteriaImpl,
				entityName,
				getLoadQueryInfluencers()
		);
		autoFlushIfRequired( loader.getQuerySpaces() );
		dontFlushFromFind++;
		try {
			return loader.scroll( this, scrollMode );
		}
		finally {
			delayedAfterCompletion();
			dontFlushFromFind--;
		}
	}

	@Override
	public List list(Criteria criteria) throws HibernateException {
		// TODO: Is this guaranteed to always be CriteriaImpl?
		CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;
		if ( criteriaImpl.getMaxResults() != null && criteriaImpl.getMaxResults() == 0 ) {
			return Collections.EMPTY_LIST;
		}

		final NaturalIdLoadAccess naturalIdLoadAccess = this.tryNaturalIdLoadAccess( criteriaImpl );
		if ( naturalIdLoadAccess != null ) {
			// EARLY EXIT!
			return Arrays.asList( naturalIdLoadAccess.load() );
		}


		checkOpen();
//		checkTransactionSynchStatus();

		String[] implementors = getFactory().getMetamodel().getImplementors( criteriaImpl.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		Set spaces = new HashSet();
		for ( int i = 0; i < size; i++ ) {

			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
					getFactory(),
					criteriaImpl,
					implementors[i],
					getLoadQueryInfluencers()
			);

			spaces.addAll( loaders[i].getQuerySpaces() );

		}

		autoFlushIfRequired( spaces );

		List results = Collections.EMPTY_LIST;
		dontFlushFromFind++;
		boolean success = false;
		try {
			for ( int i = 0; i < size; i++ ) {
				final List currentResults = loaders[i].list( this );
				currentResults.addAll( results );
				results = currentResults;
			}
			success = true;
		}
		finally {
			dontFlushFromFind--;
			afterOperation( success );
			delayedAfterCompletion();
		}

		return results;
	}

	/**
	 * Checks to see if the CriteriaImpl is a naturalId lookup that can be done via
	 * NaturalIdLoadAccess
	 *
	 * @param criteria The criteria to check as a complete natural identifier lookup.
	 *
	 * @return A fully configured NaturalIdLoadAccess or null, if null is returned the standard CriteriaImpl execution
	 * should be performed
	 */
	private NaturalIdLoadAccess tryNaturalIdLoadAccess(CriteriaImpl criteria) {
		// See if the criteria lookup is by naturalId
		if ( !criteria.isLookupByNaturalKey() ) {
			return null;
		}

		final String entityName = criteria.getEntityOrClassName();
		final EntityPersister entityPersister = getFactory().getMetamodel().entityPersister( entityName );

		// Verify the entity actually has a natural id, needed for legacy support as NaturalIdentifier criteria
		// queries did no natural id validation
		if ( !entityPersister.hasNaturalIdentifier() ) {
			return null;
		}

		// Since isLookupByNaturalKey is true there can be only one CriterionEntry and getCriterion() will
		// return an instanceof NaturalIdentifier
		final CriterionEntry criterionEntry = criteria.iterateExpressionEntries().next();
		final NaturalIdentifier naturalIdentifier = (NaturalIdentifier) criterionEntry.getCriterion();

		final Map<String, Object> naturalIdValues = naturalIdentifier.getNaturalIdValues();
		final int[] naturalIdentifierProperties = entityPersister.getNaturalIdentifierProperties();

		// Verify the NaturalIdentifier criterion includes all naturalId properties, first check that the property counts match
		if ( naturalIdentifierProperties.length != naturalIdValues.size() ) {
			return null;
		}

		final String[] propertyNames = entityPersister.getPropertyNames();
		final NaturalIdLoadAccess naturalIdLoader = this.byNaturalId( entityName );

		// Build NaturalIdLoadAccess and in the process verify all naturalId properties were specified
		for ( int naturalIdentifierProperty : naturalIdentifierProperties ) {
			final String naturalIdProperty = propertyNames[naturalIdentifierProperty];
			final Object naturalIdValue = naturalIdValues.get( naturalIdProperty );

			if ( naturalIdValue == null ) {
				// A NaturalId property is missing from the critera query, can't use NaturalIdLoadAccess
				return null;
			}

			naturalIdLoader.using( naturalIdProperty, naturalIdValue );
		}

		// Criteria query contains a valid naturalId, use the new API
		log.warn(
				"Session.byNaturalId(" + entityName
						+ ") should be used for naturalId queries instead of Restrictions.naturalId() from a Criteria"
		);

		return naturalIdLoader;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = getFactory().getMetamodel().entityPersister( entityName );
		if ( !( persister instanceof OuterJoinLoadable ) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return (OuterJoinLoadable) persister;
	}

	@Override
	public boolean contains(Object object) {
		checkOpen();
		checkTransactionSynchStatus();

		if ( object == null ) {
			return false;
		}

		try {
			if ( object instanceof HibernateProxy ) {
				//do not use proxiesByKey, since not all
				//proxies that point to this session's
				//instances are in that collection!
				LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
				if ( li.isUninitialized() ) {
					//if it is an uninitialized proxy, pointing
					//with this session, then when it is accessed,
					//the underlying instance will be "contained"
					return li.getSession() == this;
				}
				else {
					//if it is initialized, see if the underlying
					//instance is contained, since we need to
					//account for the fact that it might have been
					//evicted
					object = li.getImplementation();
				}
			}

			// A session is considered to contain an entity only if the entity has
			// an entry in the session's persistence context and the entry reports
			// that the entity has not been removed
			EntityEntry entry = persistenceContext.getEntry( object );
			delayedAfterCompletion();

			if ( entry == null ) {
				if ( !HibernateProxy.class.isInstance( object ) && persistenceContext.getEntry( object ) == null ) {
					// check if it is even an entity -> if not throw an exception (per JPA)
					try {
						final String entityName = getEntityNameResolver().resolveEntityName( object );
						if ( entityName == null ) {
							throw new IllegalArgumentException( "Could not resolve entity-name [" + object + "]" );
						}
						getSessionFactory().getMetamodel().entityPersister( object.getClass() );
					}
					catch (HibernateException e) {
						throw new IllegalArgumentException( "Not an entity [" + object.getClass() + "]", e );
					}
				}
				return false;
			}
			else {
				return entry.getStatus() != Status.DELETED && entry.getStatus() != Status.GONE;
			}
		}
		catch (MappingException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public boolean contains(String entityName, Object object) {
		checkOpen();
		checkTransactionSynchStatus();

		if ( object == null ) {
			return false;
		}

		try {
			if ( !HibernateProxy.class.isInstance( object ) && persistenceContext.getEntry( object ) == null ) {
				// check if it is an entity -> if not throw an exception (per JPA)
				try {
					getSessionFactory().getMetamodel().entityPersister( entityName );
				}
				catch (HibernateException e) {
					throw new IllegalArgumentException( "Not an entity [" + entityName + "] : " + object );
				}
			}

			if ( object instanceof HibernateProxy ) {
				//do not use proxiesByKey, since not all
				//proxies that point to this session's
				//instances are in that collection!
				LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
				if ( li.isUninitialized() ) {
					//if it is an uninitialized proxy, pointing
					//with this session, then when it is accessed,
					//the underlying instance will be "contained"
					return li.getSession() == this;
				}
				else {
					//if it is initialized, see if the underlying
					//instance is contained, since we need to
					//account for the fact that it might have been
					//evicted
					object = li.getImplementation();
				}
			}
			// A session is considered to contain an entity only if the entity has
			// an entry in the session's persistence context and the entry reports
			// that the entity has not been removed
			EntityEntry entry = persistenceContext.getEntry( object );
			delayedAfterCompletion();
			return entry != null && entry.getStatus() != Status.DELETED && entry.getStatus() != Status.GONE;
		}
		catch (MappingException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
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
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		checkOpen();
//		checkTransactionSynchStatus();
		return super.createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public ScrollableResultsImplementor scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) {
		checkOpen();
//		checkTransactionSynchStatus();

		if ( log.isTraceEnabled() ) {
			log.tracev( "Scroll SQL query: {0}", customQuery.getSQL() );
		}

		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return loader.scroll( queryParameters, this );
		}
		finally {
			delayedAfterCompletion();
			dontFlushFromFind--;
		}
	}

	// basically just an adapted copy of find(CriteriaImpl)
	@Override
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) {
		checkOpen();
//		checkTransactionSynchStatus();

		if ( log.isTraceEnabled() ) {
			log.tracev( "SQL query: {0}", customQuery.getSQL() );
		}

		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++;
		boolean success = false;
		try {
			List results = loader.list( this, queryParameters );
			success = true;
			return results;
		}
		finally {
			dontFlushFromFind--;
			delayedAfterCompletion();
			afterOperation( success );
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
//		checkTransactionSynchStatus();
		return getFactory();
	}

	@Override
	public void initializeCollection(PersistentCollection collection, boolean writing) {
		checkOpenOrWaitingForAutoClose();
		checkTransactionSynchStatus();
		InitializeCollectionEvent event = new InitializeCollectionEvent( collection, this );
		for ( InitializeCollectionEventListener listener : listeners( EventType.INIT_COLLECTION ) ) {
			listener.onInitializeCollection( event );
		}
		delayedAfterCompletion();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		if ( object instanceof HibernateProxy ) {
			LazyInitializer initializer = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( initializer.isUninitialized() ) {
				return initializer.getEntityName();
			}
			object = initializer.getImplementation();
		}
		EntityEntry entry = persistenceContext.getEntry( object );
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
		if ( object instanceof HibernateProxy ) {
			if ( !persistenceContext.containsProxy( object ) ) {
				throw new TransientObjectException( "proxy was not associated with the session" );
			}
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}

		EntityEntry entry = persistenceContext.getEntry( object );
		if ( entry == null ) {
			throwTransientObjectException( object );
		}
		return entry.getPersister().getEntityName();
	}

	private void throwTransientObjectException(Object object) throws HibernateException {
		throw new TransientObjectException(
				"object references an unsaved transient instance - save the transient instance beforeQuery flushing: " +
						guessEntityName( object )
		);
	}

	@Override
	public String guessEntityName(Object object) throws HibernateException {
		checkOpen();
		return getEntityNameResolver().resolveEntityName( object );
	}

	@Override
	public void cancelQuery() throws HibernateException {
		checkOpen();
		getJdbcCoordinator().cancelLastQuery();
	}


	@Override
	public int getDontFlushFromFind() {
		return dontFlushFromFind;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder( 500 )
				.append( "SessionImpl(" );
		if ( !isClosed() ) {
			buf.append( persistenceContext )
					.append( ";" )
					.append( actionQueue );
		}
		else {
			buf.append( "<closed>" );
		}
		return buf.append( ')' ).toString();
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
	public SessionStatistics getStatistics() {
		checkTransactionSynchStatus();
		return new SessionStatisticsImpl( this );
	}

	@Override
	public boolean isEventSource() {
		checkTransactionSynchStatus();
		return true;
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
	public void doWork(final Work work) throws HibernateException {
		WorkExecutorVisitable<Void> realWork = new WorkExecutorVisitable<Void>() {
			@Override
			public Void accept(WorkExecutor<Void> workExecutor, Connection connection) throws SQLException {
				workExecutor.executeWork( work, connection );
				return null;
			}
		};
		doWork( realWork );
	}

	@Override
	public <T> T doReturningWork(final ReturningWork<T> work) throws HibernateException {
		WorkExecutorVisitable<T> realWork = new WorkExecutorVisitable<T>() {
			@Override
			public T accept(WorkExecutor<T> workExecutor, Connection connection) throws SQLException {
				return workExecutor.executeReturningWork( work, connection );
			}
		};
		return doWork( realWork );
	}

	private <T> T doWork(WorkExecutorVisitable<T> work) throws HibernateException {
		return getJdbcCoordinator().coordinateWork( work );
	}

	@Override
	public void afterScrollOperation() {
		// nothing to do in a stateful session
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Filter getEnabledFilter(String filterName) {
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getEnabledFilter( filterName );
	}

	@Override
	public Filter enableFilter(String filterName) {
		checkOpen();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.enableFilter( filterName );
	}

	@Override
	public void disableFilter(String filterName) {
		checkOpen();
		checkTransactionSynchStatus();
		loadQueryInfluencers.disableFilter( filterName );
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
	public TypeHelper getTypeHelper() {
		return getSessionFactory().getTypeHelper();
	}

	@Override
	public LobHelper getLobHelper() {
		if ( lobHelper == null ) {
			lobHelper = new LobHelperImpl( this );
		}
		return lobHelper;
	}

	private transient LobHelperImpl lobHelper;

	@Override
	public void beforeTransactionCompletion() {
		log.tracef( "SessionImpl#beforeTransactionCompletion()" );
		flushBeforeTransactionCompletion();
		actionQueue.beforeTransactionCompletion();
		try {
			getInterceptor().beforeTransactionCompletion( getCurrentTransaction() );
		}
		catch (Throwable t) {
			log.exceptionInBeforeTransactionCompletionInterceptor( t );
		}
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		log.tracef( "SessionImpl#afterTransactionCompletion(successful=%s, delayed=%s)", successful, delayed );

		if ( !isClosed() || waitingForAutoClose ) {
			if ( autoClear ||!successful ) {
				internalClear();
			}
		}

		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion( successful );

		getEventListenerManager().transactionCompletion( successful );

		if ( getFactory().getStatistics().isStatisticsEnabled() ) {
			getFactory().getStatistics().endTransaction( successful );
		}

		try {
			getInterceptor().afterTransactionCompletion( getCurrentTransaction() );
		}
		catch (Throwable t) {
			log.exceptionInAfterTransactionCompletionInterceptor( t );
		}

		if ( !delayed ) {
			if ( shouldAutoClose() && (!isClosed() || waitingForAutoClose) ) {
				managedClose();
			}
		}
	}

	private static class LobHelperImpl implements LobHelper {
		private final SessionImpl session;

		private LobHelperImpl(SessionImpl session) {
			this.session = session;
		}

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

	private static class SharedSessionBuilderImpl<T extends SharedSessionBuilder>
			extends SessionFactoryImpl.SessionBuilderImpl<T>
			implements SharedSessionBuilder<T>, SharedSessionCreationOptions {
		private final SessionImpl session;
		private boolean shareTransactionContext;

		private SharedSessionBuilderImpl(SessionImpl session) {
			super( (SessionFactoryImpl) session.getFactory() );
			this.session = session;
			super.owner( session.sessionOwner );
			super.tenantIdentifier( session.getTenantIdentifier() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SharedSessionBuilder


		@Override
		public T tenantIdentifier(String tenantIdentifier) {
			// todo : is this always true?  Or just in the case of sharing JDBC resources?
			throw new SessionException( "Cannot redefine tenant identifier on child session" );
		}

		@Override
		public T interceptor() {
			return interceptor( session.getInterceptor() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public T connection() {
			this.shareTransactionContext = true;
			return (T) this;
		}

		@Override
		public T connectionReleaseMode() {
			return connectionReleaseMode( session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode().getReleaseMode() );
		}

		@Override
		public T connectionHandlingMode() {
			return connectionHandlingMode( session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode() );
		}

		@Override
		public T autoJoinTransactions() {
			return autoJoinTransactions( session.isAutoCloseSessionEnabled() );
		}

		@Override
		public T flushMode() {
			return flushMode( session.getHibernateFlushMode() );
		}

		@Override
		public T autoClose() {
			return autoClose( session.autoClose );
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

	private class LockRequestImpl implements LockRequest {
		private final LockOptions lockOptions;

		private LockRequestImpl(LockOptions lo) {
			lockOptions = new LockOptions();
			LockOptions.copy( lo, lockOptions );
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

		@Override
		public boolean getScope() {
			return lockOptions.getScope();
		}

		@Override
		public LockRequest setScope(boolean scope) {
			lockOptions.setScope( scope );
			return this;
		}

		@Override
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
		transactionCoordinator.addObserver(
				new TransactionObserver() {
					@Override
					public void afterBegin() {
					}

					@Override
					public void beforeCompletion() {
						if ( isOpen() && getHibernateFlushMode() !=  FlushMode.MANUAL ) {
							managedFlush();
						}
						actionQueue.beforeTransactionCompletion();
						try {
							getInterceptor().beforeTransactionCompletion( getCurrentTransaction() );
						}
						catch (Throwable t) {
							log.exceptionInBeforeTransactionCompletionInterceptor( t );
						}
					}

					@Override
					public void afterCompletion(boolean successful, boolean delayed) {
						afterTransactionCompletion( successful, delayed );
						if ( !isClosed() && autoClose ) {
							managedClose();
						}
					}
				}
		);
	}

	private class IdentifierLoadAccessImpl<T> implements IdentifierLoadAccess<T> {
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;
		private CacheMode cacheMode;

		private IdentifierLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}

		private IdentifierLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private IdentifierLoadAccessImpl(Class<T> entityClass) {
			this( locateEntityPersister( entityClass ) );
		}

		@Override
		public final IdentifierLoadAccessImpl<T> with(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		@Override
		public IdentifierLoadAccess<T> with(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return this;
		}

		@Override
		public final T getReference(Serializable id) {
			CacheMode sessionCacheMode = getCacheMode();
			boolean cacheModeChanged = false;
			if ( cacheMode != null ) {
				// naive check for now...
				// todo : account for "conceptually equal"
				if ( cacheMode != sessionCacheMode ) {
					setCacheMode( cacheMode );
					cacheModeChanged = true;
				}
			}

			try {
				return doGetReference( id );
			}
			finally {
				if ( cacheModeChanged ) {
					// change it back
					setCacheMode( sessionCacheMode );
				}
			}
		}

		@SuppressWarnings("unchecked")
		protected T doGetReference(Serializable id) {
			if ( this.lockOptions != null ) {
				LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, SessionImpl.this );
				fireLoad( event, LoadEventListener.LOAD );
				return (T) event.getResult();
			}

			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, SessionImpl.this );
			boolean success = false;
			try {
				fireLoad( event, LoadEventListener.LOAD );
				if ( event.getResult() == null ) {
					getFactory().getEntityNotFoundDelegate().handleEntityNotFound(
							entityPersister.getEntityName(),
							id
					);
				}
				success = true;
				return (T) event.getResult();
			}
			finally {
				afterOperation( success );
			}
		}

		@Override
		public final T load(Serializable id) {
			CacheMode sessionCacheMode = getCacheMode();
			boolean cacheModeChanged = false;
			if ( cacheMode != null ) {
				// naive check for now...
				// todo : account for "conceptually equal"
				if ( cacheMode != sessionCacheMode ) {
					setCacheMode( cacheMode );
					cacheModeChanged = true;
				}
			}

			try {
				return doLoad( id );
			}
			finally {
				if ( cacheModeChanged ) {
					// change it back
					setCacheMode( sessionCacheMode );
				}
			}
		}

		@Override
		public Optional<T> loadOptional(Serializable id) {
			return Optional.ofNullable( load( id ) );
		}

		@SuppressWarnings("unchecked")
		protected final T doLoad(Serializable id) {
			if ( this.lockOptions != null ) {
				LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, SessionImpl.this );
				fireLoad( event, LoadEventListener.GET );
				return (T) event.getResult();
			}

			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, SessionImpl.this );
			boolean success = false;
			try {
				fireLoad( event, LoadEventListener.GET );
				success = true;
			}
			catch (ObjectNotFoundException e) {
				// if session cache contains proxy for non-existing object
			}
			finally {
				afterOperation( success );
			}
			return (T) event.getResult();
		}
	}

	private class MultiIdentifierLoadAccessImpl<T> implements MultiIdentifierLoadAccess<T>, MultiLoadOptions {
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;
		private CacheMode cacheMode;
		private Integer batchSize;
		private boolean sessionCheckingEnabled;
		private boolean returnOfDeletedEntitiesEnabled;
		private boolean orderedReturnEnabled = true;

		public MultiIdentifierLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}

		@Override
		public LockOptions getLockOptions() {
			return lockOptions;
		}

		@Override
		public final MultiIdentifierLoadAccessImpl<T> with(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		@Override
		public MultiIdentifierLoadAccessImpl<T> with(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return this;
		}

		@Override
		public Integer getBatchSize() {
			return batchSize;
		}

		@Override
		public MultiIdentifierLoadAccess<T> withBatchSize(int batchSize) {
			if ( batchSize < 1 ) {
				this.batchSize = null;
			}
			else {
				this.batchSize = batchSize;
			}
			return this;
		}

		@Override
		public boolean isSessionCheckingEnabled() {
			return sessionCheckingEnabled;
		}

		@Override
		public MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled) {
			this.sessionCheckingEnabled = enabled;
			return this;
		}

		@Override
		public boolean isReturnOfDeletedEntitiesEnabled() {
			return returnOfDeletedEntitiesEnabled;
		}

		@Override
		public MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
			this.returnOfDeletedEntitiesEnabled = enabled;
			return this;
		}

		@Override
		public boolean isOrderReturnEnabled() {
			return orderedReturnEnabled;
		}

		@Override
		public MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled) {
			this.orderedReturnEnabled = enabled;
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <K extends Serializable> List<T> multiLoad(K... ids) {
			CacheMode sessionCacheMode = getCacheMode();
			boolean cacheModeChanged = false;
			if ( cacheMode != null ) {
				// naive check for now...
				// todo : account for "conceptually equal"
				if ( cacheMode != sessionCacheMode ) {
					setCacheMode( cacheMode );
					cacheModeChanged = true;
				}
			}

			try {
				return entityPersister.multiLoad( ids, SessionImpl.this, this );
			}
			finally {
				if ( cacheModeChanged ) {
					// change it back
					setCacheMode( sessionCacheMode );
				}
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public <K extends Serializable> List<T> multiLoad(List<K> ids) {
			CacheMode sessionCacheMode = getCacheMode();
			boolean cacheModeChanged = false;
			if ( cacheMode != null ) {
				// naive check for now...
				// todo : account for "conceptually equal"
				if ( cacheMode != sessionCacheMode ) {
					setCacheMode( cacheMode );
					cacheModeChanged = true;
				}
			}

			try {
				return entityPersister.multiLoad( ids.toArray( new Serializable[ ids.size() ] ), SessionImpl.this, this );
			}
			finally {
				if ( cacheModeChanged ) {
					// change it back
					setCacheMode( sessionCacheMode );
				}
			}
		}
	}

	private EntityPersister locateEntityPersister(Class entityClass) {
		return getFactory().getMetamodel().locateEntityPersister( entityClass );
	}

	private EntityPersister locateEntityPersister(String entityName) {
		return getFactory().getMetamodel().locateEntityPersister( entityName );
	}

	private abstract class BaseNaturalIdLoadAccessImpl<T> {
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;
		private boolean synchronizationEnabled = true;

		private BaseNaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;

			if ( !entityPersister.hasNaturalIdentifier() ) {
				throw new HibernateException(
						String.format( "Entity [%s] did not define a natural id", entityPersister.getEntityName() )
				);
			}
		}

		public BaseNaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		protected void synchronizationEnabled(boolean synchronizationEnabled) {
			this.synchronizationEnabled = synchronizationEnabled;
		}

		protected final Serializable resolveNaturalId(Map<String, Object> naturalIdParameters) {
			performAnyNeededCrossReferenceSynchronizations();

			final ResolveNaturalIdEvent event =
					new ResolveNaturalIdEvent( naturalIdParameters, entityPersister, SessionImpl.this );
			fireResolveNaturalId( event );

			if ( event.getEntityId() == PersistenceContext.NaturalIdHelper.INVALID_NATURAL_ID_REFERENCE ) {
				return null;
			}
			else {
				return event.getEntityId();
			}
		}

		protected void performAnyNeededCrossReferenceSynchronizations() {
			if ( !synchronizationEnabled ) {
				// synchronization (this process) was disabled
				return;
			}
			if ( entityPersister.getEntityMetamodel().hasImmutableNaturalId() ) {
				// only mutable natural-ids need this processing
				return;
			}
			if ( !isTransactionInProgress() ) {
				// not in a transaction so skip synchronization
				return;
			}

			final boolean debugEnabled = log.isDebugEnabled();
			for ( Serializable pk : getPersistenceContext().getNaturalIdHelper()
					.getCachedPkResolutions( entityPersister ) ) {
				final EntityKey entityKey = generateEntityKey( pk, entityPersister );
				final Object entity = getPersistenceContext().getEntity( entityKey );
				final EntityEntry entry = getPersistenceContext().getEntry( entity );

				if ( entry == null ) {
					if ( debugEnabled ) {
						log.debug(
								"Cached natural-id/pk resolution linked to null EntityEntry in persistence context : "
										+ MessageHelper.infoString( entityPersister, pk, getFactory() )
						);
					}
					continue;
				}

				if ( !entry.requiresDirtyCheck( entity ) ) {
					continue;
				}

				// MANAGED is the only status we care about here...
				if ( entry.getStatus() != Status.MANAGED ) {
					continue;
				}

				getPersistenceContext().getNaturalIdHelper().handleSynchronization(
						entityPersister,
						pk,
						entity
				);
			}
		}

		protected final IdentifierLoadAccess getIdentifierLoadAccess() {
			final IdentifierLoadAccessImpl identifierLoadAccess = new IdentifierLoadAccessImpl( entityPersister );
			if ( this.lockOptions != null ) {
				identifierLoadAccess.with( lockOptions );
			}
			return identifierLoadAccess;
		}

		protected EntityPersister entityPersister() {
			return entityPersister;
		}
	}

	private class NaturalIdLoadAccessImpl<T> extends BaseNaturalIdLoadAccessImpl<T> implements NaturalIdLoadAccess<T> {
		private final Map<String, Object> naturalIdParameters = new LinkedHashMap<String, Object>();

		private NaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			super( entityPersister );
		}

		private NaturalIdLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private NaturalIdLoadAccessImpl(Class entityClass) {
			this( locateEntityPersister( entityClass ) );
		}

		@Override
		public NaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
			return (NaturalIdLoadAccessImpl<T>) super.with( lockOptions );
		}

		@Override
		public NaturalIdLoadAccess<T> using(String attributeName, Object value) {
			naturalIdParameters.put( attributeName, value );
			return this;
		}

		@Override
		public NaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
			super.synchronizationEnabled( synchronizationEnabled );
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public final T getReference() {
			final Serializable entityId = resolveNaturalId( this.naturalIdParameters );
			if ( entityId == null ) {
				return null;
			}
			return (T) this.getIdentifierLoadAccess().getReference( entityId );
		}

		@Override
		@SuppressWarnings("unchecked")
		public final T load() {
			final Serializable entityId = resolveNaturalId( this.naturalIdParameters );
			if ( entityId == null ) {
				return null;
			}
			try {
				return (T) this.getIdentifierLoadAccess().load( entityId );
			}
			catch (EntityNotFoundException | ObjectNotFoundException enf) {
				// OK
			}
			return null;
		}

		@Override
		public Optional<T> loadOptional() {
			return Optional.ofNullable( load() );
		}
	}

	private class SimpleNaturalIdLoadAccessImpl<T> extends BaseNaturalIdLoadAccessImpl<T>
			implements SimpleNaturalIdLoadAccess<T> {
		private final String naturalIdAttributeName;

		private SimpleNaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			super( entityPersister );

			if ( entityPersister.getNaturalIdentifierProperties().length != 1 ) {
				throw new HibernateException(
						String.format(
								"Entity [%s] did not define a simple natural id",
								entityPersister.getEntityName()
						)
				);
			}

			final int naturalIdAttributePosition = entityPersister.getNaturalIdentifierProperties()[0];
			this.naturalIdAttributeName = entityPersister.getPropertyNames()[naturalIdAttributePosition];
		}

		private SimpleNaturalIdLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private SimpleNaturalIdLoadAccessImpl(Class entityClass) {
			this( locateEntityPersister( entityClass ) );
		}

		@Override
		public final SimpleNaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
			return (SimpleNaturalIdLoadAccessImpl<T>) super.with( lockOptions );
		}

		private Map<String, Object> getNaturalIdParameters(Object naturalIdValue) {
			return Collections.singletonMap( naturalIdAttributeName, naturalIdValue );
		}

		@Override
		public SimpleNaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
			super.synchronizationEnabled( synchronizationEnabled );
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T getReference(Object naturalIdValue) {
			final Serializable entityId = resolveNaturalId( getNaturalIdParameters( naturalIdValue ) );
			if ( entityId == null ) {
				return null;
			}
			return (T) this.getIdentifierLoadAccess().getReference( entityId );
		}

		@Override
		@SuppressWarnings("unchecked")
		public T load(Object naturalIdValue) {
			final Serializable entityId = resolveNaturalId( getNaturalIdParameters( naturalIdValue ) );
			if ( entityId == null ) {
				return null;
			}
			try {
				return (T) this.getIdentifierLoadAccess().load( entityId );
			}
			catch (EntityNotFoundException | ObjectNotFoundException e) {
				// OK
			}
			return null;
		}

		@Override
		public Optional<T> loadOptional(Serializable naturalIdValue) {
			return Optional.ofNullable( load( naturalIdValue ) );
		}
	}

	@Override
	public void afterTransactionBegin() {
		checkOpenOrWaitingForAutoClose();
		getInterceptor().afterTransactionBegin( getCurrentTransaction() );
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		final boolean doFlush = isTransactionFlushable()
				&& getHibernateFlushMode() != FlushMode.MANUAL;

		try {
			if ( doFlush ) {
				managedFlush();
			}
		}
		catch (RuntimeException re) {
			throw exceptionMapper.mapManagedFlushFailure( "error during managed flush", re, this );
		}
	}

	private boolean isTransactionFlushable() {
		if ( getCurrentTransaction() == null ) {
			// assume it is flushable - CMT, auto-commit, etc
			return true;
		}
		final TransactionStatus status = getCurrentTransaction().getStatus();
		return status == TransactionStatus.ACTIVE || status == TransactionStatus.COMMITTING;
	}

	@Override
	public boolean isFlushBeforeCompletionEnabled() {
		return getHibernateFlushMode() != FlushMode.MANUAL;
	}

	private static final AfterCompletionAction STANDARD_AFTER_COMPLETION_ACTION = (AfterCompletionAction) (successful, session) -> {
		// nothing to do by default.
	};


	public static class ManagedFlushCheckerStandardImpl implements ManagedFlushChecker {
		@Override
		public boolean shouldDoManagedFlush(SessionImplementor session) {
			if ( session.isClosed() ) {
				return false;
			}
			return session.getHibernateFlushMode() != FlushMode.MANUAL;
		}
	}

	private static final ManagedFlushCheckerStandardImpl STANDARD_MANAGED_FLUSH_CHECKER = new ManagedFlushCheckerStandardImpl() {
	};


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HibernateEntityManager impl

	@Override
	public SessionImplementor getSession() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HibernateEntityManagerImplementor impl


	@Override
	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties) {
		LockOptions lockOptions = new LockOptions();
		LockOptions.copy( this.lockOptions, lockOptions );
		lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
		if ( properties != null ) {
			setLockOptions( properties, lockOptions );
		}
		return lockOptions;
	}

	private void setLockOptions(Map<String, Object> props, LockOptions options) {
		Object lockScope = props.get( JPA_LOCK_SCOPE );
		if ( lockScope instanceof String && PessimisticLockScope.valueOf( ( String ) lockScope ) == PessimisticLockScope.EXTENDED ) {
			options.setScope( true );
		}
		else if ( lockScope instanceof PessimisticLockScope ) {
			boolean extended = PessimisticLockScope.EXTENDED.equals( lockScope );
			options.setScope( extended );
		}
		else if ( lockScope != null ) {
			throw new PersistenceException( "Unable to parse " + JPA_LOCK_SCOPE + ": " + lockScope );
		}

		Object lockTimeout = props.get( JPA_LOCK_TIMEOUT );
		int timeout = 0;
		boolean timeoutSet = false;
		if ( lockTimeout instanceof String ) {
			timeout = Integer.parseInt( ( String ) lockTimeout );
			timeoutSet = true;
		}
		else if ( lockTimeout instanceof Number ) {
			timeout = ( (Number) lockTimeout ).intValue();
			timeoutSet = true;
		}
		else if ( lockTimeout != null ) {
			throw new PersistenceException( "Unable to parse " + JPA_LOCK_TIMEOUT + ": " + lockTimeout );
		}

		if ( timeoutSet ) {
			if ( timeout == LockOptions.SKIP_LOCKED ) {
				options.setTimeOut( LockOptions.SKIP_LOCKED );
			}
			else if ( timeout < 0 ) {
				options.setTimeOut( LockOptions.WAIT_FOREVER );
			}
			else if ( timeout == 0 ) {
				options.setTimeOut( LockOptions.NO_WAIT );
			}
			else {
				options.setTimeOut( timeout );
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			QueryOptions queryOptions) {
		try {
			final QueryImplementor query = createQuery( jpaqlString );

			if ( queryOptions.getValueHandlers() == null ) {
				if ( queryOptions.getResultMetadataValidator() != null ) {
					queryOptions.getResultMetadataValidator().validate( query.getReturnTypes() );
				}
			}

			// determine if we need a result transformer
			List tupleElements = Tuple.class.equals( resultClass )
					? ( (CompoundSelectionImpl<Tuple>) selection ).getCompoundSelectionItems()
					: null;
			if ( queryOptions.getValueHandlers() != null || tupleElements != null ) {
				query.setResultTransformer(
						new CriteriaQueryTupleTransformer( queryOptions.getValueHandlers(), tupleElements )
				);
			}

			return query;
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityManager impl

	@Override
	public void remove(Object entity) {
		checkOpen();

		try {
			delete( entity );
		}
		catch (MappingException e) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw exceptionConverter.convert( e );
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
			if ( properties != null && !properties.isEmpty() ) {
				getLoadQueryInfluencers().setFetchGraph( (EntityGraph) properties.get( QueryHints.HINT_FETCHGRAPH ) );
				getLoadQueryInfluencers().setLoadGraph( (EntityGraph) properties.get( QueryHints.HINT_LOADGRAPH ) );
			}

			final IdentifierLoadAccess<T> loadAccess = byId( entityClass );
			loadAccess.with( determineAppropriateLocalCacheMode( properties ) );

			if ( lockModeType != null ) {
				if ( !LockModeType.NONE.equals( lockModeType) ) {
					checkTransactionNeeded();
				}
				lockOptions = buildLockOptions( lockModeType, properties );
				loadAccess.with( lockOptions );
			}

			return loadAccess.load( (Serializable) primaryKey );
		}
		catch ( EntityNotFoundException ignored ) {
			// DefaultLoadEventListener.returnNarrowedProxy may throw ENFE (see HHH-7861 for details),
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
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e, lockOptions );
		}
		finally {
			getLoadQueryInfluencers().setFetchGraph( null );
			getLoadQueryInfluencers().setLoadGraph( null );
		}
	}

	private CacheMode determineAppropriateLocalCacheMode(Map<String, Object> localProperties) {
		CacheRetrieveMode retrieveMode = null;
		CacheStoreMode storeMode = null;
		if ( localProperties != null ) {
			retrieveMode = determineCacheRetrieveMode( localProperties );
			storeMode = determineCacheStoreMode( localProperties );
		}
		if ( retrieveMode == null ) {
			// use the EM setting
			retrieveMode = determineCacheRetrieveMode( this.properties );
		}
		if ( storeMode == null ) {
			// use the EM setting
			storeMode = determineCacheStoreMode( this.properties );
		}
		return CacheModeHelper.interpretCacheMode( storeMode, retrieveMode );
	}

	private CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		return ( CacheRetrieveMode ) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
	}

	private CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		return ( CacheStoreMode ) settings.get( JPA_SHARED_CACHE_STORE_MODE );
	}

	private void checkTransactionNeeded() {
		if ( disallowOutOfTransactionUpdateOperations && !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "no transaction is in progress" );
		}
	}

	@Override
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		checkOpen();

		try {
			return byId( entityClass ).getReference( (Serializable) primaryKey );
		}
		catch ( MappingException | TypeMismatchException | ClassCastException e ) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType) {
		lock( entity, lockModeType, null );
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		checkTransactionNeeded();

		if ( !contains( entity ) ) {
			throw new IllegalArgumentException( "entity not in the persistence context" );
		}

		final LockOptions lockOptions = buildLockOptions( lockModeType, properties );
		try {
			buildLockRequest( lockOptions ).lock( entity );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e, lockOptions );
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
				throw exceptionConverter.convert( new IllegalArgumentException( "Entity not managed" ) );
			}

			if ( lockModeType != null ) {
				if ( !LockModeType.NONE.equals( lockModeType) ) {
					checkTransactionNeeded();
				}

				lockOptions = buildLockOptions( lockModeType, properties );
				refresh( entity, lockOptions );
			}
			else {
				refresh( entity );
			}
		}
		catch (MappingException e) {
			throw exceptionConverter.convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e, lockOptions );
		}
		finally {
			setCacheMode( previousCacheMode );
		}
	}

	@Override
	public void detach(Object entity) {
		checkOpen();
		try {
			evict( entity );
		}
		catch (RuntimeException e) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		checkOpen();

		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "Call to EntityManager#getLockMode should occur within transaction according to spec" );
		}

		if ( !contains( entity ) ) {
			throw exceptionConverter.convert( new IllegalArgumentException( "entity not in the persistence context" ) );
		}

		return LockModeTypeHelper.getLockModeType( getCurrentLockMode( entity ) );

	}

	@Override
	public void setProperty(String propertyName, Object value) {
		checkOpen();

		if ( ENTITY_MANAGER_SPECIFIC_PROPERTIES.contains( propertyName ) ) {
			properties.put( propertyName, value );
			applyProperties();
		}
		else {
			log.debugf( "Trying to set a property which is not supported on entity manager level" );
		}
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap( properties );
	}

	private CriteriaCompiler criteriaCompiler;

	@SuppressWarnings("WeakerAccess")
	protected CriteriaCompiler criteriaCompiler() {
		if ( criteriaCompiler == null ) {
			criteriaCompiler = new CriteriaCompiler( this );
		}
		return criteriaCompiler;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryImplementor<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		try {
			return (QueryImplementor<T>) criteriaCompiler().compile( (CompilableCriteria) criteriaQuery );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public QueryImplementor createQuery(CriteriaUpdate criteriaUpdate) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaUpdate );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public QueryImplementor createQuery(CriteriaDelete criteriaDelete) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaDelete );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	protected void initQueryFromNamedDefinition(Query query, NamedQueryDefinition namedQueryDefinition) {
		super.initQueryFromNamedDefinition( query, namedQueryDefinition );

		if ( namedQueryDefinition.isCacheable() ) {
			query.setHint( QueryHints.HINT_CACHEABLE, true );
			if ( namedQueryDefinition.getCacheRegion() != null ) {
				query.setHint( QueryHints.HINT_CACHE_REGION, namedQueryDefinition.getCacheRegion() );
			}
		}

		if ( namedQueryDefinition.getCacheMode() != null ) {
			query.setHint( QueryHints.HINT_CACHE_MODE, namedQueryDefinition.getCacheMode() );
		}

		if ( namedQueryDefinition.isReadOnly() ) {
			query.setHint( QueryHints.HINT_READONLY, true );
		}

		if ( namedQueryDefinition.getTimeout() != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, namedQueryDefinition.getTimeout() * 1000 );
		}

		if ( namedQueryDefinition.getFetchSize() != null ) {
			query.setHint( QueryHints.HINT_FETCH_SIZE, namedQueryDefinition.getFetchSize() );
		}

		if ( namedQueryDefinition.getComment() != null ) {
			query.setHint( QueryHints.HINT_COMMENT, namedQueryDefinition.getComment() );
		}

		if ( namedQueryDefinition.getFirstResult() != null ) {
			query.setFirstResult( namedQueryDefinition.getFirstResult() );
		}

		if ( namedQueryDefinition.getMaxResults() != null ) {
			query.setMaxResults( namedQueryDefinition.getMaxResults() );
		}

		if ( namedQueryDefinition.getLockOptions() != null ) {
			if ( namedQueryDefinition.getLockOptions().getLockMode() != null ) {
				query.setLockMode(
						LockModeTypeHelper.getLockModeType( namedQueryDefinition.getLockOptions().getLockMode() )
				);
			}
		}

		if ( namedQueryDefinition.getFlushMode() != null ) {
			if ( namedQueryDefinition.getFlushMode() == FlushMode.COMMIT ) {
				query.setFlushMode( FlushModeType.COMMIT );
			}
			else {
				query.setFlushMode( FlushModeType.AUTO );
			}
		}
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		checkOpen();
		try {
			final ProcedureCallMemento memento = getFactory().getNamedQueryRepository().getNamedProcedureCallMemento( name );
			if ( memento == null ) {
				throw new IllegalArgumentException( "No @NamedStoredProcedureQuery was found with that name : " + name );
			}
			return memento.makeProcedureCall( this );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		try {
			return createStoredProcedureCall( procedureName );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		try {
			return createStoredProcedureCall( procedureName, resultClasses );
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		try {
			try {
				return createStoredProcedureCall( procedureName, resultSetMappings );
			}
			catch (UnknownSqlResultSetMappingException unknownResultSetMapping) {
				throw new IllegalArgumentException( unknownResultSetMapping.getMessage(), unknownResultSetMapping );
			}
		}
		catch ( RuntimeException e ) {
			throw exceptionConverter.convert( e );
		}
	}

	@Override
	public void joinTransaction() {
		checkOpen();
		joinTransaction( true );
	}

	private void joinTransaction(boolean explicitRequest) {
		if ( !getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
			if ( explicitRequest ) {
				log.callingJoinTransactionOnNonJtaEntityManager();
			}
			return;
		}

		try {
			getTransactionCoordinator().explicitJoin();
		}
		catch (TransactionRequiredForJoinException e) {
			throw new TransactionRequiredException( e.getMessage() );
		}
		catch (HibernateException he) {
			throw exceptionConverter.convert( he );
		}
	}

	@Override
	public boolean isJoinedToTransaction() {
		checkOpen();
		return getTransactionCoordinator().isJoined();
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

		throw new PersistenceException( "Hibernate cannot unwrap " + clazz );
	}

	@Override
	public Object getDelegate() {
		return this;
	}

	@Override
	public SessionFactoryImplementor getEntityManagerFactory() {
		return getFactory();
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return getFactory().getCriteriaBuilder();
	}

	@Override
	public MetamodelImplementor getMetamodel() {
		return getFactory().getMetamodel();
	}

	@Override
	public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
		checkOpen();
		return new EntityGraphImpl<T>( null, getMetamodel().entity( rootType ), getEntityManagerFactory() );
	}

	@Override
	public EntityGraph<?> createEntityGraph(String graphName) {
		checkOpen();
		final EntityGraph named = getEntityManagerFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			return null;
		}

		if ( EntityGraphImplementor.class.isInstance( named ) ) {
			return ( (EntityGraphImplementor) named ).makeMutableCopy();
		}
		else {
			return named;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityGraph<?> getEntityGraph(String graphName) {
		checkOpen();
		final EntityGraph named = getEntityManagerFactory().findEntityGraphByName( graphName );
		if ( named == null ) {
			throw new IllegalArgumentException( "Could not locate EntityGraph with given name : " + graphName );
		}
		return named;
	}

	@Override
	public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
		checkOpen();
		return getEntityManagerFactory().findEntityGraphsByType( entityClass );
	}


	/**
	 * Used by JDK serialization...
	 *
	 * @param oos The output stream to which we are being written...
	 *
	 * @throws IOException Indicates a general IO stream exception
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		log.tracef( "Serializing Session [%s]", getSessionIdentifier() );

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
		log.tracef( "Deserializing Session [%s]", getSessionIdentifier() );

		ois.defaultReadObject();

		persistenceContext = StatefulPersistenceContext.deserialize( ois, this );
		actionQueue = ActionQueue.deserialize( ois, this );

		loadQueryInfluencers = (LoadQueryInfluencers) ois.readObject();

		// LoadQueryInfluencers.getEnabledFilters() tries to validate each enabled
		// filter, which will fail when called beforeQuery FilterImpl.afterDeserialize( factory );
		// Instead lookup the filter by name and then call FilterImpl.afterDeserialize( factory ).
		for ( String filterName : loadQueryInfluencers.getEnabledFilterNames() ) {
			( (FilterImpl) loadQueryInfluencers.getEnabledFilter( filterName ) ).afterDeserialize( getFactory() );
		}
	}
}
