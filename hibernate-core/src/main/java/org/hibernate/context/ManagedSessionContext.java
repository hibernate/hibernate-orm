/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.context;

import org.hibernate.classic.Session;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents a {@link CurrentSessionContext} the notion of a contextual session
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
public class ManagedSessionContext implements CurrentSessionContext {

	private static final ThreadLocal context = new ThreadLocal();
	private final SessionFactoryImplementor factory;

	public ManagedSessionContext(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * {@inheritDoc}
	 */
	public Session currentSession() {
		Session current = existingSession( factory );
		if ( current == null ) {
			throw new HibernateException( "No session currently bound to execution context" );
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
		return ( Session ) sessionMap( true ).put( session.getSessionFactory(), session );
	}

	/**
	 * Unbinds the session (if one) current associated with the context for the
	 * given session.
	 *
	 * @param factory The factory for which to unbind the current session.
	 * @return The bound session if one, else null.
	 */
	public static Session unbind(SessionFactory factory) {
		Session existing = null;
		Map sessionMap = sessionMap();
		if ( sessionMap != null ) {
			existing = ( Session ) sessionMap.remove( factory );
			doCleanup();
		}
		return existing;
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
		return sessionMap( false );
	}

	private static synchronized Map sessionMap(boolean createMap) {
		Map sessionMap = ( Map ) context.get();
		if ( sessionMap == null && createMap ) {
			sessionMap = new HashMap();
			context.set( sessionMap );
		}
		return sessionMap;
	}

	private static synchronized void doCleanup() {
		Map sessionMap = sessionMap( false );
		if ( sessionMap != null ) {
			if ( sessionMap.isEmpty() ) {
				context.set( null );
			}
		}
	}
}
