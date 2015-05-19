/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.context.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.AbstractCurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Represents a {@link org.hibernate.context.spi.CurrentSessionContext} the notion of a contextual session
 * is managed by some external entity (generally some form of interceptor, etc).
 * This external manager is responsible for scoping these contextual sessions
 * appropriately binding/unbinding them here for exposure to the application
 * through {@link SessionFactory#getCurrentSession} calls.
 * <p/>
 *  Basically exposes two interfaces.  <ul>
 * <li>First is the implementation of CurrentSessionContext which is then used
 * by the {@link SessionFactory#getCurrentSession()} calls.  This
 * portion is instance-based specific to the session factory owning the given
 * instance of this impl (there will be one instance of this per each session
 * factory using this strategy).
 * <li>Second is the externally facing methods {@link #hasBind}, {@link #bind},
 * and {@link #unbind} used by the external thing to manage exposure of the
 * current session it is scoping.  This portion is static to allow easy
 * reference from that external thing.
 * </ul>
 * The underlying storage of the current sessions here is a static
 * {@link ThreadLocal}-based map where the sessions are keyed by the
 * the owning session factory.
 *
 * @author Steve Ebersole
 */
public class ManagedSessionContext extends AbstractCurrentSessionContext {
	private static final ThreadLocal<Map<SessionFactory,Session>> CONTEXT_TL = new ThreadLocal<Map<SessionFactory,Session>>();

	/**
	 * Constructs a new ManagedSessionContext
	 *
	 * @param factory The factory this context will service
	 */
	public ManagedSessionContext(SessionFactoryImplementor factory) {
		super( factory );
	}

	@Override
	public Session currentSession() {
		final Session current = existingSession( factory() );
		if ( current == null ) {
			throw new HibernateException( "No session currently bound to execution context" );
		}
		else {
			validateExistingSession( current );
		}
		return current;
	}

	/**
	 * Check to see if there is already a session associated with the current
	 * thread for the given session factory.
	 *
	 * @param factory The factory against which to check for a given session
	 * within the current thread.
	 * @return True if there is currently a session bound.
	 */
	public static boolean hasBind(SessionFactory factory) {
		return existingSession( factory ) != null;
	}

	/**
	 * Binds the given session to the current context for its session factory.
	 *
	 * @param session The session to be bound.
	 * @return Any previously bound session (should be null in most cases).
	 */
	public static Session bind(Session session) {
		return sessionMap( true ).put( session.getSessionFactory(), session );
	}

	/**
	 * Unbinds the session (if one) current associated with the context for the
	 * given session.
	 *
	 * @param factory The factory for which to unbind the current session.
	 * @return The bound session if one, else null.
	 */
	public static Session unbind(SessionFactory factory) {
		final Map<SessionFactory,Session> sessionMap = sessionMap();
		Session existing = null;
		if ( sessionMap != null ) {
			existing = sessionMap.remove( factory );
			doCleanup();
		}
		return existing;
	}

	private static Session existingSession(SessionFactory factory) {
		final Map sessionMap = sessionMap();
		if ( sessionMap == null ) {
			return null;
		}
		else {
			return (Session) sessionMap.get( factory );
		}
	}

	protected static Map<SessionFactory,Session> sessionMap() {
		return sessionMap( false );
	}

	private static synchronized Map<SessionFactory,Session> sessionMap(boolean createMap) {
		Map<SessionFactory,Session> sessionMap = CONTEXT_TL.get();
		if ( sessionMap == null && createMap ) {
			sessionMap = new HashMap<SessionFactory,Session>();
			CONTEXT_TL.set( sessionMap );
		}
		return sessionMap;
	}

	private static synchronized void doCleanup() {
		final Map<SessionFactory,Session> sessionMap = sessionMap( false );
		if ( sessionMap != null ) {
			if ( sessionMap.isEmpty() ) {
				CONTEXT_TL.set( null );
			}
		}
	}
}
