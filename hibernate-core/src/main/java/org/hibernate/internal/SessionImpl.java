/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2005-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Criteria;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityNameResolver;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.Interceptor;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionException;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.engine.internal.StatefulPersistenceContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.query.spi.FilterQueryPlan;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.NonFlushedChanges;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.transaction.internal.TransactionCoordinatorImpl;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
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
import org.hibernate.internal.CriteriaImpl.CriterionEntry;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.internal.SessionStatisticsImpl;
import org.hibernate.type.SerializationException;
import org.hibernate.type.Type;

/**
 * Concrete implementation of a Session.
 *
 * Exposes two interfaces:<ul>
 *     <li>{@link Session} to the application</li>
 *     <li>{@link org.hibernate.engine.spi.SessionImplementor} to other Hibernate components (SPI)</li>
 * </ul>
 *
 * This class is not thread-safe.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class SessionImpl extends AbstractSessionImpl implements EventSource {

	// todo : need to find a clean way to handle the "event source" role
	// a separate class responsible for generating/dispatching events just duplicates most of the Session methods...
	// passing around separate interceptor, factory, actionQueue, and persistentContext is not manageable...

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SessionImpl.class.getName());

   private static final boolean tracing = LOG.isTraceEnabled();

	private transient long timestamp;

	private transient SessionOwner sessionOwner;

	private transient ActionQueue actionQueue;
	private transient StatefulPersistenceContext persistenceContext;
	private transient TransactionCoordinatorImpl transactionCoordinator;
	private transient Interceptor interceptor;
	private transient EntityNameResolver entityNameResolver = new CoordinatingEntityNameResolver();

	private transient ConnectionReleaseMode connectionReleaseMode;
	private transient FlushMode flushMode = FlushMode.AUTO;
	private transient CacheMode cacheMode = CacheMode.NORMAL;

	private transient boolean autoClear; //for EJB3
	private transient boolean autoJoinTransactions = true;
	private transient boolean flushBeforeCompletionEnabled;
	private transient boolean autoCloseSessionEnabled;

	private transient int dontFlushFromFind = 0;

	private transient LoadQueryInfluencers loadQueryInfluencers;

	private final transient boolean isTransactionCoordinatorShared;
	private transient TransactionObserver transactionObserver;

	/**
	 * Constructor used for openSession(...) processing, as well as construction
	 * of sessions for getCurrentSession().
	 *
	 * @param connection The user-supplied connection to use for this session.
	 * @param factory The factory from which this session was obtained
	 * @param transactionCoordinator The transaction coordinator to use, may be null to indicate that a new transaction
	 * coordinator should get created.
	 * @param autoJoinTransactions Should the session automatically join JTA transactions?
	 * @param timestamp The timestamp for this session
	 * @param interceptor The interceptor to be applied to this session
	 * @param flushBeforeCompletionEnabled Should we auto flush before completion of transaction
	 * @param autoCloseSessionEnabled Should we auto close after completion of transaction
	 * @param connectionReleaseMode The mode by which we should release JDBC connections.
	 * @param tenantIdentifier The tenant identifier to use.  May be null
	 */
	SessionImpl(
			final Connection connection,
			final SessionFactoryImpl factory,
			final SessionOwner sessionOwner,
			final TransactionCoordinatorImpl transactionCoordinator,
			final boolean autoJoinTransactions,
			final long timestamp,
			final Interceptor interceptor,
			final boolean flushBeforeCompletionEnabled,
			final boolean autoCloseSessionEnabled,
			final ConnectionReleaseMode connectionReleaseMode,
			final String tenantIdentifier) {
		super( factory, tenantIdentifier );
		this.timestamp = timestamp;
		this.sessionOwner = sessionOwner;
		this.interceptor = interceptor == null ? EmptyInterceptor.INSTANCE : interceptor;
		this.actionQueue = new ActionQueue( this );
		this.persistenceContext = new StatefulPersistenceContext( this );

		this.autoCloseSessionEnabled = autoCloseSessionEnabled;
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;

		if ( transactionCoordinator == null ) {
			this.isTransactionCoordinatorShared = false;
			this.connectionReleaseMode = connectionReleaseMode;
			this.autoJoinTransactions = autoJoinTransactions;

			this.transactionCoordinator = new TransactionCoordinatorImpl( connection, this );
			this.transactionCoordinator.getJdbcCoordinator().getLogicalConnection().addObserver(
					new ConnectionObserverStatsBridge( factory )
			);
		}
		else {
			if ( connection != null ) {
				throw new SessionException( "Cannot simultaneously share transaction context and specify connection" );
			}
			this.transactionCoordinator = transactionCoordinator;
			this.isTransactionCoordinatorShared = true;
			this.autoJoinTransactions = false;
			if ( autoJoinTransactions ) {
				LOG.debug(
						"Session creation specified 'autoJoinTransactions', which is invalid in conjunction " +
								"with sharing JDBC connection between sessions; ignoring"
				);
			}
			if ( connectionReleaseMode != transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getConnectionReleaseMode() ) {
				LOG.debug(
						"Session creation specified 'connectionReleaseMode', which is invalid in conjunction " +
								"with sharing JDBC connection between sessions; ignoring"
				);
			}
			this.connectionReleaseMode = transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getConnectionReleaseMode();

			// add a transaction observer so that we can handle delegating managed actions back to THIS session
			// versus the session that created (and therefore "owns") the transaction coordinator
			transactionObserver = new TransactionObserver() {
				@Override
				public void afterBegin(TransactionImplementor transaction) {
				}

				@Override
				public void beforeCompletion(TransactionImplementor transaction) {
					if ( isOpen() && flushBeforeCompletionEnabled ) {
						SessionImpl.this.managedFlush();
					}
					beforeTransactionCompletion( transaction );
				}

				@Override
				public void afterCompletion(boolean successful, TransactionImplementor transaction) {
					afterTransactionCompletion( transaction, successful );
					if ( isOpen() && autoCloseSessionEnabled ) {
						managedClose();
					}
					transactionCoordinator.removeObserver( this );
				}
			};

			transactionCoordinator.addObserver( transactionObserver );
		}

		loadQueryInfluencers = new LoadQueryInfluencers( factory );

		if (factory.getStatistics().isStatisticsEnabled()) {
			factory.getStatisticsImplementor().openSession();
		}

      if (tracing)
		   LOG.tracef( "Opened session at timestamp: %s", timestamp );
	}

	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new SharedSessionBuilderImpl( this );
	}

	public void clear() {
		errorIfClosed();
		checkTransactionSynchStatus();
		internalClear();
	}

	private void internalClear() {
		persistenceContext.clear();
		actionQueue.clear();
	}

	public long getTimestamp() {
		checkTransactionSynchStatus();
		return timestamp;
	}

	public Connection close() throws HibernateException {
		LOG.trace( "Closing session" );
		if ( isClosed() ) {
			throw new SessionException( "Session was already closed" );
		}


		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().closeSession();
		}

		try {
			if ( !isTransactionCoordinatorShared ) {
				return transactionCoordinator.close();
			}
			else {
				if ( getActionQueue().hasAfterTransactionActions() ){
					LOG.warn( "On close, shared Session had after transaction actions that have not yet been processed" );
				}
				else {
					transactionCoordinator.removeObserver( transactionObserver );
				}
				return null;
			}
		}
		finally {
			setClosed();
			cleanup();
		}
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return connectionReleaseMode;
	}

	@Override
	public boolean shouldAutoJoinTransaction() {
		return autoJoinTransactions;
	}

	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}

	public boolean isOpen() {
		checkTransactionSynchStatus();
		return !isClosed();
	}

	public boolean isFlushModeNever() {
		return FlushMode.isManualFlushMode( getFlushMode() );
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}

	public void managedFlush() {
		if ( isClosed() ) {
			LOG.trace( "Skipping auto-flush due to session closed" );
			return;
		}
		LOG.trace( "Automatically flushing session" );
		flush();
	}

	/**
	 * Return changes to this session and its child sessions that have not been flushed yet.
	 * <p/>
	 * @return The non-flushed changes.
	 */
	public NonFlushedChanges getNonFlushedChanges() throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new NonFlushedChangesImpl( this );
	}

	/**
	 * Apply non-flushed changes from a different session to this session. It is assumed
	 * that this SessionImpl is "clean" (e.g., has no non-flushed changes, no cached entities,
	 * no cached collections, no queued actions). The specified NonFlushedChanges object cannot
	 * be bound to any session.
	 * <p/>
	 * @param nonFlushedChanges the non-flushed changes
	 */
	public void applyNonFlushedChanges(NonFlushedChanges nonFlushedChanges) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		// todo : why aren't these just part of the NonFlushedChanges API ?
		replacePersistenceContext( ((NonFlushedChangesImpl) nonFlushedChanges).getPersistenceContext() );
		replaceActionQueue( ((NonFlushedChangesImpl) nonFlushedChanges).getActionQueue() );
	}

	private void replacePersistenceContext(StatefulPersistenceContext persistenceContextNew) {
		if ( persistenceContextNew.getSession() != null ) {
			throw new IllegalStateException( "new persistence context is already connected to a session " );
		}
		persistenceContext.clear();
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream( new ByteArrayInputStream( serializePersistenceContext( persistenceContextNew ) ) );
			this.persistenceContext = StatefulPersistenceContext.deserialize( ois, this );
		}
		catch (IOException ex) {
			throw new SerializationException( "could not deserialize the persistence context",  ex );
		}
		catch (ClassNotFoundException ex) {
			throw new SerializationException( "could not deserialize the persistence context", ex );
		}
		finally {
			try {
				if (ois != null) ois.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	private static byte[] serializePersistenceContext(StatefulPersistenceContext pc) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream( 512 );
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream( baos );
			( pc ).serialize( oos );
		}
		catch (IOException ex) {
			throw new SerializationException( "could not serialize persistence context", ex );
		}
		finally {
			if ( oos != null ) {
				try {
					oos.close();
				}
				catch( IOException ignore ) {
					//ignore
				}
			}
		}
		return baos.toByteArray();
	}

	private void replaceActionQueue(ActionQueue actionQueueNew) {
		if ( actionQueue.hasAnyQueuedActions() ) {
			throw new IllegalStateException( "cannot replace an ActionQueue with queued actions " );
		}
		actionQueue.clear();
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream( new ByteArrayInputStream( serializeActionQueue( actionQueueNew ) ) );
			actionQueue = ActionQueue.deserialize( ois, this );
		}
		catch (IOException ex) {
			throw new SerializationException( "could not deserialize the action queue",  ex );
		}
		catch (ClassNotFoundException ex) {
			throw new SerializationException( "could not deserialize the action queue", ex );
		}
		finally {
			try {
				if (ois != null) ois.close();
			}
			catch (IOException ignore) {
			}
		}
	}

	private static byte[] serializeActionQueue(ActionQueue actionQueue) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream( 512 );
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream( baos );
			actionQueue.serialize( oos );
		}
		catch (IOException ex) {
			throw new SerializationException( "could not serialize action queue", ex );
		}
		finally {
			if ( oos != null ) {
				try {
					oos.close();
				}
				catch( IOException ex ) {
					//ignore
				}
			}
		}
		return baos.toByteArray();
	}

	@Override
	public boolean shouldAutoClose() {
		if ( isClosed() ) {
			return false;
		}
		else if ( sessionOwner != null ) {
			return sessionOwner.shouldAutoCloseSession();
		}
		else {
			return isAutoCloseSessionEnabled();
		}
	}

	public void managedClose() {
		LOG.trace( "Automatically closing session" );
		close();
	}

	public Connection connection() throws HibernateException {
		errorIfClosed();
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().getDistinctConnectionProxy();
	}

	public boolean isConnected() {
		checkTransactionSynchStatus();
		return !isClosed() && transactionCoordinator.getJdbcCoordinator().getLogicalConnection().isOpen();
	}

	public boolean isTransactionInProgress() {
		checkTransactionSynchStatus();
		return !isClosed() && transactionCoordinator.isTransactionInProgress();
	}

	@Override
	public Connection disconnect() throws HibernateException {
		errorIfClosed();
		LOG.debug( "Disconnecting session" );
		return transactionCoordinator.getJdbcCoordinator().getLogicalConnection().manualDisconnect();
	}

	@Override
	public void reconnect(Connection conn) throws HibernateException {
		errorIfClosed();
		LOG.debug( "Reconnecting session" );
		checkTransactionSynchStatus();
		transactionCoordinator.getJdbcCoordinator().getLogicalConnection().manualReconnect( conn );
	}

	public void setAutoClear(boolean enabled) {
		errorIfClosed();
		autoClear = enabled;
	}

	@Override
	public void disableTransactionAutoJoin() {
		errorIfClosed();
		autoJoinTransactions = false;
	}

	/**
	 * Check if there is a Hibernate or JTA transaction in progress and,
	 * if there is not, flush if necessary, make sure the connection has
	 * been committed (if it is not in autocommit mode) and run the after
	 * completion processing
	 *
	 * @param success Was the operation a success
	 */
	public void afterOperation(boolean success) {
		if ( ! transactionCoordinator.isTransactionInProgress() ) {
			transactionCoordinator.afterNonTransactionalQuery( success );
		}
	}

	@Override
	public void afterTransactionBegin(TransactionImplementor hibernateTransaction) {
		errorIfClosed();
		interceptor.afterTransactionBegin( hibernateTransaction );
	}

	@Override
	public void beforeTransactionCompletion(TransactionImplementor hibernateTransaction) {
		LOG.trace( "before transaction completion" );
		actionQueue.beforeTransactionCompletion();
		try {
			interceptor.beforeTransactionCompletion( hibernateTransaction );
		}
		catch (Throwable t) {
			LOG.exceptionInBeforeTransactionCompletionInterceptor( t );
		}
	}

	@Override
	public void afterTransactionCompletion(TransactionImplementor hibernateTransaction, boolean successful) {
		LOG.trace( "after transaction completion" );
		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion( successful );
		if ( hibernateTransaction != null ) {
			try {
				interceptor.afterTransactionCompletion( hibernateTransaction );
			}
			catch (Throwable t) {
				LOG.exceptionInAfterTransactionCompletionInterceptor( t );
			}
		}
		if ( autoClear ) {
			internalClear();
		}
	}

	@Override
	public String onPrepareStatement(String sql) {
		errorIfClosed();
		sql = interceptor.onPrepareStatement( sql );
		if ( sql == null || sql.length() == 0 ) {
			throw new AssertionFailure( "Interceptor.onPrepareStatement() returned null or empty string." );
		}
		return sql;
	}

	/**
	 * clear all the internal collections, just
	 * to help the garbage collector, does not
	 * clear anything that is needed during the
	 * afterTransactionCompletion() phase
	 */
	private void cleanup() {
		persistenceContext.clear();
	}

	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		if ( object == null ) {
			throw new NullPointerException( "null object passed to getCurrentLockMode()" );
		}
		if ( object instanceof HibernateProxy ) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation(this);
			if ( object == null ) {
				return LockMode.NONE;
			}
		}
		EntityEntry e = persistenceContext.getEntry(object);
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

	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		errorIfClosed();
		// todo : should this get moved to PersistentContext?
		// logically, is PersistentContext the "thing" to which an interceptor gets attached?
		final Object result = persistenceContext.getEntity(key);
		if ( result == null ) {
			final Object newObject = interceptor.getEntity( key.getEntityName(), key.getIdentifier() );
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
			throw new IllegalStateException( "There are delayed insert actions before operation as cascade level 0." );
		}
	}

	private void checkNoUnresolvedActionsAfterOperation() {
		if ( persistenceContext.getCascadeLevel() == 0 ) {
			actionQueue.checkNoUnresolvedActionsAfterOperation();
		}
	}

	// saveOrUpdate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void saveOrUpdate(Object object) throws HibernateException {
		saveOrUpdate( null, object );
	}

	public void saveOrUpdate(String entityName, Object obj) throws HibernateException {
		fireSaveOrUpdate( new SaveOrUpdateEvent( entityName, obj, this ) );
	}

	private void fireSaveOrUpdate(SaveOrUpdateEvent event) {
		errorIfClosed();
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
		return factory.getServiceRegistry().getService( EventListenerRegistry.class ).getEventListenerGroup( type );
	}


	// save() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Serializable save(Object obj) throws HibernateException {
		return save( null, obj );
	}

	public Serializable save(String entityName, Object object) throws HibernateException {
		return fireSave( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private Serializable fireSave(SaveOrUpdateEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.SAVE ) ) {
			listener.onSaveOrUpdate( event );
		}
		checkNoUnresolvedActionsAfterOperation();
		return event.getResultId();
	}


	// update() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void update(Object obj) throws HibernateException {
		update(null, obj);
	}

	public void update(String entityName, Object object) throws HibernateException {
		fireUpdate( new SaveOrUpdateEvent( entityName, object, this ) );
	}

	private void fireUpdate(SaveOrUpdateEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( SaveOrUpdateEventListener listener : listeners( EventType.UPDATE ) ) {
			listener.onSaveOrUpdate( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}


	// lock() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		fireLock( new LockEvent( entityName, object, lockMode, this ) );
	}

	public LockRequest buildLockRequest(LockOptions lockOptions) {
		return new LockRequestImpl(lockOptions);
	}

	public void lock(Object object, LockMode lockMode) throws HibernateException {
		fireLock( new LockEvent(object, lockMode, this) );
	}

	private void fireLock(String entityName, Object object, LockOptions options) {
		fireLock( new LockEvent( entityName, object, options, this) );
	}

	private void fireLock( Object object, LockOptions options) {
		fireLock( new LockEvent( object, options, this ) );
	}

	private void fireLock(LockEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( LockEventListener listener : listeners( EventType.LOCK ) ) {
			listener.onLock( event );
		}
	}


	// persist() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void persist(String entityName, Object object) throws HibernateException {
		firePersist( new PersistEvent( entityName, object, this ) );
	}

	public void persist(Object object) throws HibernateException {
		persist( null, object );
	}

	public void persist(String entityName, Object object, Map copiedAlready)
	throws HibernateException {
		firePersist( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersist(Map copiedAlready, PersistEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( PersistEventListener listener : listeners( EventType.PERSIST ) ) {
			listener.onPersist( event, copiedAlready );
		}
	}

	private void firePersist(PersistEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( PersistEventListener listener : listeners( EventType.PERSIST ) ) {
			listener.onPersist( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}


	// persistOnFlush() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void persistOnFlush(String entityName, Object object)
			throws HibernateException {
		firePersistOnFlush( new PersistEvent( entityName, object, this ) );
	}

	public void persistOnFlush(Object object) throws HibernateException {
		persist( null, object );
	}

	public void persistOnFlush(String entityName, Object object, Map copiedAlready)
			throws HibernateException {
		firePersistOnFlush( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersistOnFlush(Map copiedAlready, PersistEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( PersistEventListener listener : listeners( EventType.PERSIST_ONFLUSH ) ) {
			listener.onPersist( event, copiedAlready );
		}
	}

	private void firePersistOnFlush(PersistEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( PersistEventListener listener : listeners( EventType.PERSIST_ONFLUSH ) ) {
			listener.onPersist( event );
		}
		checkNoUnresolvedActionsAfterOperation();
	}


	// merge() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Object merge(String entityName, Object object) throws HibernateException {
		return fireMerge( new MergeEvent( entityName, object, this ) );
	}

	public Object merge(Object object) throws HibernateException {
		return merge( null, object );
	}

	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException {
		fireMerge( copiedAlready, new MergeEvent( entityName, object, this ) );
	}

	private Object fireMerge(MergeEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		checkNoUnresolvedActionsBeforeOperation();
		for ( MergeEventListener listener : listeners( EventType.MERGE ) ) {
			listener.onMerge( event );
		}
		checkNoUnresolvedActionsAfterOperation();
		return event.getResult();
	}

	private void fireMerge(Map copiedAlready, MergeEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( MergeEventListener listener : listeners( EventType.MERGE ) ) {
			listener.onMerge( event, copiedAlready );
		}
	}


	// delete() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Delete a persistent object
	 */
	public void delete(Object object) throws HibernateException {
		fireDelete( new DeleteEvent( object, this ) );
	}

	/**
	 * Delete a persistent object (by explicit entity name)
	 */
	public void delete(String entityName, Object object) throws HibernateException {
		fireDelete( new DeleteEvent( entityName, object, this ) );
	}

	/**
	 * Delete a persistent object
	 */
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, Set transientEntities) throws HibernateException {
		fireDelete( new DeleteEvent( entityName, object, isCascadeDeleteEnabled, this ), transientEntities );
	}

	private void fireDelete(DeleteEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( DeleteEventListener listener : listeners( EventType.DELETE ) ) {
			listener.onDelete( event );
		}
	}

	private void fireDelete(DeleteEvent event, Set transientEntities) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( DeleteEventListener listener : listeners( EventType.DELETE ) ) {
			listener.onDelete( event, transientEntities );
		}
	}


	// load()/get() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void load(Object object, Serializable id) throws HibernateException {
		LoadEvent event = new LoadEvent(id, object, this);
		fireLoad( event, LoadEventListener.RELOAD );
	}

	public Object load(Class entityClass, Serializable id) throws HibernateException {
		return this.byId( entityClass ).getReference( id );
	}

	public Object load(String entityName, Serializable id) throws HibernateException {
		return this.byId( entityName ).getReference( id );
	}

	public Object get(Class entityClass, Serializable id) throws HibernateException {
		return this.byId( entityClass ).load( id );
	}

	public Object get(String entityName, Serializable id) throws HibernateException {
		return this.byId( entityName ).load( id );
	}

	/**	
	 * Load the data for the object with the specified id into a newly created object.
	 * This is only called when lazily initializing a proxy.
	 * Do NOT return a proxy.
	 */
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		if ( LOG.isDebugEnabled() ) {
			EntityPersister persister = getFactory().getEntityPersister(entityName);
			LOG.debugf( "Initializing proxy: %s", MessageHelper.infoString( persister, id, getFactory() ) );
		}

		LoadEvent event = new LoadEvent(id, entityName, true, this);
		fireLoad(event, LoadEventListener.IMMEDIATE_LOAD);
		return event.getResult();
	}

	public Object internalLoad(String entityName, Serializable id, boolean eager, boolean nullable) throws HibernateException {
		// todo : remove
		LoadEventListener.LoadType type = nullable
				? LoadEventListener.INTERNAL_LOAD_NULLABLE
				: eager
						? LoadEventListener.INTERNAL_LOAD_EAGER
						: LoadEventListener.INTERNAL_LOAD_LAZY;
		LoadEvent event = new LoadEvent(id, entityName, true, this);
		fireLoad(event, type);
		if ( !nullable ) {
			UnresolvableObjectException.throwIfNull( event.getResult(), id, entityName );
		}
		return event.getResult();
	}

	public Object load(Class entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	public Object load(Class entityClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).getReference( id );
	}

	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).getReference( id );
	}

	public Object load(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).getReference( id );
	}

	public Object get(Class entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).load( id );
	}

	public Object get(Class entityClass, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityClass ).with( lockOptions ).load( id );
	}

	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).load( id );
	}

	public Object get(String entityName, Serializable id, LockOptions lockOptions) throws HibernateException {
		return this.byId( entityName ).with( lockOptions ).load( id );
	}
	
	@Override
	public IdentifierLoadAccessImpl byId(String entityName) {
		return new IdentifierLoadAccessImpl( entityName );
	}

	@Override
	public IdentifierLoadAccessImpl byId(Class entityClass) {
		return new IdentifierLoadAccessImpl( entityClass );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		return new NaturalIdLoadAccessImpl( entityName );
	}

	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		return new NaturalIdLoadAccessImpl( entityClass );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		return new SimpleNaturalIdLoadAccessImpl( entityName );
	}

	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		return new SimpleNaturalIdLoadAccessImpl( entityClass );
	}

	private void fireLoad(LoadEvent event, LoadType loadType) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( LoadEventListener listener : listeners( EventType.LOAD ) ) {
			listener.onLoad( event, loadType );
		}
	}

	private void fireResolveNaturalId(ResolveNaturalIdEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( ResolveNaturalIdEventListener listener : listeners( EventType.RESOLVE_NATURAL_ID ) ) {
			listener.onResolveNaturalId( event );
		}
	}


	// refresh() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void refresh(Object object) throws HibernateException {
		refresh( null, object );
	}

	@Override
	public void refresh(String entityName, Object object) throws HibernateException {
		fireRefresh( new RefreshEvent( entityName, object, this ) );
	}

	public void refresh(Object object, LockMode lockMode) throws HibernateException {
		fireRefresh( new RefreshEvent( object, lockMode, this ) );
	}

	public void refresh(Object object, LockOptions lockOptions) throws HibernateException {
		refresh( null, object, lockOptions );
	}
	@Override
	public void refresh(String entityName, Object object, LockOptions lockOptions) throws HibernateException {
		fireRefresh( new RefreshEvent( entityName, object, lockOptions, this ) );
	}

	public void refresh(Object object, Map refreshedAlready) throws HibernateException {
		fireRefresh( refreshedAlready, new RefreshEvent( object, this ) );
	}

	private void fireRefresh(RefreshEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( RefreshEventListener listener : listeners( EventType.REFRESH ) ) {
			listener.onRefresh( event );
		}
	}

	private void fireRefresh(Map refreshedAlready, RefreshEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( RefreshEventListener listener : listeners( EventType.REFRESH ) ) {
			listener.onRefresh( event, refreshedAlready );
		}
	}


	// replicate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void replicate(Object obj, ReplicationMode replicationMode) throws HibernateException {
		fireReplicate( new ReplicateEvent( obj, replicationMode, this ) );
	}

	public void replicate(String entityName, Object obj, ReplicationMode replicationMode)
	throws HibernateException {
		fireReplicate( new ReplicateEvent( entityName, obj, replicationMode, this ) );
	}

	private void fireReplicate(ReplicateEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( ReplicateEventListener listener : listeners( EventType.REPLICATE ) ) {
			listener.onReplicate( event );
		}
	}


	// evict() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * remove any hard references to the entity that are held by the infrastructure
	 * (references held by application or other persistant instances are okay)
	 */
	public void evict(Object object) throws HibernateException {
		fireEvict( new EvictEvent( object, this ) );
	}

	private void fireEvict(EvictEvent event) {
		errorIfClosed();
		checkTransactionSynchStatus();
		for ( EvictEventListener listener : listeners( EventType.EVICT ) ) {
			listener.onEvict( event );
		}
	}

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 */
	protected boolean autoFlushIfRequired(Set querySpaces) throws HibernateException {
		errorIfClosed();
		if ( ! isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		AutoFlushEvent event = new AutoFlushEvent( querySpaces, this );
		for ( AutoFlushEventListener listener : listeners( EventType.AUTO_FLUSH ) ) {
			listener.onAutoFlush( event );
		}
		return event.isFlushRequired();
	}

	public boolean isDirty() throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		LOG.debug( "Checking session dirtiness" );
		if ( actionQueue.areInsertionsOrDeletionsQueued() ) {
			LOG.debug( "Session dirty (scheduled updates and insertions)" );
			return true;
		}
		DirtyCheckEvent event = new DirtyCheckEvent( this );
		for ( DirtyCheckEventListener listener : listeners( EventType.DIRTY_CHECK ) ) {
			listener.onDirtyCheck( event );
		}
		return event.isDirty();
	}

	public void flush() throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new HibernateException("Flush during cascade is dangerous");
		}
		FlushEvent flushEvent = new FlushEvent( this );
		for ( FlushEventListener listener : listeners( EventType.FLUSH ) ) {
			listener.onFlush( flushEvent );
		}
	}

	public void forceFlush(EntityEntry entityEntry) throws HibernateException {
		errorIfClosed();
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Flushing to force deletion of re-saved object: %s",
					MessageHelper.infoString( entityEntry.getPersister(), entityEntry.getId(), getFactory() ) );
		}

		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ObjectDeletedException(
				"deleted object would be re-saved by cascade (remove deleted object from associations)",
				entityEntry.getId(),
				entityEntry.getPersister().getEntityName()
			);
		}

		flush();
	}

	public List list(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
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
			afterOperation(success);
		}
		return results;
	}

	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		autoFlushIfRequired( plan.getQuerySpaces() );

		boolean success = false;
		int result = 0;
		try {
			result = plan.performExecuteUpdate( queryParameters, this );
			success = true;
		}
		finally {
			afterOperation(success);
		}
		return result;
	}

    public int executeNativeUpdate(NativeSQLQuerySpecification nativeQuerySpecification,
            QueryParameters queryParameters) throws HibernateException {
        errorIfClosed();
        checkTransactionSynchStatus();
        queryParameters.validateParameters();
        NativeSQLQueryPlan plan = getNativeSQLQueryPlan( nativeQuerySpecification );


        autoFlushIfRequired( plan.getCustomQuery().getQuerySpaces() );

        boolean success = false;
        int result = 0;
        try {
            result = plan.performExecuteUpdate(queryParameters, this);
            success = true;
        } finally {
            afterOperation(success);
        }
        return result;
    }

	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		queryParameters.validateParameters();
		HQLQueryPlan plan = getHQLQueryPlan( query, true );
		autoFlushIfRequired( plan.getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return plan.performIterate( queryParameters, this );
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		HQLQueryPlan plan = getHQLQueryPlan( query, false );
		autoFlushIfRequired( plan.getQuerySpaces() );
		dontFlushFromFind++;
		try {
			return plan.performScroll( queryParameters, this );
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public Query createFilter(Object collection, String queryString) {
		errorIfClosed();
		checkTransactionSynchStatus();
		CollectionFilterImpl filter = new CollectionFilterImpl(
				queryString,
		        collection,
		        this,
		        getFilterQueryPlan( collection, queryString, null, false ).getParameterMetadata()
		);
		filter.setComment( queryString );
		return filter;
	}

	public Query getNamedQuery(String queryName) throws MappingException {
		errorIfClosed();
		checkTransactionSynchStatus();
		return super.getNamedQuery( queryName );
	}

	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return instantiate( factory.getEntityPersister( entityName ), id );
	}

	/**
	 * give the interceptor an opportunity to override the default instantiation
	 */
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		Object result = interceptor.instantiate( persister.getEntityName(), persister.getEntityMetamodel().getEntityMode(), id );
		if ( result == null ) {
			result = persister.instantiate( id, this );
		}
		return result;
	}

	public void setFlushMode(FlushMode flushMode) {
		errorIfClosed();
		checkTransactionSynchStatus();
		LOG.tracev( "Setting flush mode to: {0}", flushMode );
		this.flushMode = flushMode;
	}

	public FlushMode getFlushMode() {
		checkTransactionSynchStatus();
		return flushMode;
	}

	public CacheMode getCacheMode() {
		checkTransactionSynchStatus();
		return cacheMode;
	}

	public void setCacheMode(CacheMode cacheMode) {
		errorIfClosed();
		checkTransactionSynchStatus();
		LOG.tracev( "Setting cache mode to: {0}", cacheMode );
		this.cacheMode= cacheMode;
	}

	public Transaction getTransaction() throws HibernateException {
		errorIfClosed();
		return transactionCoordinator.getTransaction();
	}

	public Transaction beginTransaction() throws HibernateException {
		errorIfClosed();
		Transaction result = getTransaction();
		result.begin();
		return result;
	}

	public EntityPersister getEntityPersister(final String entityName, final Object object) {
		errorIfClosed();
		if (entityName==null) {
			return factory.getEntityPersister( guessEntityName( object ) );
		}
		else {
			// try block is a hack around fact that currently tuplizers are not
			// given the opportunity to resolve a subclass entity name.  this
			// allows the (we assume custom) interceptor the ability to
			// influence this decision if we were not able to based on the
			// given entityName
			try {
				return factory.getEntityPersister( entityName ).getSubclassEntityPersister( object, getFactory() );
			}
			catch( HibernateException e ) {
				try {
					return getEntityPersister( null, object );
				}
				catch( HibernateException e2 ) {
					throw e;
				}
			}
		}
	}

	// not for internal use:
	public Serializable getIdentifier(Object object) throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		if ( object instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.getSession() != this ) {
				throw new TransientObjectException( "The proxy was not associated with this session" );
			}
			return li.getIdentifier();
		}
		else {
			EntityEntry entry = persistenceContext.getEntry(object);
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
	public Serializable getContextEntityIdentifier(Object object) {
		errorIfClosed();
		if ( object instanceof HibernateProxy ) {
			return getProxyIdentifier( object );
		}
		else {
			EntityEntry entry = persistenceContext.getEntry(object);
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
		final CollectionPersister roleBeforeFlush = (entry == null) ? null : entry.getLoadedPersister();

		FilterQueryPlan plan = null;
		if ( roleBeforeFlush == null ) {
			// if it was previously unreferenced, we need to flush in order to
			// get its state into the database in order to execute query
			flush();
			entry = persistenceContext.getCollectionEntryOrNull( collection );
			CollectionPersister roleAfterFlush = (entry == null) ? null : entry.getLoadedPersister();
			if ( roleAfterFlush == null ) {
				throw new QueryException( "The collection was unreferenced" );
			}
			plan = factory.getQueryPlanCache().getFilterQueryPlan( filter, roleAfterFlush.getRole(), shallow, getEnabledFilters() );
		}
		else {
			// otherwise, we only need to flush if there are in-memory changes
			// to the queried tables
			plan = factory.getQueryPlanCache().getFilterQueryPlan( filter, roleBeforeFlush.getRole(), shallow, getEnabledFilters() );
			if ( autoFlushIfRequired( plan.getQuerySpaces() ) ) {
				// might need to run a different filter entirely after the flush
				// because the collection role may have changed
				entry = persistenceContext.getCollectionEntryOrNull( collection );
				CollectionPersister roleAfterFlush = (entry == null) ? null : entry.getLoadedPersister();
				if ( roleBeforeFlush != roleAfterFlush ) {
					if ( roleAfterFlush == null ) {
						throw new QueryException( "The collection was dereferenced" );
					}
					plan = factory.getQueryPlanCache().getFilterQueryPlan( filter, roleAfterFlush.getRole(), shallow, getEnabledFilters() );
				}
			}
		}

		if ( parameters != null ) {
			parameters.getPositionalParameterValues()[0] = entry.getLoadedKey();
			parameters.getPositionalParameterTypes()[0] = entry.getLoadedPersister().getKeyType();
		}

		return plan;
	}

	public List listFilter(Object collection, String filter, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
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
			afterOperation(success);
		}
		return results;
	}

	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		FilterQueryPlan plan = getFilterQueryPlan( collection, filter, queryParameters, true );
		return plan.performIterate( queryParameters, this );
	}

	public Criteria createCriteria(Class persistentClass, String alias) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	public Criteria createCriteria(String entityName, String alias) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl(entityName, alias, this);
	}

	public Criteria createCriteria(Class persistentClass) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	public Criteria createCriteria(String entityName) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return new CriteriaImpl(entityName, this);
	}

	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
		errorIfClosed();
		checkTransactionSynchStatus();
		String entityName = criteria.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable(entityName),
				factory,
				criteria,
				entityName,
				getLoadQueryInfluencers()
		);
		autoFlushIfRequired( loader.getQuerySpaces() );
		dontFlushFromFind++;
		try {
			return loader.scroll(this, scrollMode);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public List list(CriteriaImpl criteria) throws HibernateException {
		final NaturalIdLoadAccess naturalIdLoadAccess = this.tryNaturalIdLoadAccess( criteria );
		if ( naturalIdLoadAccess != null ) {
			// EARLY EXIT!
			return Arrays.asList( naturalIdLoadAccess.load() );
		}

		errorIfClosed();
		checkTransactionSynchStatus();
		String[] implementors = factory.getImplementors( criteria.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		Set spaces = new HashSet();
		for( int i=0; i <size; i++ ) {

			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
					factory,
					criteria,
					implementors[i],
					getLoadQueryInfluencers()
				);

			spaces.addAll( loaders[i].getQuerySpaces() );

		}

		autoFlushIfRequired(spaces);

		List results = Collections.EMPTY_LIST;
		dontFlushFromFind++;
		boolean success = false;
		try {
			for( int i=0; i<size; i++ ) {
				final List currentResults = loaders[i].list(this);
				currentResults.addAll(results);
				results = currentResults;
			}
			success = true;
		}
		finally {
			dontFlushFromFind--;
			afterOperation(success);
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
	 *         should be performed
	 */
	private NaturalIdLoadAccess tryNaturalIdLoadAccess(CriteriaImpl criteria) {
		// See if the criteria lookup is by naturalId
		if ( !criteria.isLookupByNaturalKey() ) {
			return null;
		}

		final String entityName = criteria.getEntityOrClassName();
		final EntityPersister entityPersister = factory.getEntityPersister( entityName );

		// Verify the entity actually has a natural id, needed for legacy support as NaturalIdentifier criteria
		// queries did no natural id validation
		if ( !entityPersister.hasNaturalIdentifier() ) {
			return null;
		}

		// Since isLookupByNaturalKey is true there can be only one CriterionEntry and getCriterion() will
		// return an instanceof NaturalIdentifier
		final CriterionEntry criterionEntry = (CriterionEntry) criteria.iterateExpressionEntries().next();
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
		for ( int i = 0; i < naturalIdentifierProperties.length; i++ ) {
			final String naturalIdProperty = propertyNames[naturalIdentifierProperties[i]];
			final Object naturalIdValue = naturalIdValues.get( naturalIdProperty );

			if ( naturalIdValue == null ) {
				// A NaturalId property is missing from the critera query, can't use NaturalIdLoadAccess
				return null;
			}

			naturalIdLoader.using( naturalIdProperty, naturalIdValue );
		}

		// Critera query contains a valid naturalId, use the new API
		LOG.warn( "Session.byNaturalId(" + entityName
				+ ") should be used for naturalId queries instead of Restrictions.naturalId() from a Criteria" );

		return naturalIdLoader;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister(entityName);
		if ( !(persister instanceof OuterJoinLoadable) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return ( OuterJoinLoadable ) persister;
	}

	public boolean contains(Object object) {
		errorIfClosed();
		checkTransactionSynchStatus();
		if ( object instanceof HibernateProxy ) {
			//do not use proxiesByKey, since not all
			//proxies that point to this session's
			//instances are in that collection!
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				//if it is an uninitialized proxy, pointing
				//with this session, then when it is accessed,
				//the underlying instance will be "contained"
				return li.getSession()==this;
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
		return entry != null && entry.getStatus() != Status.DELETED && entry.getStatus() != Status.GONE;
	}

	public Query createQuery(String queryString) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return super.createQuery( queryString );
	}

	public SQLQuery createSQLQuery(String sql) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return super.createSQLQuery( sql );
	}

	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Scroll SQL query: {0}", customQuery.getSQL() );
		}

		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return loader.scroll(queryParameters, this);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	// basically just an adapted copy of find(CriteriaImpl)
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters)
	throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "SQL query: {0}", customQuery.getSQL() );
		}

		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++;
		boolean success = false;
		try {
			List results = loader.list(this, queryParameters);
			success = true;
			return results;
		}
		finally {
			dontFlushFromFind--;
			afterOperation(success);
		}
	}

	public SessionFactoryImplementor getSessionFactory() {
		checkTransactionSynchStatus();
		return factory;
	}

	public void initializeCollection(PersistentCollection collection, boolean writing)
	throws HibernateException {
		errorIfClosed();
		checkTransactionSynchStatus();
		InitializeCollectionEvent event = new InitializeCollectionEvent( collection, this );
		for ( InitializeCollectionEventListener listener : listeners( EventType.INIT_COLLECTION ) ) {
			listener.onInitializeCollection( event );
		}
	}

	public String bestGuessEntityName(Object object) {
		if (object instanceof HibernateProxy) {
			LazyInitializer initializer = ( ( HibernateProxy ) object ).getHibernateLazyInitializer();
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( initializer.isUninitialized() ) {
				return initializer.getEntityName();
			}
			object = initializer.getImplementation();
		}
		EntityEntry entry = persistenceContext.getEntry(object);
		if (entry==null) {
			return guessEntityName(object);
		}
		else {
			return entry.getPersister().getEntityName();
		}
	}

	public String getEntityName(Object object) {
		errorIfClosed();
		checkTransactionSynchStatus();
		if (object instanceof HibernateProxy) {
			if ( !persistenceContext.containsProxy( object ) ) {
				throw new TransientObjectException("proxy was not associated with the session");
			}
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}

		EntityEntry entry = persistenceContext.getEntry(object);
		if ( entry == null ) {
			throwTransientObjectException( object );
		}
		return entry.getPersister().getEntityName();
	}

	private void throwTransientObjectException(Object object) throws HibernateException {
		throw new TransientObjectException(
				"object references an unsaved transient instance - save the transient instance before flushing: " +
				guessEntityName(object)
			);
	}

	public String guessEntityName(Object object) throws HibernateException {
		errorIfClosed();
		return entityNameResolver.resolveEntityName( object );
	}

	public void cancelQuery() throws HibernateException {
		errorIfClosed();
		getTransactionCoordinator().getJdbcCoordinator().cancelLastQuery();
	}

	public Interceptor getInterceptor() {
		checkTransactionSynchStatus();
		return interceptor;
	}

	public int getDontFlushFromFind() {
		return dontFlushFromFind;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder(500)
			.append( "SessionImpl(" );
		if ( !isClosed() ) {
			buf.append(persistenceContext)
				.append(";")
				.append(actionQueue);
		}
		else {
			buf.append("<closed>");
		}
		return buf.append(')').toString();
	}

	public ActionQueue getActionQueue() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return actionQueue;
	}

	public PersistenceContext getPersistenceContext() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return persistenceContext;
	}

	public SessionStatistics getStatistics() {
		checkTransactionSynchStatus();
		return new SessionStatisticsImpl(this);
	}

	public boolean isEventSource() {
		checkTransactionSynchStatus();
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDefaultReadOnly() {
		return persistenceContext.isDefaultReadOnly();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefaultReadOnly(boolean defaultReadOnly) {
		persistenceContext.setDefaultReadOnly( defaultReadOnly );
	}

	public boolean isReadOnly(Object entityOrProxy) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return persistenceContext.isReadOnly( entityOrProxy );
	}

	public void setReadOnly(Object entity, boolean readOnly) {
		errorIfClosed();
		checkTransactionSynchStatus();
		persistenceContext.setReadOnly( entity, readOnly );
	}

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
		return transactionCoordinator.getJdbcCoordinator().coordinateWork( work );
	}

	public void afterScrollOperation() {
		// nothing to do in a stateful session
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		errorIfClosed();
		return transactionCoordinator;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public Filter getEnabledFilter(String filterName) {
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getEnabledFilter( filterName );
	}

	/**
	 * {@inheritDoc}
	 */
	public Filter enableFilter(String filterName) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.enableFilter( filterName );
	}

	/**
	 * {@inheritDoc}
	 */
	public void disableFilter(String filterName) {
		errorIfClosed();
		checkTransactionSynchStatus();
		loadQueryInfluencers.disableFilter( filterName );
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getFilterParameterValue(String filterParameterName) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getFilterParameterValue( filterParameterName );
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getFilterParameterType(String filterParameterName) {
		errorIfClosed();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getFilterParameterType( filterParameterName );
	}

	/**
	 * {@inheritDoc}
	 */
	public Map getEnabledFilters() {
		errorIfClosed();
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getEnabledFilters();
	}


	// internal fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * {@inheritDoc}
	 */
	public String getFetchProfile() {
		checkTransactionSynchStatus();
		return loadQueryInfluencers.getInternalFetchProfile();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFetchProfile(String fetchProfile) {
		errorIfClosed();
		checkTransactionSynchStatus();
		loadQueryInfluencers.setInternalFetchProfile( fetchProfile );
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		return loadQueryInfluencers.isFetchProfileEnabled( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		loadQueryInfluencers.enableFetchProfile( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		loadQueryInfluencers.disableFetchProfile( name );
	}


	private void checkTransactionSynchStatus() {
		if ( !isClosed() ) {
			transactionCoordinator.pulse();
		}
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param ois The input stream from which we are being read...
	 * @throws IOException Indicates a general IO stream exception
	 * @throws ClassNotFoundException Indicates a class resolution issue
	 */
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		LOG.trace( "Deserializing session" );

		ois.defaultReadObject();

		entityNameResolver = new CoordinatingEntityNameResolver();

		connectionReleaseMode = ConnectionReleaseMode.parse( ( String ) ois.readObject() );
		autoClear = ois.readBoolean();
		autoJoinTransactions = ois.readBoolean();
		flushMode = FlushMode.valueOf( ( String ) ois.readObject() );
		cacheMode = CacheMode.valueOf( ( String ) ois.readObject() );
		flushBeforeCompletionEnabled = ois.readBoolean();
		autoCloseSessionEnabled = ois.readBoolean();
		interceptor = ( Interceptor ) ois.readObject();

		factory = SessionFactoryImpl.deserialize( ois );
		sessionOwner = ( SessionOwner ) ois.readObject();

		transactionCoordinator = TransactionCoordinatorImpl.deserialize( ois, this );

		persistenceContext = StatefulPersistenceContext.deserialize( ois, this );
		actionQueue = ActionQueue.deserialize( ois, this );

		loadQueryInfluencers = (LoadQueryInfluencers) ois.readObject();

		// LoadQueryInfluencers.getEnabledFilters() tries to validate each enabled
		// filter, which will fail when called before FilterImpl.afterDeserialize( factory );
		// Instead lookup the filter by name and then call FilterImpl.afterDeserialize( factory ).
		for ( String filterName : loadQueryInfluencers.getEnabledFilterNames() ) {
			((FilterImpl) loadQueryInfluencers.getEnabledFilter( filterName )).afterDeserialize( factory );
		}
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param oos The output stream to which we are being written...
	 * @throws IOException Indicates a general IO stream exception
	 */
	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( ! transactionCoordinator.getJdbcCoordinator().getLogicalConnection().isReadyForSerialization() ) {
			throw new IllegalStateException( "Cannot serialize a session while connected" );
		}

		LOG.trace( "Serializing session" );

		oos.defaultWriteObject();

		oos.writeObject( connectionReleaseMode.toString() );
		oos.writeBoolean( autoClear );
		oos.writeBoolean( autoJoinTransactions );
		oos.writeObject( flushMode.toString() );
		oos.writeObject( cacheMode.name() );
		oos.writeBoolean( flushBeforeCompletionEnabled );
		oos.writeBoolean( autoCloseSessionEnabled );
		// we need to writeObject() on this since interceptor is user defined
		oos.writeObject( interceptor );

		factory.serialize( oos );
		oos.writeObject( sessionOwner );

		transactionCoordinator.serialize( oos );

		persistenceContext.serialize( oos );
		actionQueue.serialize( oos );

		// todo : look at optimizing these...
		oos.writeObject( loadQueryInfluencers );
	}

	/**
	 * {@inheritDoc}
	 */
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
			return session.getFactory().getJdbcServices().getLobCreator( session );
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

	private static class SharedSessionBuilderImpl extends SessionFactoryImpl.SessionBuilderImpl implements SharedSessionBuilder {
		private final SessionImpl session;
		private boolean shareTransactionContext;

		private SharedSessionBuilderImpl(SessionImpl session) {
			super( session.factory );
			this.session = session;
			super.owner( session.sessionOwner );
			super.tenantIdentifier( session.getTenantIdentifier() );
		}

		@Override
		public SessionBuilder tenantIdentifier(String tenantIdentifier) {
			// todo : is this always true?  Or just in the case of sharing JDBC resources?
			throw new SessionException( "Cannot redefine tenant identifier on child session" );
		}

		@Override
		protected TransactionCoordinatorImpl getTransactionCoordinator() {
			return shareTransactionContext ? session.transactionCoordinator : super.getTransactionCoordinator();
		}

		@Override
		public SharedSessionBuilder interceptor() {
			return interceptor( session.interceptor );
		}

		@Override
		public SharedSessionBuilder connection() {
			this.shareTransactionContext = true;
			return this;
		}

		@Override
		public SharedSessionBuilder connectionReleaseMode() {
			return connectionReleaseMode( session.connectionReleaseMode );
		}

		@Override
		public SharedSessionBuilder autoJoinTransactions() {
			return autoJoinTransactions( session.autoJoinTransactions );
		}

		@Override
		public SharedSessionBuilder autoClose() {
			return autoClose( session.autoCloseSessionEnabled );
		}

		@Override
		public SharedSessionBuilder flushBeforeCompletion() {
			return flushBeforeCompletion( session.flushBeforeCompletionEnabled );
		}

		/**
		 * @deprecated Use {@link #connection()} instead
		 */
		@Override
		@Deprecated
		public SharedSessionBuilder transactionContext() {
			return connection();
		}

		@Override
		public SharedSessionBuilder interceptor(Interceptor interceptor) {
			return (SharedSessionBuilder) super.interceptor( interceptor );
		}

		@Override
		public SharedSessionBuilder noInterceptor() {
			return (SharedSessionBuilder) super.noInterceptor();
		}

		@Override
		public SharedSessionBuilder connection(Connection connection) {
			return (SharedSessionBuilder) super.connection( connection );
		}

		@Override
		public SharedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
			return (SharedSessionBuilder) super.connectionReleaseMode( connectionReleaseMode );
		}

		@Override
		public SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
			return (SharedSessionBuilder) super.autoJoinTransactions( autoJoinTransactions );
		}

		@Override
		public SharedSessionBuilder autoClose(boolean autoClose) {
			return (SharedSessionBuilder) super.autoClose( autoClose );
		}

		@Override
		public SharedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
			return (SharedSessionBuilder) super.flushBeforeCompletion( flushBeforeCompletion );
		}
	}

	private class CoordinatingEntityNameResolver implements EntityNameResolver {
		public String resolveEntityName(Object entity) {
			String entityName = interceptor.getEntityName( entity );
			if ( entityName != null ) {
				return entityName;
			}

			for ( EntityNameResolver resolver : factory.iterateEntityNameResolvers() ) {
				entityName = resolver.resolveEntityName( entity );
				if ( entityName != null ) {
					break;
				}
			}

			if ( entityName != null ) {
				return entityName;
			}

			// the old-time stand-by...
			return entity.getClass().getName();
		}
	}

	private class LockRequestImpl implements LockRequest {
		private final LockOptions lockOptions;
		private LockRequestImpl(LockOptions lo) {
			lockOptions = new LockOptions();
			LockOptions.copy(lo, lockOptions);
		}

		public LockMode getLockMode() {
			return lockOptions.getLockMode();
		}

		public LockRequest setLockMode(LockMode lockMode) {
			lockOptions.setLockMode(lockMode);
			return this;
		}

		public int getTimeOut() {
			return lockOptions.getTimeOut();
		}

		public LockRequest setTimeOut(int timeout) {
			lockOptions.setTimeOut(timeout);
			return this;
		}

		public boolean getScope() {
			return lockOptions.getScope();
		}

		public LockRequest setScope(boolean scope) {
			lockOptions.setScope(scope);
			return this;
		}

		public void lock(String entityName, Object object) throws HibernateException {
			fireLock( entityName, object, lockOptions );
		}
		public void lock(Object object) throws HibernateException {
			fireLock( object, lockOptions );
		}
	}

	private class IdentifierLoadAccessImpl implements IdentifierLoadAccess {
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;

		private IdentifierLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}

		private IdentifierLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private IdentifierLoadAccessImpl(Class entityClass) {
			this( entityClass.getName() );
		}

		@Override
		public final IdentifierLoadAccessImpl with(LockOptions lockOptions) {
			this.lockOptions = lockOptions;
			return this;
		}

		@Override
		public final Object getReference(Serializable id) {
			if ( this.lockOptions != null ) {
				LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, SessionImpl.this );
				fireLoad( event, LoadEventListener.LOAD );
				return event.getResult();
			}

			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, SessionImpl.this );
			boolean success = false;
			try {
				fireLoad( event, LoadEventListener.LOAD );
				if ( event.getResult() == null ) {
					getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityPersister.getEntityName(), id );
				}
				success = true;
				return event.getResult();
			}
			finally {
				afterOperation( success );
			}
		}

		@Override
		public final Object load(Serializable id) {
			if ( this.lockOptions != null ) {
				LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, SessionImpl.this );
				fireLoad( event, LoadEventListener.GET );
				return event.getResult();
			}

			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, SessionImpl.this );
			boolean success = false;
			try {
				fireLoad( event, LoadEventListener.GET );
				success = true;
				return event.getResult();
			}
			finally {
				afterOperation( success );
			}
		}
	}

	private EntityPersister locateEntityPersister(String entityName) {
		final EntityPersister entityPersister = factory.getEntityPersister( entityName );
		if ( entityPersister == null ) {
			throw new HibernateException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	private abstract class BaseNaturalIdLoadAccessImpl  {
		private final EntityPersister entityPersister;
		private LockOptions lockOptions;
		private boolean synchronizationEnabled = true;

		private BaseNaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			this.entityPersister = entityPersister;

			if ( ! entityPersister.hasNaturalIdentifier() ) {
				throw new HibernateException(
						String.format( "Entity [%s] did not define a natural id", entityPersister.getEntityName() )
				);
			}
		}

		private BaseNaturalIdLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private BaseNaturalIdLoadAccessImpl(Class entityClass) {
			this( entityClass.getName() );
		}

		public BaseNaturalIdLoadAccessImpl with(LockOptions lockOptions) {
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
			if ( ! synchronizationEnabled ) {
				// synchronization (this process) was disabled
				return;
			}
			if ( ! isTransactionInProgress() ) {
				// not in a transaction so skip synchronization
				return;
			}
			if ( entityPersister.getEntityMetamodel().hasImmutableNaturalId() ) {
				// only mutable natural-ids need this processing 
				return;
			}

			for ( Serializable pk : getPersistenceContext().getNaturalIdHelper().getCachedPkResolutions( entityPersister ) ) {
				final EntityKey entityKey = generateEntityKey( pk, entityPersister );
				final Object entity = getPersistenceContext().getEntity( entityKey );
				final EntityEntry entry = getPersistenceContext().getEntry( entity );

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

	private class NaturalIdLoadAccessImpl extends BaseNaturalIdLoadAccessImpl implements NaturalIdLoadAccess {
		private final Map<String, Object> naturalIdParameters = new LinkedHashMap<String, Object>();

		private NaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			super(entityPersister);
		}

		private NaturalIdLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private NaturalIdLoadAccessImpl(Class entityClass) {
			this( entityClass.getName() );
		}
		
		@Override
		public NaturalIdLoadAccessImpl with(LockOptions lockOptions) {
			return (NaturalIdLoadAccessImpl) super.with( lockOptions );
		}

		@Override
		public NaturalIdLoadAccess using(String attributeName, Object value) {
			naturalIdParameters.put( attributeName, value );
			return this;
		}

		@Override
		public NaturalIdLoadAccessImpl setSynchronizationEnabled(boolean synchronizationEnabled) {
			super.synchronizationEnabled( synchronizationEnabled );
			return this;
		}

		@Override
		public final Object getReference() {
			final Serializable entityId = resolveNaturalId( this.naturalIdParameters );
			if ( entityId == null ) {
				return null;
			}
			return this.getIdentifierLoadAccess().getReference( entityId );
		}

		@Override
		public final Object load() {
			final Serializable entityId = resolveNaturalId( this.naturalIdParameters );
			if ( entityId == null ) {
				return null;
			}
			try {
				return this.getIdentifierLoadAccess().load( entityId );
			}
			catch (EntityNotFoundException enf) {
				// OK
			}
			catch (ObjectNotFoundException nf) {
				// OK
			}
			return null;
		}
	}

	private class SimpleNaturalIdLoadAccessImpl extends BaseNaturalIdLoadAccessImpl implements SimpleNaturalIdLoadAccess {
		private final String naturalIdAttributeName;

		private SimpleNaturalIdLoadAccessImpl(EntityPersister entityPersister) {
			super(entityPersister);

			if ( entityPersister.getNaturalIdentifierProperties().length != 1 ) {
				throw new HibernateException(
						String.format( "Entity [%s] did not define a simple natural id", entityPersister.getEntityName() )
				);
			}

			final int naturalIdAttributePosition = entityPersister.getNaturalIdentifierProperties()[0];
			this.naturalIdAttributeName = entityPersister.getPropertyNames()[ naturalIdAttributePosition ];
		}

		private SimpleNaturalIdLoadAccessImpl(String entityName) {
			this( locateEntityPersister( entityName ) );
		}

		private SimpleNaturalIdLoadAccessImpl(Class entityClass) {
			this( entityClass.getName() );
		}

		@Override
		public final SimpleNaturalIdLoadAccessImpl with(LockOptions lockOptions) {
			return (SimpleNaturalIdLoadAccessImpl) super.with( lockOptions );
		}
		
		private Map<String, Object> getNaturalIdParameters(Object naturalIdValue) {
			return Collections.singletonMap( naturalIdAttributeName, naturalIdValue );
		}

		@Override
		public SimpleNaturalIdLoadAccessImpl setSynchronizationEnabled(boolean synchronizationEnabled) {
			super.synchronizationEnabled( synchronizationEnabled );
			return this;
		}

		@Override
		public Object getReference(Object naturalIdValue) {
			final Serializable entityId = resolveNaturalId( getNaturalIdParameters( naturalIdValue ) );
			if ( entityId == null ) {
				return null;
			}
			return this.getIdentifierLoadAccess().getReference( entityId );
		}

		@Override
		public Object load(Object naturalIdValue) {
			final Serializable entityId = resolveNaturalId( getNaturalIdParameters( naturalIdValue ) );
			if ( entityId == null ) {
				return null;
			}
			try {
				return this.getIdentifierLoadAccess().load( entityId );
			}
			catch (EntityNotFoundException enf) {
				// OK
			}
			catch (ObjectNotFoundException nf) {
				// OK
			}
			return null;
		}
	}
}
