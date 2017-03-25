/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.transaction.Synchronization;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.AbstractCurrentSessionContext;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.jboss.logging.Logger;

/**
 * A {@link org.hibernate.context.spi.CurrentSessionContext} impl which scopes the notion of current
 * session by the current thread of execution.  Unlike the JTA counterpart, threads do not give us a nice
 * hook to perform any type of cleanup making it questionable for this impl to actually generate Session
 * instances.  In the interest of usability, it was decided to have this default impl actually generate
 * a session upon first request and then clean it up afterQuery the {@link org.hibernate.Transaction}
 * associated with that session is committed/rolled-back.  In order for ensuring that happens, the
 * sessions generated here are unusable until afterQuery {@link Session#beginTransaction()} has been
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
 */
public class ThreadLocalSessionContext extends AbstractCurrentSessionContext {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			ThreadLocalSessionContext.class.getName()
	);

	private static final Class[] SESSION_PROXY_INTERFACES = new Class[] {
			Session.class,
			SessionImplementor.class,
			EventSource.class,
			LobCreationContext.class
	};

	/**
	 * A ThreadLocal maintaining current sessions for the given execution thread.
	 * The actual ThreadLocal variable is a java.util.Map to account for
	 * the possibility for multiple SessionFactory instances being used during execution
	 * of the given thread.
	 */
	private static final ThreadLocal<Map> CONTEXT_TL = new ThreadLocal<Map>();

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
			// wrap the session in the transaction-protection proxy
			if ( needsWrapping( current ) ) {
				current = wrap( current );
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
		// try to make sure we don't wrap and already wrapped session
		if ( session != null ) {
			if ( Proxy.isProxyClass( session.getClass() ) ) {
				final InvocationHandler invocationHandler = Proxy.getInvocationHandler( session );
				if ( invocationHandler != null && TransactionProtectionWrapper.class.isInstance( invocationHandler ) ) {
					return false;
				}
			}
		}
		return true;
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

	protected Session wrap(Session session) {
		final TransactionProtectionWrapper wrapper = new TransactionProtectionWrapper( session );
		final Session wrapped = (Session) Proxy.newProxyInstance(
				Session.class.getClassLoader(),
				SESSION_PROXY_INTERFACES,
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
		final SessionFactory factory = session.getSessionFactory();
		cleanupAnyOrphanedSession( factory );
		doBind( session, factory );
	}

	private static void cleanupAnyOrphanedSession(SessionFactory factory) {
		final Session orphan = doUnbind( factory, false );
		if ( orphan != null ) {
			LOG.alreadySessionBound();
			try {
				if ( orphan.getTransaction() != null && orphan.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
					try {
						orphan.getTransaction().rollback();
					}
					catch( Throwable t ) {
						LOG.debug( "Unable to rollback transaction for orphaned session", t );
					}
				}
				orphan.close();
			}
			catch( Throwable t ) {
				LOG.debug( "Unable to close orphaned session", t );
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
		final Map sessionMap = sessionMap();
		if ( sessionMap == null ) {
			return null;
		}
		return (Session) sessionMap.get( factory );
	}

	protected static Map sessionMap() {
		return CONTEXT_TL.get();
	}

	@SuppressWarnings({"unchecked"})
	private static void doBind(org.hibernate.Session session, SessionFactory factory) {
		Map sessionMap = sessionMap();
		if ( sessionMap == null ) {
			sessionMap = new HashMap();
			CONTEXT_TL.set( sessionMap );
		}
		sessionMap.put( factory, session );
	}

	private static Session doUnbind(SessionFactory factory, boolean releaseMapIfEmpty) {
		Session session = null;
		final Map sessionMap = sessionMap();
		if ( sessionMap != null ) {
			session = (Session) sessionMap.remove( factory );
			if ( releaseMapIfEmpty && sessionMap.isEmpty() ) {
				CONTEXT_TL.set( null );
			}
		}
		return session;
	}

	/**
	 * Transaction sync used for cleanup of the internal session map.
	 */
	protected static class CleanupSync implements Synchronization, Serializable {
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

	private class TransactionProtectionWrapper implements InvocationHandler, Serializable {
		private final Session realSession;
		private Session wrappedSession;

		public TransactionProtectionWrapper(Session realSession) {
			this.realSession = realSession;
		}

		@Override
		@SuppressWarnings("SimplifiableIfStatement")
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String methodName = method.getName();

			// first check methods calls that we handle completely locally:
			if ( "equals".equals( methodName ) && method.getParameterCount() == 1 ) {
				if ( args[0] == null
						|| !Proxy.isProxyClass( args[0].getClass() ) ) {
					return false;
				}
				return this.equals( Proxy.getInvocationHandler( args[0] ) );
			}
			else if ( "hashCode".equals( methodName ) && method.getParameterCount() == 0 ) {
				return this.hashCode();
			}
			else if ( "toString".equals( methodName ) && method.getParameterCount() == 0 ) {
				return String.format( Locale.ROOT, "ThreadLocalSessionContext.TransactionProtectionWrapper[%s]", realSession );
			}


			// then check method calls that we need to delegate to the real Session
			try {
				// If close() is called, guarantee unbind()
				if ( "close".equals( methodName ) ) {
					unbind( realSession.getSessionFactory() );
				}
				else if ( "getStatistics".equals( methodName )
						|| "isOpen".equals( methodName )
						|| "getListeners".equals( methodName ) ) {
					// allow these to go through the the real session no matter what
					LOG.tracef( "Allowing invocation [%s] to proceed to real session", methodName );
				}
				else if ( !realSession.isOpen() ) {
					// essentially, if the real session is closed allow any
					// method call to pass through since the real session
					// will complain by throwing an appropriate exception;
					// NOTE that allowing close() above has the same basic effect,
					//   but we capture that there simply to doAfterTransactionCompletion the unbind...
					LOG.tracef( "Allowing invocation [%s] to proceed to real (closed) session", methodName );
				}
				else if ( realSession.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
					// limit the methods available if no transaction is active
					if ( "beginTransaction".equals( methodName )
							|| "getTransaction".equals( methodName )
							|| "isTransactionInProgress".equals( methodName )
							|| "setFlushMode".equals( methodName )
							|| "getFactory".equals( methodName )
							|| "getSessionFactory".equals( methodName )
							|| "getTenantIdentifier".equals( methodName ) ) {
						LOG.tracef( "Allowing invocation [%s] to proceed to real (non-transacted) session", methodName );
					}
					else if ( "reconnect".equals( methodName ) || "disconnect".equals( methodName ) ) {
						// allow these (deprecated) methods to pass through
						LOG.tracef( "Allowing invocation [%s] to proceed to real (non-transacted) session - deprecated methods", methodName );
					}
					else {
						throw new HibernateException( methodName + " is not valid without active transaction" );
					}
				}
				LOG.tracef( "Allowing proxy invocation [%s] to proceed to real session", methodName );
				return method.invoke( realSession, args );
			}
			catch ( InvocationTargetException e ) {
				if (e.getTargetException() instanceof RuntimeException) {
					throw (RuntimeException)e.getTargetException();
				}
				throw e;
			}
		}

		/**
		 * Setter for property 'wrapped'.
		 *
		 * @param wrapped Value to set for property 'wrapped'.
		 */
		public void setWrapped(Session wrapped) {
			this.wrappedSession = wrapped;
		}


		// serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
			realSession.getTransaction().registerSynchronization( buildCleanupSynch() );
			doBind( wrappedSession, factory() );
		}
	}
}
