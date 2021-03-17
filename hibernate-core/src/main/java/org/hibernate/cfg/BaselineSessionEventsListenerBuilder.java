/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

/**
 * @author Steve Ebersole
 */
public class BaselineSessionEventsListenerBuilder {

	private static final SessionEventListener[] EMPTY = new SessionEventListener[0];

	private boolean logSessionMetrics;
	private Class<? extends SessionEventListener> autoListener;

	public BaselineSessionEventsListenerBuilder(
			boolean logSessionMetrics,
			Class<? extends SessionEventListener> autoListener) {
		this.logSessionMetrics = logSessionMetrics;
		this.autoListener = autoListener;
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean isLogSessionMetrics() {
		return logSessionMetrics;
	}

	@SuppressWarnings("UnusedDeclaration")
	/**
	 * @deprecated this method will be removed as this builder should become immutable
	 */
	@Deprecated
	public void setLogSessionMetrics(boolean logSessionMetrics) {
		this.logSessionMetrics = logSessionMetrics;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Class<? extends SessionEventListener> getAutoListener() {
		return autoListener;
	}

	@SuppressWarnings("UnusedDeclaration")
	/**
	 * @deprecated this method will be removed as this builder should become immutable
	 */
	@Deprecated
	public void setAutoListener(Class<? extends SessionEventListener> autoListener) {
		this.autoListener = autoListener;
	}

	public List<SessionEventListener> buildBaselineList() {
		final SessionEventListener[] sessionEventListeners = buildBaseline();
		//Capacity: needs to hold at least all elements from the baseline, but also expect to add a little more later.
		ArrayList<SessionEventListener> list = new ArrayList<>( sessionEventListeners.length + 3 );
		Collections.addAll( list, sessionEventListeners );
		return list;
	}

	public SessionEventListener[] buildBaseline() {
		final boolean addStats = logSessionMetrics && StatisticalLoggingSessionEventListener.isLoggingEnabled();
		final boolean addAutoListener = autoListener != null;
		final SessionEventListener[] arr;
		if ( addStats && addAutoListener ) {
			arr = new SessionEventListener[2];
			arr[0] = buildStatsListener();
			arr[1] = buildAutoListener( autoListener );
		}
		else if ( !addStats && !addAutoListener ) {
			arr = EMPTY;
		}
		else if ( !addStats && addAutoListener ) {
			arr = new SessionEventListener[1];
			arr[0] = buildAutoListener( autoListener );
		}
		else { //Last case: if ( addStats && !addAutoListener )
			arr = new SessionEventListener[1];
			arr[0] = buildStatsListener();
		}
		return arr;
	}

	private static SessionEventListener buildAutoListener(final Class<? extends SessionEventListener> autoListener) {
		try {
			return autoListener.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to instantiate specified auto SessionEventListener : " + autoListener.getName(),
					e
			);
		}
	}

	private static SessionEventListener buildStatsListener() {
		return new StatisticalLoggingSessionEventListener();
	}

}
