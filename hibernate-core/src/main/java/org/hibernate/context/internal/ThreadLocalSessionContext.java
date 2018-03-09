/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.internal;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.context.spi.AbstractCurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.stat.SessionStatistics;
import org.jboss.logging.Logger;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Synchronization;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link org.hibernate.context.spi.CurrentSessionContext} impl which scopes the notion of current
 * session by the current thread of execution.  Unlike the JTA counterpart, threads do not give us a nice
 * hook to perform any type of cleanup making it questionable for this impl to actually generate Session
 * instances.  In the interest of usability, it was decided to have this default impl actually generate
 * a session upon first request and then clean it up after the {@link org.hibernate.Transaction}
 * associated with that session is committed/rolled-back.  In order for ensuring that happens, the
 * sessions generated here are unusable until after {@link Session#beginTransaction()} has been
 * called. If <tt>close()</tt> is called on a session managed by this class, it will be automatically
 * unbound.
 *
 * Additionally, the static {@link #bind} and {@link #unbind} methods are provided to allow application
 * code to explicitly control opening and closing of these sessions.  This, with some from of interception,
 * is the preferred approach.  It also allows easy framework integration and one possible approach for
 * implementing long-sessions.
 *
 * The {@link #buildOrObtainSession}, {@link #isAutoCloseEnabled}, {@link #isAutoFlushEnabled},
 * {@link #getConnectionReleaseMode}, and {@link #buildCleanupSynch} methods are all provided to allow easy
 * subclassing (for long-running session scenarios, for example).
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public class ThreadLocalSessionContext extends AbstractCurrentSessionContext {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ThreadLocalSessionContext.class.getName()
	);

	/**
	 * A ThreadLocal maintaining current sessions for the given execution thread.
	 * The actual ThreadLocal variable is a java.util.Map to account for
	 * the possibility for multiple SessionFactory instances being used during execution
	 * of the given thread.
	 */
	private static final ThreadLocal<Map<SessionFactory,Session>> CONTEXT_TL = ThreadLocal.withInitial( HashMap::new );

	/**
	 * Constructs a ThreadLocal
	 *
	 * @param factory The factory this context will service
	 */
	public ThreadLocalSessionContext(SessionFactoryImplementor factory) {
		super( factory );
	}

	@Override
	public final Session currentSession() throws HibernateException {
		Session current = existingSession( factory() );
		if ( current == null ) {
			current = buildOrObtainSession();
			// register a cleanup sync
			current.getTransaction().registerSynchronization( buildCleanupSynch() );
			//we can't trust buildOrObtainSession() so check for wrapping needs
			if ( needsWrapping( current ) ) {
				current = new TransactionProtectionWrappedSession( current );
			}

			// then bind it
			doBind( current, factory() );
		}
		else {
			validateExistingSession( current );
		}
		return current;
	}

	private boolean needsWrapping(Session session) {
		return TransactionProtectionWrappedSession.class.isAssignableFrom( session.getClass() ) == false;
	}

	/**
	 * Getter for property 'factory'.
	 *
	 * @return Value for property 'factory'.
	 */
	protected SessionFactoryImplementor getFactory() {
		return factory();
	}

	/**
	 * Strictly provided for sub-classing purposes; specifically to allow long-session
	 * support.
	 * <p/>
	 * This implementation always just opens a new session.
	 *
	 * @return the built or (re)obtained session.
	 */
	@SuppressWarnings("deprecation")
	protected Session buildOrObtainSession() {
		return baseSessionBuilder()
				.autoClose( isAutoCloseEnabled() )
				.connectionReleaseMode( getConnectionReleaseMode() )
				.flushBeforeCompletion( isAutoFlushEnabled() )
				.openSession();
	}

	protected CleanupSync buildCleanupSynch() {
		return new CleanupSync( factory() );
	}

	/**
	 * Mainly for subclass usage.  This impl always returns true.
	 *
	 * @return Whether or not the the session should be closed by transaction completion.
	 */
	protected boolean isAutoCloseEnabled() {
		return true;
	}

	/**
	 * Mainly for subclass usage.  This impl always returns true.
	 *
	 * @return Whether or not the the session should be flushed prior transaction completion.
	 */
	protected boolean isAutoFlushEnabled() {
		return true;
	}

	/**
	 * Mainly for subclass usage.  This impl always returns after_transaction.
	 *
	 * @return The connection release mode for any built sessions.
	 */
	protected ConnectionReleaseMode getConnectionReleaseMode() {
		return factory().getSettings().getConnectionReleaseMode();
	}

	protected Session wrap(final SessionImplementor session) {
		return new TransactionProtectionWrappedSession( session );
	}

	/**
	 * Associates the given session with the current thread of execution.
	 *
	 * @param session The session to bind.
	 */
	public static void bind(org.hibernate.Session session) {
		final SessionFactory factory = session.getSessionFactory();
		doBind( session, factory );
	}

	private static void terminateOrphanedSession(Session orphan) {
		if ( orphan != null ) {
			LOG.alreadySessionBound();
			try {
				final Transaction orphanTransaction = orphan.getTransaction();
				if ( orphanTransaction != null && orphanTransaction.getStatus() == TransactionStatus.ACTIVE ) {
					try {
						orphanTransaction.rollback();
					}
					catch( Throwable t ) {
						LOG.debug( "Unable to rollback transaction for orphaned session", t );
					}
				}
			}
			finally {
				try {
					orphan.close();
				}
				catch( Throwable t ) {
					LOG.debug( "Unable to close orphaned session", t );
				}
			}

		}
	}

	/**
	 * Disassociates a previously bound session from the current thread of execution.
	 *
	 * @param factory The factory for which the session should be unbound.
	 * @return The session which was unbound.
	 */
	public static Session unbind(SessionFactory factory) {
		return doUnbind( factory, true );
	}

	private static Session existingSession(SessionFactory factory) {
		return sessionMap().get( factory );
	}

	protected static Map<SessionFactory,Session> sessionMap() {
		return CONTEXT_TL.get();
	}

	@SuppressWarnings({"unchecked"})
	private static void doBind(Session session, SessionFactory factory) {
		Session orphanedPreviousSession = sessionMap().put( factory, session );
		terminateOrphanedSession( orphanedPreviousSession );
	}

	private static Session doUnbind(SessionFactory factory, boolean releaseMapIfEmpty) {
		final Map<SessionFactory, Session> sessionMap = sessionMap();
		final Session session = sessionMap.remove( factory );
		if ( releaseMapIfEmpty && sessionMap.isEmpty() ) {
			//Do not use set(null) as it would prevent the initialValue to be invoked again in case of need.
			CONTEXT_TL.remove();
		}
		return session;
	}

	/**
	 * Transaction sync used for cleanup of the internal session map.
	 */
	private static final class CleanupSync implements Synchronization, Serializable {
		protected final SessionFactory factory;

		public CleanupSync(SessionFactory factory) {
			this.factory = factory;
		}

		@Override
		public void beforeCompletion() {
		}

		@Override
		public void afterCompletion(int i) {
			unbind( factory );
		}
	}

	/**
	 * Implementation note: it would be tempting to extend SessionDelegatorBaseImpl but I chose not to
	 * so to make sure no new method is added without drawing attention on it needing ad-hoc
	 * validation rules regarding the transaction been available.
	 */
	private final class TransactionProtectionWrappedSession implements Session, Serializable {

		private final Session wrappedSession;

		/**
		 * This used to be a Proxy to intercept all method calls and apply some strict validation of state.
		 * Rules to be implemented follow;
		 * The following methods need to be handled locally:
		 *  - equals(Object)
		 *  - hashCode
		 *  - toString -> "ThreadLocalSessionContext.TransactionProtectionWrapper[%s]", realSession
		 *  Special rules:
		 *  - close() : unbind( realSession.getSessionFactory() ); [and do not actually close]
		 *  Always valid to invoke:
		 *  - getStatistics
		 *  - isOpen
		 *  - getListeners [no longer exists]
		 *  IF realSession.getTransaction().getStatus() != TransactionStatus.ACTIVE then the following ones are fine to be invoked:
		 *  - beginTransaction
		 *  - getTransaction
		 *  - isTransactionInProgress
		 *  - setFlushMode
		 *  - setFactory
		 *  - getSessionFactory
		 *  - getTenantIdentifier
		 *  - reconnect (Deprecated)
		 *  - disconnect (Deprecated)
		 *  []
		 *  All other methods are INVALID if ( realSession.isOpen() && realSession.getTransaction().getStatus() == TransactionStatus.ACTIVE )
		 *  (no need to prevent any of these on closed session as it will check on that already and throw an appropriate exception)
		 *
		 *  Not valid methods shall throw new HibernateException( methodName + " is not valid without active transaction" );
		 */

		public TransactionProtectionWrappedSession(Session delegate) {
			Objects.requireNonNull( delegate );
			this.wrappedSession = delegate;
		}

		// The protection method, applied on most methods (not all!)

		private final void requireActiveTransaction() {
			//No need to check if the Session is closed as it will already throw an exception
			if ( /* realSession.isClosed() ||*/ wrappedSession.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
				StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
				String methodName = stackTraceElements[0].getMethodName();
				final int startStack = Math.min( 3, stackTraceElements.length );
				final int maxStack = stackTraceElements.length;
				HibernateException error = new HibernateException( "Invoking org.hibernate.Session#" + methodName + " is not valid without an active transaction" );
				error.setStackTrace( Arrays.copyOfRange( stackTraceElements, startStack, maxStack ) );
			}
		}

		// --------- Method implemented locally

		@Override
		public String toString() {
			return "ThreadLocalSessionContext.TransactionProtectionWrapper[" + wrappedSession + ']';
		}

		@Override
		public boolean equals(Object o) {
			return ( this == o );
		}

		@Override
		public int hashCode() {
			return Objects.hash( wrappedSession );
		}

		// ----------- Handle serialization tricks

		private void writeObject(ObjectOutputStream oos) throws IOException {
			// if a ThreadLocalSessionContext-bound session happens to get
			// serialized, to be completely correct, we need to make sure
			// that unbinding of that session occurs.
			oos.defaultWriteObject();
			if ( existingSession( factory() ) == wrappedSession ) {
				unbind( factory() );
			}
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			// on the inverse, it makes sense that if a ThreadLocalSessionContext-
			// bound session then gets deserialized to go ahead and re-bind it to
			// the ThreadLocalSessionContext session map.
			ois.defaultReadObject();
			wrappedSession.getTransaction().registerSynchronization( buildCleanupSynch() );
			doBind( wrappedSession, factory() );
		}

		// close() is the "Special" one:

		@Override
		public void close() {
			unbind( wrappedSession.getSessionFactory() );
		}

		// These are always valid to be invoked:

		@Override
		public SessionStatistics getStatistics() {
			return wrappedSession.getStatistics();
		}

		@Override
		public boolean isOpen() {
			return wrappedSession.isOpen();
		}

		@Override
		public void reconnect(Connection connection) {
			this.wrappedSession.reconnect( connection );
		}

		@Override
		public Transaction beginTransaction() {
			return wrappedSession.beginTransaction();
		}

		@Override
		public Transaction getTransaction() {
			return wrappedSession.getTransaction();
		}

		@Override
		public void setFlushMode(FlushModeType flushMode) {
			wrappedSession.setFlushMode( flushMode );
		}

		@Override
		public void setFlushMode(FlushMode flushMode) {
			wrappedSession.setFlushMode( flushMode );
		}

		@Override
		public SessionFactory getSessionFactory() {
			return wrappedSession.getSessionFactory();
		}

		@Override
		public String getTenantIdentifier() {
			return wrappedSession.getTenantIdentifier();
		}

		// Follows, all methods which need to be checking for an active transaction:

		@Override
		public SharedSessionBuilder sessionWithOptions() {
			requireActiveTransaction();
			return null;
		}

		@Override
		public void flush() throws HibernateException {
			requireActiveTransaction();
			wrappedSession.flush();
		}

		@Override
		public FlushMode getFlushMode$$bridge() {
			requireActiveTransaction();
			//TODO verify that the bridge also transforms bytecode of invokers to invoke the renamed method?
			return wrappedSession.getFlushMode$$bridge();
		}

		@Override
		public FlushModeType getFlushMode() {
			requireActiveTransaction();
			return wrappedSession.getFlushMode();
		}

		@Override
		public void lock(Object entity, LockModeType lockMode) {

		}

		@Override
		public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {

		}

		@Override
		public void setHibernateFlushMode(FlushMode flushMode) {

		}

		@Override
		public FlushMode getHibernateFlushMode() {
			return null;
		}

		@Override
		public void setCacheMode(CacheMode cacheMode) {

		}

		@Override
		public CacheMode getCacheMode() {
			return null;
		}


		@Override
		public void cancelQuery() throws HibernateException {

		}

		@Override
		public boolean isDirty() throws HibernateException {
			return false;
		}

		@Override
		public boolean isDefaultReadOnly() {
			return false;
		}

		@Override
		public void setDefaultReadOnly(boolean readOnly) {

		}

		@Override
		public Serializable getIdentifier(Object object) {
			return null;
		}

		@Override
		public boolean contains(String entityName, Object object) {
			return false;
		}

		@Override
		public void evict(Object object) {

		}

		@Override
		public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
			return null;
		}

		@Override
		public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
			return null;
		}

		@Override
		public Object load(String entityName, Serializable id, LockMode lockMode) {
			return null;
		}

		@Override
		public Object load(String entityName, Serializable id, LockOptions lockOptions) {
			return null;
		}

		@Override
		public <T> T load(Class<T> theClass, Serializable id) {
			return null;
		}

		@Override
		public Object load(String entityName, Serializable id) {
			return null;
		}

		@Override
		public void load(Object object, Serializable id) {

		}

		@Override
		public void replicate(Object object, ReplicationMode replicationMode) {

		}

		@Override
		public void replicate(String entityName, Object object, ReplicationMode replicationMode) {

		}

		@Override
		public Serializable save(Object object) {
			return null;
		}

		@Override
		public Serializable save(String entityName, Object object) {
			return null;
		}

		@Override
		public void saveOrUpdate(Object object) {

		}

		@Override
		public void saveOrUpdate(String entityName, Object object) {

		}

		@Override
		public void update(Object object) {

		}

		@Override
		public void update(String entityName, Object object) {

		}

		@Override
		public Object merge(Object object) {
			return null;
		}

		@Override
		public Object merge(String entityName, Object object) {
			return null;
		}

		@Override
		public void persist(Object object) {

		}

		@Override
		public void remove(Object entity) {

		}

		@Override
		public <T> T find(Class<T> entityClass, Object primaryKey) {
			return null;
		}

		@Override
		public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
			return null;
		}

		@Override
		public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
			return null;
		}

		@Override
		public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
			return null;
		}

		@Override
		public <T> T getReference(Class<T> entityClass, Object primaryKey) {
			return null;
		}

		@Override
		public void persist(String entityName, Object object) {

		}

		@Override
		public void delete(Object object) {

		}

		@Override
		public void delete(String entityName, Object object) {

		}

		@Override
		public void lock(Object object, LockMode lockMode) {

		}

		@Override
		public void lock(String entityName, Object object, LockMode lockMode) {

		}

		@Override
		public LockRequest buildLockRequest(LockOptions lockOptions) {
			return null;
		}

		@Override
		public void refresh(Object object) {

		}

		@Override
		public void refresh(Object entity, Map<String, Object> properties) {

		}

		@Override
		public void refresh(Object entity, LockModeType lockMode) {

		}

		@Override
		public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {

		}

		@Override
		public void refresh(String entityName, Object object) {

		}

		@Override
		public void refresh(Object object, LockMode lockMode) {

		}

		@Override
		public void refresh(Object object, LockOptions lockOptions) {

		}

		@Override
		public void refresh(String entityName, Object object, LockOptions lockOptions) {

		}

		@Override
		public LockMode getCurrentLockMode(Object object) {
			return null;
		}

		@Override
		public Query createFilter(Object collection, String queryString) {
			return null;
		}

		@Override
		public void clear() {

		}

		@Override
		public void detach(Object entity) {

		}

		@Override
		public boolean contains(Object entity) {
			return false;
		}

		@Override
		public LockModeType getLockMode(Object entity) {
			return null;
		}

		@Override
		public void setProperty(String propertyName, Object value) {

		}

		@Override
		public Map<String, Object> getProperties() {
			return null;
		}

		@Override
		public <T> T get(Class<T> entityType, Serializable id) {
			return null;
		}

		@Override
		public <T> T get(Class<T> entityType, Serializable id, LockMode lockMode) {
			return null;
		}

		@Override
		public <T> T get(Class<T> entityType, Serializable id, LockOptions lockOptions) {
			return null;
		}

		@Override
		public Object get(String entityName, Serializable id) {
			return null;
		}

		@Override
		public Object get(String entityName, Serializable id, LockMode lockMode) {
			return null;
		}

		@Override
		public Object get(String entityName, Serializable id, LockOptions lockOptions) {
			return null;
		}

		@Override
		public String getEntityName(Object object) {
			return null;
		}

		@Override
		public IdentifierLoadAccess byId(String entityName) {
			return null;
		}

		@Override
		public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
			return null;
		}

		@Override
		public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
			return null;
		}

		@Override
		public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
			return null;
		}

		@Override
		public NaturalIdLoadAccess byNaturalId(String entityName) {
			return null;
		}

		@Override
		public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
			return null;
		}

		@Override
		public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
			return null;
		}

		@Override
		public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
			return null;
		}

		@Override
		public Filter enableFilter(String filterName) {
			return null;
		}

		@Override
		public Filter getEnabledFilter(String filterName) {
			return null;
		}

		@Override
		public void disableFilter(String filterName) {

		}


		@Override
		public boolean isReadOnly(Object entityOrProxy) {
			return false;
		}

		@Override
		public void setReadOnly(Object entityOrProxy, boolean readOnly) {

		}

		@Override
		public void doWork(Work work) throws HibernateException {

		}

		@Override
		public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
			return null;
		}

		@Override
		public Connection disconnect() {
			return null;
		}


		@Override
		public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
			return false;
		}

		@Override
		public void enableFetchProfile(String name) throws UnknownProfileException {

		}

		@Override
		public void disableFetchProfile(String name) throws UnknownProfileException {

		}

		@Override
		public TypeHelper getTypeHelper() {
			return null;
		}

		@Override
		public LobHelper getLobHelper() {
			return null;
		}

		@Override
		public void addEventListeners(SessionEventListener... listeners) {

		}

		@Override
		public Query getNamedQuery(String queryName) {
			return null;
		}

		@Override
		public Query createQuery(String queryString) {
			return null;
		}

		@Override
		public <T> Query<T> createQuery(String queryString, Class<T> resultType) {
			return null;
		}

		@Override
		public Query createNamedQuery(String name) {
			return null;
		}

		@Override
		public <T> Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
			return null;
		}

		@Override
		public Query createQuery(CriteriaUpdate updateQuery) {
			return null;
		}

		@Override
		public Query createQuery(CriteriaDelete deleteQuery) {
			return null;
		}

		@Override
		public <T> Query<T> createNamedQuery(String name, Class<T> resultType) {
			return null;
		}

		@Override
		public NativeQuery createNativeQuery(String sqlString) {
			return null;
		}

		@Override
		public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
			return null;
		}

		@Override
		public NativeQuery getNamedNativeQuery(String name) {
			return null;
		}

		@Override
		//also satisfies javax.persistence.Query createNativeQuery(String sqlString, Class resultClass)
		public NativeQuery createNativeQuery(String sqlString, Class resultClass) {

			return null;
		}

		@Override
		public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
			return null;
		}

		@Override
		public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
			return null;
		}

		@Override
		public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
			return null;
		}

		@Override
		public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
			return null;
		}

		@Override
		public void joinTransaction() {

		}

		@Override
		public boolean isJoinedToTransaction() {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> cls) {
			return null;
		}

		@Override
		public Object getDelegate() {
			return null;
		}

		@Override
		public EntityManagerFactory getEntityManagerFactory() {
			return null;
		}

		@Override
		public CriteriaBuilder getCriteriaBuilder() {
			return null;
		}

		@Override
		public Metamodel getMetamodel() {
			return null;
		}

		@Override
		public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
			return null;
		}

		@Override
		public EntityGraph<?> createEntityGraph(String graphName) {
			return null;
		}

		@Override
		public EntityGraph<?> getEntityGraph(String graphName) {
			return null;
		}

		@Override
		public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
			return null;
		}

		@Override
		public boolean isConnected() {
			return false;
		}

		@Override
		public ProcedureCall getNamedProcedureCall(String name) {
			return null;
		}

		@Override
		public ProcedureCall createStoredProcedureCall(String procedureName) {
			return null;
		}

		@Override
		public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
			return null;
		}

		@Override
		public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
			return null;
		}

		@Override
		public Criteria createCriteria(Class persistentClass) {
			return null;
		}

		@Override
		public Criteria createCriteria(Class persistentClass, String alias) {
			return null;
		}

		@Override
		public Criteria createCriteria(String entityName) {
			return null;
		}

		@Override
		public Criteria createCriteria(String entityName, String alias) {
			return null;
		}

		@Override
		public Integer getJdbcBatchSize() {
			return null;
		}

		@Override
		public void setJdbcBatchSize(Integer jdbcBatchSize) {

		}

		@Override
		public Session getSession() {
			return null;
		}
	}

}
