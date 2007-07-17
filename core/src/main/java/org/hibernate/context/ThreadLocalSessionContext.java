package org.hibernate.context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * A {@link CurrentSessionContext} impl which scopes the notion of current
 * session by the current thread of execution.  Unlike the JTA counterpart,
 * threads do not give us a nice hook to perform any type of cleanup making
 * it questionable for this impl to actually generate Session instances.  In
 * the interest of usability, it was decided to have this default impl
 * actually generate a session upon first request and then clean it up
 * after the {@link org.hibernate.Transaction} associated with that session
 * is committed/rolled-back.  In order for ensuring that happens, the sessions
 * generated here are unusable until after {@link Session#beginTransaction()}
 * has been called. If <tt>close()</tt> is called on a session managed by
 * this class, it will be automatically unbound.
 * <p/>
 * Additionally, the static {@link #bind} and {@link #unbind} methods are
 * provided to allow application code to explicitly control opening and
 * closing of these sessions.  This, with some from of interception,
 * is the preferred approach.  It also allows easy framework integration
 * and one possible approach for implementing long-sessions.
 * <p/>
 * The {@link #buildOrObtainSession}, {@link #isAutoCloseEnabled},
 * {@link #isAutoFlushEnabled}, {@link #getConnectionReleaseMode}, and
 * {@link #buildCleanupSynch} methods are all provided to allow easy
 * subclassing (for long- running session scenarios, for example).
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class ThreadLocalSessionContext implements CurrentSessionContext {

	private static final Log log = LogFactory.getLog( ThreadLocalSessionContext.class );
	private static final Class[] SESS_PROXY_INTERFACES = new Class[] {
			org.hibernate.classic.Session.class,
	        org.hibernate.engine.SessionImplementor.class,
	        org.hibernate.jdbc.JDBCContext.Context.class,
	        org.hibernate.event.EventSource.class
	};

	/**
	 * A ThreadLocal maintaining current sessions for the given execution thread.
	 * The actual ThreadLocal variable is a java.util.Map to account for
	 * the possibility for multiple SessionFactorys being used during execution
	 * of the given thread.
	 */
	private static final ThreadLocal context = new ThreadLocal();

	protected final SessionFactoryImplementor factory;

	public ThreadLocalSessionContext(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public final Session currentSession() throws HibernateException {
		Session current = existingSession( factory );
		if (current == null) {
			current = buildOrObtainSession();
			// register a cleanup synch
			current.getTransaction().registerSynchronization( buildCleanupSynch() );
			// wrap the session in the transaction-protection proxy
			if ( needsWrapping( current ) ) {
				current = wrap( current );
			}
			// then bind it
			doBind( current, factory );
		}
		return current;
	}

	private boolean needsWrapping(Session session) {
		// try to make sure we don't wrap and already wrapped session
		return session != null
		       && ! Proxy.isProxyClass( session.getClass() )
		       || ( Proxy.getInvocationHandler( session ) != null
		       && ! ( Proxy.getInvocationHandler( session ) instanceof TransactionProtectionWrapper ) );
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	/**
	 * Strictly provided for subclassing purposes; specifically to allow long-session
	 * support.
	 * <p/>
	 * This implementation always just opens a new session.
	 *
	 * @return the built or (re)obtained session.
	 */
	protected Session buildOrObtainSession() {
		return factory.openSession(
				null,
		        isAutoFlushEnabled(),
		        isAutoCloseEnabled(),
		        getConnectionReleaseMode()
			);
	}

	protected CleanupSynch buildCleanupSynch() {
		return new CleanupSynch( factory );
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
		return factory.getSettings().getConnectionReleaseMode();
	}

	protected Session wrap(Session session) {
		TransactionProtectionWrapper wrapper = new TransactionProtectionWrapper( session );
		Session wrapped = ( Session ) Proxy.newProxyInstance(
				Session.class.getClassLoader(),
		        SESS_PROXY_INTERFACES,
		        wrapper
			);
		// yick!  need this for proper serialization/deserialization handling...
		wrapper.setWrapped( wrapped );
		return wrapped;
	}

	/**
	 * Associates the given session with the current thread of execution.
	 *
	 * @param session The session to bind.
	 */
	public static void bind(org.hibernate.Session session) {
		SessionFactory factory = session.getSessionFactory();
		cleanupAnyOrphanedSession( factory );
		doBind( session, factory );
	}

	private static void cleanupAnyOrphanedSession(SessionFactory factory) {
		Session orphan = doUnbind( factory, false );
		if ( orphan != null ) {
			log.warn( "Already session bound on call to bind(); make sure you clean up your sessions!" );
			try {
				if ( orphan.getTransaction() != null && orphan.getTransaction().isActive() ) {
					try {
						orphan.getTransaction().rollback();
					}
					catch( Throwable t ) {
						log.debug( "Unable to rollback transaction for orphaned session", t );
					}
				}
				orphan.close();
			}
			catch( Throwable t ) {
				log.debug( "Unable to close orphaned session", t );
			}
		}
	}

	/**
	 * Unassociate a previously bound session from the current thread of execution.
	 *
	 * @return The session which was unbound.
	 */
	public static Session unbind(SessionFactory factory) {
		return doUnbind( factory, true );
	}

	private static Session existingSession(SessionFactory factory) {
		Map sessionMap = sessionMap();
		if ( sessionMap == null ) {
			return null;
		}
		else {
			return ( Session ) sessionMap.get( factory );
		}
	}

	protected static Map sessionMap() {
		return ( Map ) context.get();
	}

	private static void doBind(org.hibernate.Session session, SessionFactory factory) {
		Map sessionMap = sessionMap();
		if ( sessionMap == null ) {
			sessionMap = new HashMap();
			context.set( sessionMap );
		}
		sessionMap.put( factory, session );
	}

	private static Session doUnbind(SessionFactory factory, boolean releaseMapIfEmpty) {
		Map sessionMap = sessionMap();
		Session session = null;
		if ( sessionMap != null ) {
			session = ( Session ) sessionMap.remove( factory );
			if ( releaseMapIfEmpty && sessionMap.isEmpty() ) {
				context.set( null );
			}
		}
		return session;
	}

	/**
	 * JTA transaction synch used for cleanup of the internal session map.
	 */
	protected static class CleanupSynch implements Synchronization, Serializable {
		protected final SessionFactory factory;

		public CleanupSynch(SessionFactory factory) {
			this.factory = factory;
		}

		public void beforeCompletion() {
		}

		public void afterCompletion(int i) {
			unbind( factory );
		}
	}

	private class TransactionProtectionWrapper implements InvocationHandler, Serializable {
		private final Session realSession;
		private Session wrappedSession;

		public TransactionProtectionWrapper(Session realSession) {
			this.realSession = realSession;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				// If close() is called, guarantee unbind()
				if ( "close".equals( method.getName()) ) {
					unbind( realSession.getSessionFactory() );
				}
				else if ( "toString".equals( method.getName() )
					     || "equals".equals( method.getName() )
					     || "hashCode".equals( method.getName() )
				         || "getStatistics".equals( method.getName() )
					     || "isOpen".equals( method.getName() ) ) {
					// allow these to go through the the real session no matter what
				}
				else if ( !realSession.isOpen() ) {
					// essentially, if the real session is closed allow any
					// method call to pass through since the real session
					// will complain by throwing an appropriate exception;
					// NOTE that allowing close() above has the same basic effect,
					//   but we capture that there simply to perform the unbind...
				}
				else if ( !realSession.getTransaction().isActive() ) {
					// limit the methods available if no transaction is active
					if ( "beginTransaction".equals( method.getName() )
					     || "getTransaction".equals( method.getName() )
					     || "isTransactionInProgress".equals( method.getName() )
					     || "setFlushMode".equals( method.getName() )
					     || "getSessionFactory".equals( method.getName() ) ) {
						log.trace( "allowing method [" + method.getName() + "] in non-transacted context" );
					}
					else if ( "reconnect".equals( method.getName() )
					          || "disconnect".equals( method.getName() ) ) {
						// allow these (deprecated) methods to pass through
					}
					else {
						throw new HibernateException( method.getName() + " is not valid without active transaction" );
					}
				}
				log.trace( "allowing proxied method [" + method.getName() + "] to proceed to real session" );
				return method.invoke( realSession, args );
			}
			catch ( InvocationTargetException e ) {
				if ( e.getTargetException() instanceof RuntimeException ) {
					throw ( RuntimeException ) e.getTargetException();
				}
				else {
					throw e;
				}
			}
		}

		public void setWrapped(Session wrapped) {
			this.wrappedSession = wrapped;
		}


		// serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		private void writeObject(ObjectOutputStream oos) throws IOException {
			// if a ThreadLocalSessionContext-bound session happens to get
			// serialized, to be completely correct, we need to make sure
			// that unbinding of that session occurs.
			oos.defaultWriteObject();
			if ( existingSession( factory ) == wrappedSession ) {
				unbind( factory );
			}
		}

		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			// on the inverse, it makes sense that if a ThreadLocalSessionContext-
			// bound session then gets deserialized to go ahead and re-bind it to
			// the ThreadLocalSessionContext session map.
			ois.defaultReadObject();
			realSession.getTransaction().registerSynchronization( buildCleanupSynch() );
			doBind( wrappedSession, factory );
		}
	}
}
