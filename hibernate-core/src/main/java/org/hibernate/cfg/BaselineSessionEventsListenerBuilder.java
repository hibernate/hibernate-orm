/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

/**
 * @author Steve Ebersole
 */
public class BaselineSessionEventsListenerBuilder {
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
	public void setLogSessionMetrics(boolean logSessionMetrics) {
		this.logSessionMetrics = logSessionMetrics;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Class<? extends SessionEventListener> getAutoListener() {
		return autoListener;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setAutoListener(Class<? extends SessionEventListener> autoListener) {
		this.autoListener = autoListener;
	}

	public List<SessionEventListener> buildBaselineList() {
		List<SessionEventListener> list = new ArrayList<SessionEventListener>();
		if ( logSessionMetrics && StatisticalLoggingSessionEventListener.isLoggingEnabled() ) {
			list.add( new StatisticalLoggingSessionEventListener() );
		}
		if ( autoListener != null ) {
			try {
				list.add( autoListener.newInstance() );
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to instantiate specified auto SessionEventListener : " + autoListener.getName(),
						e
				);
			}
		}
		return list;
	}
}
