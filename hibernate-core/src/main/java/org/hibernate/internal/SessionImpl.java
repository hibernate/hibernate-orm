/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.Timeout;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.*;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.PersistenceContexts;
import org.hibernate.engine.internal.TransactionCompletionCallbacksImpl;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.ActionQueue.TransactionCompletionProcesses;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroups;
import org.hibernate.event.spi.*;
import org.hibernate.event.spi.LoadEventListener.LoadType;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.jpa.internal.LegacySpecHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.FlushModeTypeHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.internal.CacheLoadHelper;
import org.hibernate.loader.internal.IdentifierLoadAccessImpl;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.loader.internal.NaturalIdLoadAccessImpl;
import org.hibernate.loader.internal.SimpleNaturalIdLoadAccessImpl;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.UnknownSqlResultSetMappingException;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.internal.SessionStatisticsImpl;
import org.hibernate.type.descriptor.WrapperOptions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.Timeouts.WAIT_FOREVER_MILLI;
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
import static org.hibernate.internal.LockOptionsHelper.applyPropertiesToLockOptions;
import static org.hibernate.jpa.HibernateHints.HINT_BATCH_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_ENABLE_SUBSELECT_FETCH;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_PROFILE;
import static org.hibernate.jpa.HibernateHints.HINT_FLUSH_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_JDBC_BATCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_LOCK_TIMEOUT;
import static org.hibernate.jpa.LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.jpa.internal.util.CacheModeHelper.interpretCacheMode;
import static org.hibernate.jpa.internal.util.ConfigurationHelper.getBoolean;
import static org.hibernate.jpa.internal.util.FlushModeTypeHelper.getFlushModeType;
import static org.hibernate.pretty.MessageHelper.infoString;
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
 * @implNote The {@code SessionImpl} does not directly perform operations against the database or second-level cache.
 * Instead, it is an {@link org.hibernate.event.spi.EventSource}, raising events which are processed by various
 * implementations of the listener interfaces defined by {@link org.hibernate.event.spi}. These listeners typically
 * place {@link org.hibernate.action.internal.EntityAction} instances on the {@link ActionQueue} associated with the
 * session, and such actions are executed asynchronously when the session is {@linkplain #flush flushed}. The
 * motivation behind this architecture is two-fold: first, it enables customization by sophisticated extensions to
 * Hibernate ORM, and, second, it enables the transactional write-behind semantics of a stateful session. The stateful
 * session holds its state in an instance of {@code StatefulPersistenceContext}, which we may view as the first-level
 * cache associated with the session.
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
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( SessionImpl.class );

	// Defaults to null which means the properties are the default
	// as defined in FastSessionServices#defaultSessionProperties
	private Map<String, Object> properties;

	private transient ActionQueue actionQueue;
	private transient EventListenerGroups eventListenerGroups;
	private transient PersistenceContext persistenceContext;

	private transient LoadQueryInfluencers loadQueryInfluencers;

	private LockOptions lockOptions;

	private FlushMode flushMode;

	private final boolean autoClear;
	private final boolean autoClose;

	private final boolean identifierRollbackEnabled;

	private transient LoadEvent loadEvent; //cached LoadEvent instance
	private transient PostLoadEvent postLoadEvent; //cached PostLoadEvent instance

	private transient TransactionObserver transactionObserver;

	public SessionImpl(SessionFactoryImpl factory, SessionCreationOptions options) {
		super( factory, options );

		final var sessionOpenEvent = getEventMonitor().beginSessionOpenEvent();
		try {
			persistenceContext = createPersistenceContext( options );
			actionQueue = createActionQueue();
			eventListenerGroups = factory.getEventListenerGroups();

			flushMode = options.getInitialSessionFlushMode();

			autoClear = options.shouldAutoClear();
			autoClose = options.shouldAutoClose();

			identifierRollbackEnabled = options.isIdentifierRollbackEnabled();

			setUpTransactionCompletionProcesses( options );

			loadQueryInfluencers = new LoadQueryInfluencers( factory, options );

			if ( properties != null ) {
				//There might be custom properties for this session that affect the LockOptions state
				applyPropertiesToLockOptions( properties, this::getLockOptionsForWrite );
			}

			// NOTE : pulse() already handles auto-join-ability correctly
			getTransactionCoordinator().pulse();

			// do not override explicitly set flush mode ( SessionBuilder#flushMode() )
			if ( getHibernateFlushMode() == null ) {
				setHibernateFlushMode( getInitialFlushMode() );
			}

			setUpMultitenancy( factory, loadQueryInfluencers );

			final var statistics = factory.getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.openSession();
			}

			if ( LOG.isTraceEnabled() ) {
				LOG.tracef( "Opened Session [%s] at timestamp: %s",
						getSessionIdentifier(), currentTimeMillis() );
			}
		}
		finally {
			getEventMonitor().completeSessionOpenEvent( sessionOpenEvent, this );
		}
	}

	private void setUpTransactionCompletionProcesses(SessionCreationOptions options) {
		if ( options instanceof SharedSessionCreationOptions sharedOptions
				&& sharedOptions.isTransactionCoordinatorShared() ) {
			final var callbacks = sharedOptions.getTransactionCompletionCallbacks();
			if ( callbacks != null ) {
				actionQueue.setTransactionCompletionCallbacks( callbacks, true );
			}
		}
	}

	private FlushMode getInitialFlushMode() {
		return properties == null
				? getSessionFactoryOptions().getInitialSessionFlushMode()
				: ConfigurationHelper.getFlushMode( getSessionProperty( HINT_FLUSH_MODE ), FlushMode.AUTO );
	}

	protected PersistenceContext createPersistenceContext(SessionCreationOptions options) {
		final var persistenceContext = PersistenceContexts.createPersistenceContext( this );
		persistenceContext.setDefaultReadOnly( options.isReadOnly() );
		return persistenceContext;
	}

	protected ActionQueue createActionQueue() {
		return new ActionQueue( this );
	}

	private LockOptions getLockOptionsForRead() {
		return lockOptions == null
				? getSessionFactoryOptions().getDefaultLockOptions()
				: lockOptions;
	}

	private LockOptions getLockOptionsForWrite() {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		return lockOptions;
	}

	protected void applyQuerySettingsAndHints(SelectionQuery<?> query) {
		applyLockOptionsHint( query );
	}

	protected void applyLockOptionsHint(SelectionQuery<?> query) {
		final var lockOptionsForRead = getLockOptionsForRead();
		if ( lockOptionsForRead.getLockMode() != LockMode.NONE ) {
			query.setLockMode( getLockMode( lockOptionsForRead.getLockMode() ) );
		}

		final Object specQueryTimeout = getHintedQueryTimeout();
		if ( specQueryTimeout != null ) {
			query.setHint( HINT_SPEC_QUERY_TIMEOUT, specQueryTimeout );
		}
	}

	private Object getHintedQueryTimeout() {
		return LegacySpecHelper.getInteger(
				HINT_SPEC_QUERY_TIMEOUT,
				HINT_JAVAEE_QUERY_TIMEOUT,
				this::getSessionProperty
		);
	}

	protected void applyQuerySettingsAndHints(Query<?> query) {
		applyQuerySettingsAndHints( (SelectionQuery<?>) query );
		applyLockTimeoutHint( query );
	}

	private void applyLockTimeoutHint(Query<?> query) {
		final Integer specLockTimeout = getHintedLockTimeout();
		if ( specLockTimeout != null ) {
			query.setHint( HINT_SPEC_LOCK_TIMEOUT, specLockTimeout );
		}
	}

	private Integer getHintedLockTimeout() {
		return LegacySpecHelper.getInteger(
				HINT_SPEC_LOCK_TIMEOUT,
				HINT_JAVAEE_LOCK_TIMEOUT,
				this::getSessionProperty,
				// treat WAIT_FOREVER the same as null
				value -> !Integer.valueOf( WAIT_FOREVER_MILLI ).equals( value )
		);
	}

	private Object getSessionProperty(String propertyName) {
		return properties == null
				? getDefaultProperties().get( propertyName )
				: properties.get( propertyName );
	}

	private Map<String, Object> getDefaultProperties() {
		return getSessionFactoryOptions().getDefaultSessionProperties();
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

		eventListenerGroups.eventListenerGroup_CLEAR
				.fireLazyEventOnEachListener( this::createClearEvent, ClearEventListener::onClear );
	}

	private ClearEvent createClearEvent() {
		return new ClearEvent( this );
	}

	@Override
	public void close() {
		if ( isClosed() ) {
			if ( getSessionFactoryOptions().getJpaCompliance().isJpaClosedComplianceEnabled() ) {
				throw new IllegalStateException( "EntityManager was already closed" );
			}
			LOG.trace( "Already closed" );
		}
		else {
			closeWithoutOpenChecks();
		}
	}

	public void closeWithoutOpenChecks() {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Closing session [%s]", getSessionIdentifier() );
		}

		final var eventMonitor = getEventMonitor();
		final var sessionClosedEvent = eventMonitor.beginSessionClosedEvent();
		try {
			if ( isJpaBootstrap() ) {
				// Original HEM close behavior
				checkSessionFactoryOpen();
				checkOpenOrWaitingForAutoClose();
				if ( getSessionFactoryOptions().isReleaseResourcesOnCloseEnabled()
						|| !isTransactionInProgressAndNotMarkedForRollback() ) {
					super.close();
				}
				else {
					// In the JPA bootstrap, if the session is closed
					// before the transaction commits, we just mark the
					// session as closed, and set waitingForAutoClose.
					// This method will be called a second time from
					// afterTransactionCompletion when the transaction
					// commits, and the session will be closed for real.
					prepareForAutoClose();
				}
			}
			else {
				// Regular Hibernate behavior
				super.close();
			}
		}
		finally {
			final var statistics = getSessionFactory().getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				statistics.closeSession();
			}
			eventMonitor.completeSessionClosedEvent( sessionClosedEvent, this );
		}
	}

	private boolean isJpaBootstrap() {
		return getSessionFactoryOptions().isJpaBootstrap();
	}

	private boolean isTransactionInProgressAndNotMarkedForRollback() {
		return isOpenOrWaitingForAutoClose()
			&& isTransactionActiveAndNotMarkedForRollback();
	}

	private boolean isTransactionActiveAndNotMarkedForRollback() {
		final var transactionCoordinator = getTransactionCoordinator();
		return transactionCoordinator.isJoined()
			&& transactionCoordinator.getTransactionDriverControl().isActiveAndNoMarkedForRollback();
	}

	@Override
	protected boolean shouldCloseJdbcCoordinatorOnClose(boolean isTransactionCoordinatorShared) {
		if ( isTransactionCoordinatorShared ) {
			final var actionQueue = getActionQueue();
			if ( actionQueue.hasBeforeTransactionActions() || actionQueue.hasAfterTransactionActions() ) {
				LOG.warn( "Closing shared session with unprocessed transaction completion actions" );
			}
		}
		return !isTransactionCoordinatorShared;
	}

	/**
	 * Should this session be automatically closed after the current
	 * transaction completes?
	 */
	public boolean isAutoCloseSessionEnabled() {
		return autoClose;
	}

	@Override
	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
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
			LOG.trace( "Forcing-closing session since factory is already closed" );
			setClosed();
		}
	}

	private void managedFlush() {
		if ( !isOpenOrWaitingForAutoClose() ) {
			LOG.trace( "Skipping auto-flush since the session is closed" );
		}
		else {
			LOG.trace( "Automatically flushing session" );
			fireFlush();
		}
	}

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
		LOG.trace( "Automatically closing session" );
		closeWithoutOpenChecks();
	}

	@Override
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
	public LockMode getCurrentLockMode(Object object) {
		checkOpen();
		checkTransactionSynchStatus();
		if ( object == null ) {
			throw new NullPointerException( "null object passed to getCurrentLockMode()" );
		}

		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			object = lazyInitializer.getImplementation( this );
			if ( object == null ) {
				return LockMode.NONE;
			}
		}

		final var entry = getEntityEntry( object );
		if ( entry.getStatus().isDeletedOrGone() ) {
			throw new ObjectDeletedException( "Given entity was removed", entry.getId(),
					entry.getPersister().getEntityName() );
		}
		else {
			return entry.getLockMode();
		}
	}

	@Override
	public Object getEntityUsingInterceptor(EntityKey key) {
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
		super.delayedAfterCompletion();
	}

	@Override
	public void pulseTransactionCoordinator() {
		super.pulseTransactionCoordinator();
	}

	@Override
	public void checkOpenOrWaitingForAutoClose() {
		if ( !waitingForAutoClose ) {
			checkOpen();
		}
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
	public void lock(Object object, LockMode lockMode) {
		final var lockOptions = copySessionLockOptions();
		lockOptions.setLockMode( lockMode );
		fireLock( new LockEvent( object, lockOptions, this ) );
	}

	@Override
	public void lock(Object object, LockMode lockMode, LockOption... lockOptions) {
		lock( object, buildLockOptions( lockMode, lockOptions ) );
	}

	private void fireLock(final LockEvent lockEvent) {
		checkOpen();
		checkEntityManaged( lockEvent.getEntityName(), lockEvent.getObject() );
		try {
			pulseTransactionCoordinator();
			checkTransactionNeededForLock( lockEvent.getLockMode() );
			eventListenerGroups.eventListenerGroup_LOCK
					.fireEventOnEachListener( lockEvent,
							LockEventListener::onLock );
		}
		catch ( RuntimeException e ) {
			convertIfJpaBootstrap( e, lockEvent.getLockOptions() );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void convertIfJpaBootstrap(RuntimeException exception, LockOptions lockOptions) {
		if ( !isJpaBootstrap() && exception instanceof HibernateException ) {
			throw exception;
		}
		else if ( exception instanceof MappingException ) {
			// I believe this is now obsolete everywhere we do it,
			// but we do it everywhere else, so let's do it here
			throw getExceptionConverter()
					.convert( new IllegalArgumentException( exception.getMessage(), exception ) );
		}
		else {
			throw getExceptionConverter().convert( exception, lockOptions );
		}
	}

	// persist() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void persist(String entityName, Object object) {
		checkOpen();
		firePersist( new PersistEvent( entityName, object, this ) );
	}

	@Override
	public void persist(Object object) {
		checkOpen();
		firePersist( new PersistEvent( null, object, this ) );
	}

	@Override
	public void persist(String entityName, Object object, PersistContext copiedAlready) {
		checkOpenOrWaitingForAutoClose();
		firePersist( copiedAlready, new PersistEvent( entityName, object, this ) );
	}

	private void firePersist(final PersistEvent persistEvent) {
		Throwable originalException = null;
		try {
			checkNotReadOnly();
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();

			eventListenerGroups.eventListenerGroup_PERSIST
					.fireEventOnEachListener( persistEvent,
							PersistEventListener::onPersist );
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
			ExceptionHelper.rethrow( originalException );
		}
	}

	private void firePersist(final PersistContext copiedAlready, final PersistEvent persistEvent) {
		try {
			pulseTransactionCoordinator();
			//Uses a capturing lambda in this case as we need to carry the additional Map parameter:
			eventListenerGroups.eventListenerGroup_PERSIST
					.fireEventOnEachListener( persistEvent, copiedAlready,
							PersistEventListener::onPersist );
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
		final var persistEvent = new PersistEvent( entityName, object, this );
		eventListenerGroups.eventListenerGroup_PERSIST_ONFLUSH
				.fireEventOnEachListener( persistEvent, copiedAlready,
						PersistEventListener::onPersist );
		delayedAfterCompletion();
	}

	// merge() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override @SuppressWarnings("unchecked")
	public <T> T merge(String entityName, T object) {
		checkOpen();
		return (T) fireMerge( new MergeEvent( entityName, object, this ) );
	}

	@Override
	public <T> T merge(T object, EntityGraph<?> loadGraph) {
		final var effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		try {
			effectiveEntityGraph.applyGraph( (RootGraphImplementor<?>) loadGraph, GraphSemantic.LOAD );
			return merge( object );
		}
		finally {
			effectiveEntityGraph.clear();
		}
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T merge(T object) {
		checkOpen();
		return (T) fireMerge( new MergeEvent( null, object, this ));
	}

	@Override
	public void merge(String entityName, Object object, MergeContext copiedAlready) {
		checkOpenOrWaitingForAutoClose();
		fireMerge( copiedAlready, new MergeEvent( entityName, object, this ) );
	}

	private Object fireMerge(final MergeEvent mergeEvent) {
		try {
			checkNotReadOnly();
			checkTransactionSynchStatus();
			checkNoUnresolvedActionsBeforeOperation();
			eventListenerGroups.eventListenerGroup_MERGE
					.fireEventOnEachListener( mergeEvent,
							MergeEventListener::onMerge );
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

		return mergeEvent.getResult();
	}

	private void fireMerge(final MergeContext mergeContext, final MergeEvent mergeEvent) {
		try {
			pulseTransactionCoordinator();
			eventListenerGroups.eventListenerGroup_MERGE
					.fireEventOnEachListener( mergeEvent, mergeContext,
							MergeEventListener::onMerge );
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

	@Override
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, DeleteContext transientEntities) {
		checkOpenOrWaitingForAutoClose();
		final boolean removingOrphanBeforeUpdates = persistenceContext.isRemovingOrphanBeforeUpdates();
		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( traceEnabled && removingOrphanBeforeUpdates ) {
			logRemoveOrphanBeforeUpdates( "before continuing", entityName, object );
		}
		fireDelete(
				new DeleteEvent( entityName, object, isCascadeDeleteEnabled, removingOrphanBeforeUpdates, this ),
				transientEntities
		);
		if ( traceEnabled && removingOrphanBeforeUpdates ) {
			logRemoveOrphanBeforeUpdates( "after continuing", entityName, object );
		}
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
		//       This should be removed once action/task ordering is improved.
		final boolean traceEnabled = LOG.isTraceEnabled();
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
		if ( LOG.isTraceEnabled() ) {
			final var entityEntry = persistenceContext.getEntry( entity );
			LOG.tracef(
					"%s remove orphan before updates: [%s]",
					timing,
					entityEntry == null ? entityName : infoString( entityName, entityEntry.getId() )
			);
		}
	}

	private void fireDelete(final DeleteEvent deleteEvent) {
		try {
			checkNotReadOnly();
			pulseTransactionCoordinator();
			eventListenerGroups.eventListenerGroup_DELETE
					.fireEventOnEachListener( deleteEvent,
							DeleteEventListener::onDelete );
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

	private void fireDelete(final DeleteEvent deleteEvent, final DeleteContext transientEntities) {
		try {
			pulseTransactionCoordinator();
			eventListenerGroups.eventListenerGroup_DELETE
					.fireEventOnEachListener( deleteEvent, transientEntities,
							DeleteEventListener::onDelete );
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
	public void load(Object object, Object id) {
		fireLoad( new LoadEvent( id, object, this, getReadOnlyFromLoadQueryInfluencers() ), LoadEventListener.RELOAD );
	}

	private <T> void setMultiIdentifierLoadAccessOptions(FindOption[] options, MultiIdentifierLoadAccess<T> loadAccess) {
		CacheStoreMode storeMode = getCacheStoreMode();
		CacheRetrieveMode retrieveMode = getCacheRetrieveMode();
		LockOptions lockOptions = copySessionLockOptions();
		int batchSize = -1;
		for ( FindOption option : options ) {
			if ( option instanceof CacheStoreMode cacheStoreMode ) {
				storeMode = cacheStoreMode;
			}
			else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
				retrieveMode = cacheRetrieveMode;
			}
			else if ( option instanceof CacheMode cacheMode ) {
				storeMode = cacheMode.getJpaStoreMode();
				retrieveMode = cacheMode.getJpaRetrieveMode();
			}
			else if ( option instanceof LockModeType lockModeType ) {
				lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
			}
			else if ( option instanceof LockMode lockMode ) {
				lockOptions.setLockMode( lockMode );
			}
			else if ( option instanceof LockOptions lockOpts ) {
				lockOptions = lockOpts;
			}
			else if ( option instanceof PessimisticLockScope pessimisticLockScope ) {
				lockOptions.setLockScope( pessimisticLockScope );
			}
			else if ( option instanceof Timeout timeout ) {
				lockOptions.setTimeOut( timeout.milliseconds() );
			}
			else if ( option instanceof EnabledFetchProfile enabledFetchProfile ) {
				loadAccess.enableFetchProfile( enabledFetchProfile.profileName() );
			}
			else if ( option instanceof ReadOnlyMode ) {
				loadAccess.withReadOnly( option == ReadOnlyMode.READ_ONLY );
			}
			else if ( option instanceof BatchSize batchSizeOption ) {
				batchSize = batchSizeOption.batchSize();
			}
		}
		loadAccess.with( lockOptions )
				.with( interpretCacheMode( storeMode, retrieveMode ) )
				.withBatchSize( batchSize );
	}

	@Override
	public <E> List<E> findMultiple(Class<E> entityType, List<?> ids, FindOption... options) {
		final var loadAccess = byMultipleIds( entityType );
		setMultiIdentifierLoadAccessOptions( options, loadAccess );
		return loadAccess.multiLoad( ids );
	}

	@Override
	public <E> List<E> findMultiple(EntityGraph<E> entityGraph, List<?> ids, FindOption... options) {
		final var rootGraph = (RootGraph<E>) entityGraph;
		final var loadAccess = byMultipleIds( rootGraph.getGraphedType().getJavaType() );
		loadAccess.withLoadGraph( rootGraph );
		setMultiIdentifierLoadAccessOptions( options, loadAccess );
		return loadAccess.multiLoad( ids );
	}

	@Override
	public <T> T get(Class<T> entityClass, Object id) {
		return byId( entityClass ).load( id );
	}

	@Override
	public Object get(String entityName, Object id) {
		return byId( entityName ).load( id );
	}

	/**
	 * Load the data for the object with the specified id into a newly created object.
	 * This is only called when lazily initializing a proxy.
	 * Do NOT return a proxy.
	 */
	@Override
	public Object immediateLoad(String entityName, Object id) {
		if ( LOG.isDebugEnabled() ) {
			final var persister = requireEntityPersister( entityName );
			LOG.tracef( "Initializing proxy: %s", infoString( persister, id, getFactory() ) );
		}
		final var event = makeLoadEvent( entityName, id, getReadOnlyFromLoadQueryInfluencers(), true );
		fireLoadNoChecks( event, IMMEDIATE_LOAD );
		final Object result = event.getResult();
		releaseLoadEvent( event );
		final var lazyInitializer = extractLazyInitializer( result );
		return lazyInitializer != null ? lazyInitializer.getImplementation() : result;
	}

	@Override
	public Object internalLoad(String entityName, Object id, boolean eager, boolean nullable) {
		final LoadType type = internalLoadType( eager, nullable );
		final var effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		final var semantic = effectiveEntityGraph.getSemantic();
		final var graph = effectiveEntityGraph.getGraph();
		boolean clearedEffectiveGraph;
		if ( semantic == null
				|| graph.appliesTo( getFactory().getJpaMetamodel().entity( entityName ) ) ) {
			clearedEffectiveGraph = false;
		}
		else {
			LOG.trace( "Clearing effective entity graph for subsequent select" );
			clearedEffectiveGraph = true;
			effectiveEntityGraph.clear();
		}
		try {
			final var event = makeLoadEvent( entityName, id, getReadOnlyFromLoadQueryInfluencers(), true );
			fireLoadNoChecks( event, type );
			final Object result = event.getResult();
			if ( !nullable ) {
				UnresolvableObjectException.throwIfNull( result, id, entityName );
			}
			releaseLoadEvent( event );
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

	@Override
	public Object loadFromSecondLevelCache(
			EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode) {
		final Object entity =
				CacheLoadHelper.loadFromSecondLevelCache( this, instanceToLoad, lockMode, persister, entityKey );
		if ( entity != null ) {
			final Object id = entityKey.getIdentifierValue();
			final var postLoadEvent = makePostLoadEvent( persister, id, entity );
			eventListenerGroups.eventListenerGroup_POST_LOAD
					.fireEventOnEachListener( postLoadEvent,
							PostLoadEventListener::onPostLoad );
			releasePostLoadEvent( postLoadEvent );
		}
		return entity;
	}

	/**
	 * Helper to avoid creating many new instances of {@link PostLoadEvent}.
	 * It's an allocation hot spot.
	 */
	// Hibernate Reactive may need to use this
	protected PostLoadEvent makePostLoadEvent(EntityPersister persister, Object id, Object entity) {
		final var event = postLoadEvent;
		if ( event == null ) {
			return new PostLoadEvent( id, persister, entity, this );
		}
		else {
			postLoadEvent = null;
			event.setId( id );
			event.setPersister( persister );
			event.setEntity( entity );
			return event;
		}
	}

	/**
	 * Helper to avoid creating many new instances of {@link LoadEvent}.
	 * It's an allocation hot spot.
	 */
	// Hibernate Reactive may need to use this
	protected LoadEvent makeLoadEvent(String entityName, Object id, Boolean readOnly, LockOptions lockOptions) {
		final var event = loadEvent;
		if ( event == null ) {
			return new LoadEvent( id, entityName, lockOptions, this, readOnly );
		}
		else {
			loadEvent = null;
			event.setEntityClassName( entityName );
			event.setEntityId( id );
			event.setInstanceToLoad( null );
			event.setReadOnly( readOnly );
			event.setLockOptions( lockOptions );
			event.setAssociationFetch( false );
			return event;
		}
	}

	/**
	 * Helper to avoid creating many new instances of {@link LoadEvent}.
	 * It's an allocation hot spot.
	 */
	// Hibernate Reactive may need to use this
	protected LoadEvent makeLoadEvent(String entityName, Object id, Boolean readOnly, boolean isAssociationFetch) {
		final var event = loadEvent;
		if ( event == null ) {
			return new LoadEvent( id, entityName, isAssociationFetch, this, readOnly );
		}
		else {
			loadEvent = null;
			event.setEntityClassName( entityName );
			event.setEntityId( id );
			event.setInstanceToLoad( null );
			event.setReadOnly( readOnly );
			event.setLockOptions( LockMode.NONE.toLockOptions() );
			event.setAssociationFetch( isAssociationFetch );
			return event;
		}
	}

	// Hibernate Reactive may need to use this
	protected void releasePostLoadEvent(PostLoadEvent event) {
		if ( postLoadEvent == null ) {
			event.setEntity( null );
			event.setId( null );
			event.setPersister( null );
			postLoadEvent = event;
		}
	}

	// Hibernate Reactive may need to use this
	protected void releaseLoadEvent(LoadEvent event) {
		if ( loadEvent == null ) {
			event.setEntityClassName( null );
			event.setEntityId( null );
			event.setInstanceToLoad( null );
			event.setResult( null );
			event.setLockOptions( null );
			event.setReadOnly( null );
			loadEvent = event;
		}
	}

	@Override @Deprecated
	public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
		return this.byId( entityClass ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override @Deprecated
	public Object get(String entityName, Object id, LockMode lockMode) {
		return this.byId( entityName ).with( new LockOptions( lockMode ) ).load( id );
	}

	@Override @Deprecated
	public <T> T get(Class<T> entityClass, Object id, LockOptions lockOptions) {
		return this.byId( entityClass ).with( lockOptions ).load( id );
	}

	@Override @Deprecated
	public Object get(String entityName, Object id, LockOptions lockOptions) {
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
	public Object load(LoadType loadType, Object id, String entityName, LockOptions lockOptions, Boolean readOnly) {
		boolean success = false;
		try {
			final var event =
					makeLoadEvent( entityName, id, readOnly,
							lockOptions == null ? LockOptions.NONE : lockOptions );
			fireLoad( event, loadType );
			final Object result = event.getResult();
			releaseLoadEvent( event );
			if ( !loadType.isAllowNulls() && result == null ) {
				getSession().getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
			}
			success = true;
			return result;
		}
		finally {
			// we might be called from outside transaction
			afterOperation( success );
		}
	}

	private void fireLoad(LoadEvent event, LoadType loadType) {
		checkOpenOrWaitingForAutoClose();
		fireLoadNoChecks( event, loadType );
		delayedAfterCompletion();
	}

	/**
	 * This version of {@link #load} is for use by internal methods only.
	 * It skips the session open check, transaction sync checks, and so on,
	 * which have been shown to be expensive (apparently they prevent these
	 * hot methods from being inlined).
	 */
	private void fireLoadNoChecks(final LoadEvent loadEvent, final LoadType loadType) {
		pulseTransactionCoordinator();
		eventListenerGroups.eventListenerGroup_LOAD
				.fireEventOnEachListener( loadEvent, loadType,
						LoadEventListener::onLoad );
	}


	// refresh() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void refresh(Object object) {
		fireRefresh( new RefreshEvent( object, this ) );
	}

	@Override
	public void refresh(Object object, LockOptions lockOptions) {
		fireRefresh( new RefreshEvent( object, lockOptions, this ) );
	}

	@Override
	public void refresh(String entityName, Object object, RefreshContext refreshedAlready) {
		fireRefresh( refreshedAlready, new RefreshEvent( entityName, object, this ) );
	}

	private void fireRefresh(final RefreshEvent refreshEvent) {
		checkOpen();
		checkEntityManaged( refreshEvent.getEntityName(), refreshEvent.getObject() );
		try {
			pulseTransactionCoordinator();
			checkTransactionNeededForLock( refreshEvent.getLockMode() );
			eventListenerGroups.eventListenerGroup_REFRESH
					.fireEventOnEachListener( refreshEvent,
							RefreshEventListener::onRefresh );
		}
		catch ( RuntimeException e ) {
			convertIfJpaBootstrap( e, refreshEvent.getLockOptions() );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void fireRefresh(final RefreshContext refreshedAlready, final RefreshEvent refreshEvent) {
		// called from cascades
		checkOpenOrWaitingForAutoClose();
		checkEntityManaged( refreshEvent.getEntityName(), refreshEvent.getObject() );
		try {
			pulseTransactionCoordinator();
			eventListenerGroups.eventListenerGroup_REFRESH
					.fireEventOnEachListener( refreshEvent, refreshedAlready,
							RefreshEventListener::onRefresh );
		}
		finally {
			delayedAfterCompletion();
		}
	}

	private void checkEntityManaged(String entityName, Object entity) {
		if ( !managed( entityName, entity ) ) {
			throw new IllegalArgumentException( "Given entity is not associated with the persistence context" );
		}
	}

	private boolean managed(String entityName, Object entity) {
		return entityName == null ? contains( entity ) : contains( entityName, entity );
	}

	// replicate() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void replicate(Object obj, ReplicationMode replicationMode) {
		fireReplicate( new ReplicateEvent( obj, replicationMode, this ) );
	}

	@Override
	public void replicate(String entityName, Object obj, ReplicationMode replicationMode) {
		fireReplicate( new ReplicateEvent( entityName, obj, replicationMode, this ) );
	}

	private void fireReplicate(final ReplicateEvent replicateEvent) {
		checkOpen();
		pulseTransactionCoordinator();
		eventListenerGroups.eventListenerGroup_REPLICATE
				.fireEventOnEachListener( replicateEvent,
						ReplicateEventListener::onReplicate );
		delayedAfterCompletion();
	}


	// evict() operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * remove any hard references to the entity that are held by the infrastructure
	 * (references held by application or other persistent instances are okay)
	 */
	@Override
	public void evict(Object object) {
		checkOpen();
		pulseTransactionCoordinator();
		eventListenerGroups.eventListenerGroup_EVICT
				.fireEventOnEachListener( new EvictEvent( object, this ),
						EvictEventListener::onEvict );
		delayedAfterCompletion();
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces) {
		return autoFlushIfRequired( querySpaces, false );
	}

	@Override
	public boolean autoFlushIfRequired(Set<String> querySpaces, boolean skipPreFlush) {
		checkOpen();
		if ( !isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		final var autoFlushEvent = new AutoFlushEvent( querySpaces, skipPreFlush, this );
		eventListenerGroups.eventListenerGroup_AUTO_FLUSH
				.fireEventOnEachListener( autoFlushEvent,
						AutoFlushEventListener::onAutoFlush );
		return autoFlushEvent.isFlushRequired();
	}

	@Override
	public void autoPreFlush() {
		checkOpen();
		// do not auto-flush while outside a transaction
		if ( isTransactionInProgress() ) {
			eventListenerGroups.eventListenerGroup_AUTO_FLUSH
					.fireEventOnEachListener( this,
							AutoFlushEventListener::onAutoPreFlush );
		}
	}

	@Override
	public boolean isDirty() {
		checkOpen();
		if ( actionQueue.areInsertionsOrDeletionsQueued() ) {
			return true;
		}
		else {
			final var dirtyCheckEvent = new DirtyCheckEvent( this );
			eventListenerGroups.eventListenerGroup_DIRTY_CHECK
					.fireEventOnEachListener( dirtyCheckEvent,
							DirtyCheckEventListener::onDirtyCheck );
			return dirtyCheckEvent.isDirty();
		}
	}

	@Override
	public void flush() {
		checkOpen();
		fireFlush();
	}

	private void fireFlush() {
		if ( !isReadOnly() ) {
			try {
				pulseTransactionCoordinator();
				checkTransactionNeededForUpdateOperation();
				if ( persistenceContext.getCascadeLevel() > 0 ) {
					throw new HibernateException( "Flush during cascade is dangerous" );
				}
				eventListenerGroups.eventListenerGroup_FLUSH
						.fireEventOnEachListener( new FlushEvent( this ),
								FlushEventListener::onFlush );
				delayedAfterCompletion();
			}
			catch (RuntimeException e) {
				throw getExceptionConverter().convert( e );
			}
		}
	}

	@Override
	public void setHibernateFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return flushMode;
	}

	@Override
	public FlushModeType getFlushMode() {
		checkOpen();
		return getFlushModeType( getHibernateFlushMode() );
	}

	@Override
	public void setFlushMode(FlushModeType flushModeType) {
		checkOpen();
		setHibernateFlushMode( FlushModeTypeHelper.getFlushMode( flushModeType ) );
	}

	@Override
	public void forceFlush(EntityEntry entityEntry) {
		forceFlush( entityEntry.getEntityKey() );
	}

	@Override
	public void forceFlush(EntityKey key) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef("Flushing to force deletion of re-saved object: "
					+ infoString( key.getPersister(), key.getIdentifier(), getFactory() ) );
		}

		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ObjectDeletedException(
					"deleted object would be re-saved by cascade (remove deleted object from associations)",
					key.getIdentifier(),
					key.getPersister().getEntityName()
			);
		}
		checkOpenOrWaitingForAutoClose();
		fireFlush();
	}

	@Override @Deprecated
	public Object instantiate(String entityName, Object id) {
		return instantiate( requireEntityPersister( entityName ), id );
	}

	/**
	 * give the interceptor an opportunity to override the default instantiation
	 */
	@Override
	public Object instantiate(EntityPersister persister, Object id) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		Object result =
				getInterceptor()
						.instantiate( persister.getEntityName(), persister.getRepresentationStrategy(), id );
		if ( result == null ) {
			result = persister.instantiate( id, this );
		}
		delayedAfterCompletion();
		return result;
	}

	@Override
	public EntityPersister getEntityPersister(final String entityName, final Object entity) {
		checkOpenOrWaitingForAutoClose();
		if ( entityName == null ) {
			return requireEntityPersister( guessEntityName( entity ) );
		}
		else {
			// try block is a hack around fact that currently tuplizers are
			// not given the opportunity to resolve a subclass entity name.
			// This allows the (we assume custom) interceptor the ability to
			// influence this decision if we were not able to based on the
			// given entityName
			try {
				return requireEntityPersister( entityName )
						.getSubclassEntityPersister( entity, getFactory() );
			}
			catch ( UnknownEntityTypeException uee ) {
				try {
					return getEntityPersister( null, entity );
				}
				catch ( HibernateException e ) {
					final var entityClass = entity.getClass();
					final String problem =
							entityClass.isAnnotationPresent( Entity.class )
									? "does not belong to this persistence unit"
									: "is not annotated '@Entity'";
					throw new UnknownEntityTypeException(
							uee.getMessage()
								+ " ('" + entityClass.getSimpleName() + "' " + problem + ")",
							e
					);
				}
			}
		}
	}

	// not for internal use:
	@Override
	public Object getIdentifier(Object object) {
		checkOpen();
		checkTransactionSynchStatus();
		if ( object == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			checkOwnsProxy( lazyInitializer );
			return lazyInitializer.getInternalIdentifier();
		}
		else {
			return getEntityEntry( object ).getId();
		}
	}

	/**
	 * Get the id value for an object that is actually associated with the session.
	 * This is a bit stricter than
	 * {@link org.hibernate.engine.internal.ForeignKeys#getEntityIdentifierIfNotUnsaved}.
	 */
	@Override
	public Object getContextEntityIdentifier(Object object) {
		checkOpenOrWaitingForAutoClose();
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		else if ( isPersistentAttributeInterceptable( object ) ) {
			if ( asPersistentAttributeInterceptable( object ).$$_hibernate_getInterceptor()
					instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
				return lazinessInterceptor.getIdentifier();
			}
		}

		final var entry = persistenceContext.getEntry( object );
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
			final var lazyInitializer = extractLazyInitializer( object );
			if ( lazyInitializer != null ) {
				// don't use proxiesByKey, since not all
				// proxies that point to this session's
				// instances are in that collection!
				if ( lazyInitializer.isUninitialized() ) {
					// if it's an uninitialized proxy associated
					// with this session, then when it is accessed,
					// the underlying instance will be "contained"
					return lazyInitializer.getSession() == this;
				}
				else {
					// if it's initialized, see if the underlying
					// instance is contained, since we need to
					// account for the fact that it might have been
					// evicted
					object = lazyInitializer.getImplementation();
				}
			}

			// A session is considered to contain an entity only if the entity has
			// an entry in the session's persistence context and the entry reports
			// that the entity has not been removed
			final var entry = persistenceContext.getEntry( object );
			delayedAfterCompletion();

			if ( entry == null ) {
				if ( lazyInitializer == null && persistenceContext.getEntry( object ) == null ) {
					// check if it is even an entity -> if not throw an exception (per JPA)
					try {
						final String entityName = getEntityNameResolver().resolveEntityName( object );
						if ( entityName == null ) {
							throw new IllegalArgumentException( "Could not resolve entity name for class '"
									+ object.getClass() + "'" );
						}
						else {
							requireEntityPersister( entityName );
						}
					}
					catch ( HibernateException e ) {
						throw new IllegalArgumentException( "Class '" + object.getClass()
								+ "' is not an entity class", e );
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
			final var lazyInitializer = extractLazyInitializer( object );
			if ( lazyInitializer == null && persistenceContext.getEntry( object ) == null ) {
				// check if it is an entity -> if not throw an exception (per JPA)
				try {
					requireEntityPersister( entityName );
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
			final var entry = persistenceContext.getEntry( object );
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
	public <T> QueryImplementor<T> createQuery(CriteriaSelect<T> selectQuery) {
		return createQuery( (CriteriaQuery<T>) selectQuery );
	}

	@Override
	public void initializeCollection(PersistentCollection<?> collection, boolean writing) {
		checkOpenOrWaitingForAutoClose();
		pulseTransactionCoordinator();
		eventListenerGroups.eventListenerGroup_INIT_COLLECTION
				.fireEventOnEachListener( new InitializeCollectionEvent( collection, this ),
						InitializeCollectionEventListener::onInitializeCollection );
		delayedAfterCompletion();
	}

	@Override
	public String bestGuessEntityName(Object object) {
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( lazyInitializer.isUninitialized() ) {
				return lazyInitializer.getEntityName();
			}
			object = lazyInitializer.getImplementation();
		}
		final var entry = persistenceContext.getEntry( object );
		return entry == null
				? guessEntityName( object )
				: entry.getPersister().getEntityName();
	}

	@Override
	public String bestGuessEntityName(Object object, EntityEntry entry) {
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			// it is possible for this method to be called during flush processing,
			// so make certain that we do not accidentally initialize an uninitialized proxy
			if ( lazyInitializer.isUninitialized() ) {
				return lazyInitializer.getEntityName();
			}
			object = lazyInitializer.getImplementation();
		}
		return entry == null
				? guessEntityName( object )
				: entry.getPersister().getEntityName();
	}

	@Override
	public String getEntityName(Object object) {
		checkOpen();
//		checkTransactionSynchStatus();

		if ( object == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}

		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			checkOwnsProxy( lazyInitializer );
			object = lazyInitializer.getImplementation();
		}

		return getEntityEntry( object ).getPersister().getEntityName();
	}

	private void checkOwnsProxy(LazyInitializer lazyInitializer) {
		if ( lazyInitializer.getSession() != this ) {
			throw new IllegalArgumentException( "Given proxy is not associated with the persistence context" );
		}
	}

	private EntityEntry getEntityEntry(Object object) {
		final var entry = persistenceContext.getEntry( object );
		if ( entry == null ) {
			throw new IllegalArgumentException( "Given entity is not associated with the persistence context" );
		}
		return entry;
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T getReference(T object) {
		checkOpen();
		final var lazyInitializer = extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			return (T) getReference( lazyInitializer.getPersistentClass(),
					lazyInitializer.getInternalIdentifier() );
		}
		else {
			final var persister = getEntityPersister( null, object );
			return (T) getReference( persister.getMappedClass(),
					persister.getIdentifier(object, this) );
		}
	}

	@Override
	public String guessEntityName(Object object) {
		checkOpenOrWaitingForAutoClose();
		return getEntityNameResolver().resolveEntityName( object );
	}

	@Override
	public void cancelQuery() {
		checkOpen();
		getJdbcCoordinator().cancelLastQuery();
	}

	@Override
	public String toString() {
		final var string =
				new StringBuilder( 500 )
						.append( "SessionImpl(" )
						.append( System.identityHashCode( this ) );
		if ( !isClosed() ) {
			if ( LOG.isTraceEnabled() ) {
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
		if ( !defaultReadOnly && isReadOnly() ) {
			throw new SessionException( "Session was created in read-only mode" );
		}
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
		return Hibernate.getLobHelper();
	}

	@Override
	public void beforeTransactionCompletion() {
		LOG.trace( "SessionImpl#beforeTransactionCompletion()" );
		flushBeforeTransactionCompletion();
		actionQueue.beforeTransactionCompletion();
		beforeTransactionCompletionEvents();
		super.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "SessionImpl#afterTransactionCompletion(successful=%s, delayed=%s)", successful, delayed );
		}

		final boolean notClosed = isOpenOrWaitingForAutoClose();

		if ( notClosed && (!successful || autoClear) ) {
			internalClear();
		}

		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion( successful );

		afterTransactionCompletionEvents( successful );

		if ( !delayed && notClosed && shouldAutoClose() ) {
			managedClose();
		}

		super.afterTransactionCompletion( successful, delayed );
	}

	private static class SharedSessionBuilderImpl
			extends SessionFactoryImpl.SessionBuilderImpl
			implements SharedSessionBuilder, SharedSessionCreationOptions {
		private final SessionImpl session;
		private boolean shareTransactionContext;
		private boolean tenantIdChanged;
		private boolean readOnlyChanged;

		private SharedSessionBuilderImpl(SessionImpl session) {
			super( (SessionFactoryImpl) session.getFactory() );
			this.session = session;
			super.tenantIdentifier( session.getTenantIdentifierValue() );
			super.identifierRollback( session.isIdentifierRollbackEnabled() );
		}

		@Override
		public SessionImpl openSession() {
			if ( session.getSessionFactoryOptions().isMultiTenancyEnabled() ) {
				if ( shareTransactionContext ) {
					if ( tenantIdChanged ) {
						throw new SessionException(
								"Cannot redefine the tenant identifier on a child session if the connection is reused" );
					}
					if ( readOnlyChanged ) {
						throw new SessionException(
								"Cannot redefine the read-only mode on a child session if the connection is reused" );
					}
				}
			}
			return super.openSession();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// SharedSessionBuilder


		@Override @Deprecated(forRemoval = true)
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
		public SharedSessionBuilderImpl readOnly(boolean readOnly) {
			super.readOnly( readOnly );
			readOnlyChanged = true;
			return this;
		}

		@Override
		public SharedSessionBuilderImpl initialCacheMode(CacheMode cacheMode) {
			super.initialCacheMode( cacheMode );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl interceptor() {
			super.interceptor( session.getInterceptor() );
			return this;
		}

		@Override
		public CommonSharedSessionBuilderOptions statementInspector() {
			return statementInspector( session.getJdbcSessionContext().getStatementInspector() );
		}

		@Override
		public CommonSharedSessionBuilderOptions noStatementInspector() {
			statementInspector( StatementInspector.NONE );
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

		private PhysicalConnectionHandlingMode getConnectionHandlingMode() {
			return session.getJdbcCoordinator().getLogicalConnection().getConnectionHandlingMode();
		}

		@Override
		@Deprecated(since = "6.0")
		public SharedSessionBuilderImpl connectionReleaseMode() {
			final PhysicalConnectionHandlingMode handlingMode =
					PhysicalConnectionHandlingMode.interpret( ConnectionAcquisitionMode.AS_NEEDED,
							getConnectionHandlingMode().getReleaseMode() );
			connectionHandlingMode( handlingMode );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl connectionHandlingMode() {
			connectionHandlingMode( getConnectionHandlingMode() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl autoJoinTransactions() {
			super.autoJoinTransactions( session.shouldAutoJoinTransaction() );
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
			autoClose( session.isAutoCloseSessionEnabled() );
			return this;
		}

		@Override
		public SharedSessionBuilderImpl identifierRollback(boolean identifierRollback) {
			super.identifierRollback( identifierRollback );
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

		@Override @Deprecated
		public SharedSessionBuilderImpl statementInspector(StatementInspector statementInspector) {
			super.statementInspector(statementInspector);
			return this;
		}

		@Override
		public SharedSessionBuilderImpl statementInspector(UnaryOperator<String> operator) {
			super.statementInspector(operator);
			return this;
		}

		@Override @Deprecated
		public SharedSessionBuilderImpl connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
			super.connectionHandlingMode(connectionHandlingMode);
			return this;
		}

		@Override @Deprecated
		public SharedSessionBuilderImpl connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
			super.connectionHandling(acquisitionMode, releaseMode);
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
		public Transaction getTransaction() {
			return shareTransactionContext ? session.getCurrentTransaction() : null;
		}

		@Override
		public TransactionCompletionCallbacksImpl getTransactionCompletionCallbacks() {
			return shareTransactionContext
					? session.getActionQueue().getTransactionCompletionCallbacks()
					: null;
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
			return status == TransactionStatus.ACTIVE
				|| status == TransactionStatus.COMMITTING;
		}
	}

	@Override
	public SessionImplementor getSession() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EntityManager impl

	@Override
	public void remove(Object entity) {
		checkOpen();
		try {
			fireDelete( new DeleteEvent( entity, this ) );
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
		return find( entityClass, primaryKey, (LockOptions) null, null );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return find( entityClass, primaryKey, (LockOptions) null, properties );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType) {
		return find( entityClass, primaryKey, lockModeType, null );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		if ( lockModeType == null ) {
			throw new IllegalArgumentException("Given LockModeType was null");
		}
		final var lockMode = LockModeTypeHelper.getLockMode( lockModeType );
		checkTransactionNeededForLock( lockMode );
		return find( entityClass, primaryKey, buildLockOptions( lockMode, properties ), properties );
	}

	private <T> T find(Class<T> entityClass, Object primaryKey, LockOptions lockOptions, Map<String, Object> properties) {
		try {
			loadQueryInfluencers.getEffectiveEntityGraph().applyConfiguredGraph( properties );
			loadQueryInfluencers.setReadOnly( readOnlyHint( properties ) );
			return byId( entityClass )
					.with( determineAppropriateLocalCacheMode( properties ) )
					.with( lockOptions )
					.load( primaryKey );
		}
		catch ( FetchNotFoundException e ) {
			// This may happen if the entity has an associations mapped with
			// @NotFound(action = NotFoundAction.EXCEPTION) and this associated
			// entity is not found
			throw e;
		}
		catch ( EntityFilterException e ) {
			// This may happen if the entity has an associations which is
			// filtered by a FilterDef and this associated entity is not found
			throw e;
		}
		catch ( EntityNotFoundException e ) {
			// We swallow other sorts of EntityNotFoundException and return null
			// For example, DefaultLoadEventListener.proxyImplementation() throws
			// EntityNotFoundException if there's an existing proxy in the session,
			// but the underlying database row has been deleted (see HHH-7861)
			logIgnoringEntityNotFound( entityClass, primaryKey );
			return null;
		}
		catch ( ObjectDeletedException e ) {
			// the spec is silent about people doing remove() find() on the same PC
			return null;
		}
		catch ( ObjectNotFoundException e ) {
			// should not happen on the entity itself with get
			// TODO: in fact this will occur instead of EntityNotFoundException
			//       when using StandardEntityNotFoundDelegate, so probably we
			//       should return null here, as we do above
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( MappingException | TypeMismatchException | ClassCastException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( JDBCException e ) {
			if ( accessTransaction().isActive() && accessTransaction().getRollbackOnly() ) {
				// Assume situation HHH-12472 running on WildFly
				// Just log the exception and return null
				LOG.jdbcExceptionThrownWithTransactionRolledBack( e );
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
		}
	}

	// Hibernate Reactive calls this
	protected static <T> void logIgnoringEntityNotFound(Class<T> entityClass, Object primaryKey) {
		if ( LOG.isDebugEnabled() ) {
			LOG.ignoringEntityNotFound(
					entityClass != null ? entityClass.getName(): null,
					primaryKey != null ? primaryKey.toString() : null
			);
		}
	}

	private <T> void setLoadAccessOptions(FindOption[] options, IdentifierLoadAccessImpl<T> loadAccess) {
		CacheStoreMode storeMode = getCacheStoreMode();
		CacheRetrieveMode retrieveMode = getCacheRetrieveMode();
		LockOptions lockOptions = copySessionLockOptions();
		for ( FindOption option : options ) {
			if ( option instanceof CacheStoreMode cacheStoreMode ) {
				storeMode = cacheStoreMode;
			}
			else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
				retrieveMode = cacheRetrieveMode;
			}
			else if ( option instanceof CacheMode cacheMode ) {
				storeMode = cacheMode.getJpaStoreMode();
				retrieveMode = cacheMode.getJpaRetrieveMode();
			}
			else if ( option instanceof LockModeType lockModeType ) {
				lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
			}
			else if ( option instanceof LockMode lockMode ) {
				lockOptions.setLockMode( lockMode );
			}
			else if ( option instanceof LockOptions lockOpts ) {
				lockOptions = lockOpts;
			}
			else if ( option instanceof Locking.Scope lockScope ) {
				lockOptions.setScope( lockScope );
			}
			else if ( option instanceof PessimisticLockScope pessimisticLockScope ) {
				lockOptions.setScope( Locking.Scope.fromJpaScope( pessimisticLockScope ) );
			}
			else if ( option instanceof Locking.FollowOn followOn ) {
				lockOptions.setFollowOnStrategy( followOn );
			}
			else if ( option instanceof Timeout timeout ) {
				lockOptions.setTimeout( timeout );
			}
			else if ( option instanceof EnabledFetchProfile enabledFetchProfile ) {
				loadAccess.enableFetchProfile( enabledFetchProfile.profileName() );
			}
			else if ( option instanceof ReadOnlyMode ) {
				loadAccess.withReadOnly( option == ReadOnlyMode.READ_ONLY );
			}
		}
		if ( lockOptions.getLockMode().isPessimistic() ) {
			if ( lockOptions.getTimeOut() == WAIT_FOREVER_MILLI ) {
				final Object factoryHint = getFactory().getProperties().get( HINT_SPEC_LOCK_TIMEOUT );
				if ( factoryHint != null ) {
					lockOptions.setTimeOut( Timeouts.fromHint( factoryHint ) );
				}
			}
		}
		loadAccess.with( lockOptions ).with( interpretCacheMode( storeMode, retrieveMode ) );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
		final IdentifierLoadAccessImpl<T> loadAccess = byId( entityClass );
		setLoadAccessOptions( options, loadAccess );
		return loadAccess.load( primaryKey );
	}

	@Override
	public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) {
		final var graph = (RootGraph<T>) entityGraph;
		final var type = graph.getGraphedType();
		final IdentifierLoadAccessImpl<T> loadAccess =
				switch ( type.getRepresentationMode() ) {
					case MAP -> byId( type.getTypeName() );
					case POJO -> byId( type.getJavaType() );
				};
		setLoadAccessOptions( options, loadAccess );
		return loadAccess.withLoadGraph( graph ).load( primaryKey );
	}

	// Hibernate Reactive may need to use this
	protected void checkTransactionNeededForLock(LockMode lockMode) {
		// OPTIMISTIC and OPTIMISTIC_FORCE_INCREMENT require a transaction
		// because they involve a version check at the end of the transaction
		// All flavors of PESSIMISTIC lock also clearly require a transaction
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			checkTransactionNeededForUpdateOperation();
		}
	}

	// Hibernate Reactive may need to use this
	protected static Boolean readOnlyHint(Map<String, Object> properties) {
		if ( properties == null ) {
			return null;
		}
		else {
			final Object value = properties.get( HINT_READ_ONLY );
			return value == null ? null : getBoolean( value );
		}
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
			retrieveMode = getSessionFactoryOptions().getCacheRetrieveMode( properties );
		}
		if ( storeMode == null ) {
			// use the EM setting
			storeMode = getSessionFactoryOptions().getCacheStoreMode( properties );
		}
		return interpretCacheMode( storeMode, retrieveMode );
	}

	private static CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		final CacheRetrieveMode cacheRetrieveMode =
				(CacheRetrieveMode) settings.get( JPA_SHARED_CACHE_RETRIEVE_MODE );
		return cacheRetrieveMode == null
				? (CacheRetrieveMode) settings.get( JAKARTA_SHARED_CACHE_RETRIEVE_MODE )
				: cacheRetrieveMode;
	}

	private static CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		final CacheStoreMode cacheStoreMode =
				(CacheStoreMode) settings.get( JPA_SHARED_CACHE_STORE_MODE );
		return cacheStoreMode == null
				? (CacheStoreMode) settings.get( JAKARTA_SHARED_CACHE_STORE_MODE )
				: cacheStoreMode;
	}

	private void checkTransactionNeededForUpdateOperation() {
		checkTransactionNeededForUpdateOperation( "No active transaction" );
	}

	@Override
	public Object find(String entityName, Object primaryKey) {
		final IdentifierLoadAccessImpl<?> loadAccess = byId( entityName );
		return loadAccess.load( primaryKey );
	}

	@Override
	public Object find(String entityName, Object primaryKey, FindOption... options) {
		final IdentifierLoadAccessImpl<?> loadAccess = byId( entityName );
		setLoadAccessOptions( options, loadAccess );
		return loadAccess.load( primaryKey );
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
		lock( entity, LockModeTypeHelper.getLockMode( lockModeType ) );
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		lock( entity, buildLockOptions( LockModeTypeHelper.getLockMode( lockModeType ), properties ) );
	}

	@Override
	public void lock(Object entity, LockModeType lockModeType, LockOption... options) {
		lock( entity, buildLockOptions( LockModeTypeHelper.getLockMode( lockModeType ), options ) );
	}

	private LockOptions buildLockOptions(LockMode lockMode, LockOption[] options) {
		final var lockOptions = copySessionLockOptions();
		lockOptions.setLockMode( lockMode );
		for ( var option : options ) {
			if ( option instanceof PessimisticLockScope lockScope ) {
				lockOptions.setLockScope( lockScope );
			}
			else if ( option instanceof Timeout timeout ) {
				lockOptions.setTimeOut( timeout.milliseconds() );
			}
		}
		return lockOptions;
	}

	private LockOptions buildLockOptions(LockMode lockMode, Map<String, Object> properties) {
		final var lockOptions = copySessionLockOptions();
		lockOptions.setLockMode( lockMode );
		if ( properties != null ) {
			applyPropertiesToLockOptions( properties, () -> lockOptions );
		}
		return lockOptions;
	}

	private LockOptions copySessionLockOptions() {
		final var copiedLockOptions = new LockOptions();
		if ( lockOptions != null ) {
			LockOptions.copy( lockOptions, copiedLockOptions );
		}
		return copiedLockOptions;
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType) {
		refresh( entity, LockModeTypeHelper.getLockMode( lockModeType ) );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		refresh( entity, null, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		final CacheMode previousCacheMode = getCacheMode();
		final CacheMode refreshCacheMode = determineAppropriateLocalCacheMode( properties );
		try {
			setCacheMode( refreshCacheMode );
			if ( lockModeType == null ) {
				refresh( entity );
			}
			else {
				refresh( entity, buildLockOptions( LockModeTypeHelper.getLockMode( lockModeType ), properties ) );
			}
		}
		finally {
			setCacheMode( previousCacheMode );
		}
	}

	@Override
	public void refresh(Object entity, RefreshOption... options) {
		CacheStoreMode storeMode = getCacheStoreMode();
		LockOptions lockOptions = copySessionLockOptions();
		for ( RefreshOption option : options ) {
			if ( option instanceof CacheStoreMode cacheStoreMode ) {
				storeMode = cacheStoreMode;
			}
			else if ( option instanceof LockModeType lockModeType ) {
				lockOptions.setLockMode( LockModeTypeHelper.getLockMode( lockModeType ) );
			}
			else if ( option instanceof LockMode lockMode ) {
				lockOptions.setLockMode( lockMode );
			}
			else if ( option instanceof LockOptions lockOpts ) {
				lockOptions = lockOpts;
			}
			else if ( option instanceof PessimisticLockScope pessimisticLockScope ) {
				lockOptions.setLockScope( pessimisticLockScope );
			}
			else if ( option instanceof Timeout timeout ) {
				lockOptions.setTimeOut( timeout.milliseconds() );
			}
		}

		final CacheMode previousCacheMode = getCacheMode();
		if ( storeMode != null ) {
			setCacheStoreMode( storeMode );
		}
		try {
			refresh( entity, lockOptions );
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
			throw getExceptionConverter().convert( e );
		}
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		checkOpen();

		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "No active transaction" );
		}

		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		if ( !contains( entity ) ) {
			// convert() calls markForRollbackOnly()
			throw getExceptionConverter()
					.convert( new IllegalArgumentException( "Given entity is not associated with the persistence context" ) );
		}

		return LockModeTypeHelper.getLockModeType( getCurrentLockMode( entity ) );

	}

	@Override
	public void setProperty(String propertyName, Object value) {
		checkOpen();

		if ( !( value instanceof Serializable ) ) {
			LOG.warnf( "Property '%s' is not serializable, value won't be set", propertyName );
			return;
		}

		if ( propertyName == null ) {
			LOG.warn( "Property having key null is illegal, value won't be set" );
			return;
		}

		// store property for future reference:
		if ( properties == null ) {
			properties = computeCurrentProperties();
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
				setSubselectFetchingEnabled( parseBoolean( value.toString() ) );
				break;
			case DEFAULT_BATCH_FETCH_SIZE:
			case HINT_BATCH_FETCH_SIZE:
				setFetchBatchSize( parseInt( value.toString() ) );
				break;
			case STATEMENT_BATCH_SIZE:
			case HINT_JDBC_BATCH_SIZE:
				setJdbcBatchSize( parseInt( value.toString() ) );
				break;
		}
	}

	private Map<String, Object> computeCurrentProperties() {
		final var map = new HashMap<>( getDefaultProperties() );
		//The FLUSH_MODE is always set at Session creation time,
		//so it needs special treatment to not eagerly initialize this Map:
		map.put( HINT_FLUSH_MODE, getHibernateFlushMode() );
		return map;
	}

	@Override
	public Map<String, Object> getProperties() {
		if ( properties == null ) {
			properties = computeCurrentProperties();
		}
		return unmodifiableMap( properties );
	}

	@Override
	public ProcedureCall createNamedStoredProcedureQuery(String name) {
		checkOpen();
		try {
			final var memento =
					getFactory().getQueryEngine().getNamedObjectRepository()
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
	public <T> T unwrap(Class<T> type) {
		checkOpen();

		if ( type.isInstance( this ) ) {
			return type.cast( this );
		}

		if ( type.isInstance( persistenceContext ) ) {
			return type.cast( persistenceContext );
		}

		throw new PersistenceException(
				"Hibernate cannot unwrap '" + getClass().getName() + "' as '" + type.getName() + "'" );
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

	@Override
	public Collection<?> getManagedEntities() {
		return persistenceContext.getEntityHoldersByKey()
				.values().stream().map( EntityHolder::getManagedObject )
				.toList();
	}

	@Override
	public Collection<?> getManagedEntities(String entityName) {
		return persistenceContext.getEntityHoldersByKey().entrySet().stream()
				.filter( entry -> entry.getKey().getEntityName().equals( entityName ) )
				.map( entry -> entry.getValue().getManagedObject() )
				.toList();
	}

	@Override
	public <E> Collection<E> getManagedEntities(Class<E> entityType) {
		return persistenceContext.getEntityHoldersByKey().entrySet().stream()
				.filter( entry -> entry.getKey().getPersister().getMappedClass().equals( entityType ) )
				.map( entry -> (E) entry.getValue().getManagedObject() )
				.toList();
	}

	@Override
	public <E> Collection<E> getManagedEntities(EntityType<E> entityType) {
		final String entityName = ( (EntityDomainType<E>) entityType ).getHibernateEntityName();
		return persistenceContext.getEntityHoldersByKey().entrySet().stream()
				.filter( entry -> entry.getKey().getEntityName().equals( entityName ) )
				.map( entry -> (E) entry.getValue().getManagedObject() )
				.toList();
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param oos The output stream to which we are being written...
	 *
	 * @throws IOException Indicates a general IO stream exception
	 */
	@Serial
	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Serializing Session [%s]", getSessionIdentifier() );
		}

		oos.defaultWriteObject();

		PersistenceContexts.serialize( persistenceContext, oos );
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
	@Serial
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException, SQLException {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Deserializing Session [%s]", getSessionIdentifier() );
		}

		ois.defaultReadObject();

		persistenceContext = PersistenceContexts.deserialize( ois, this );
		actionQueue = ActionQueue.deserialize( ois, this );

		loadQueryInfluencers = (LoadQueryInfluencers) ois.readObject();

		// LoadQueryInfluencers#getEnabledFilters() tries to validate each enabled
		// filter, which will fail when called before FilterImpl#afterDeserialize( factory );
		// Instead lookup the filter by name and then call FilterImpl#afterDeserialize( factory ).
		for ( String filterName : loadQueryInfluencers.getEnabledFilterNames() ) {
			( (FilterImpl) loadQueryInfluencers.getEnabledFilter( filterName ) )
					.afterDeserialize( getFactory() );
		}

		eventListenerGroups = getFactory().getEventListenerGroups();
	}

	// Used by Hibernate reactive
	protected Boolean getReadOnlyFromLoadQueryInfluencers() {
		return loadQueryInfluencers == null ? null : loadQueryInfluencers.getReadOnly();
	}
}
