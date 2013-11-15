/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
