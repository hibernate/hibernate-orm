/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

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

	private final Class<? extends SessionEventListener> autoListener;

	public BaselineSessionEventsListenerBuilder(Class<? extends SessionEventListener> autoListener) {
		this.autoListener = autoListener;
	}

	@SuppressWarnings("unused")
	public Class<? extends SessionEventListener> getAutoListener() {
		return autoListener;
	}

	public List<SessionEventListener> buildBaselineList() {
		final SessionEventListener[] sessionEventListeners = buildBaseline();
		//Capacity: needs to hold at least all elements from the baseline, but also expect to add a little more later.
		ArrayList<SessionEventListener> list = new ArrayList<>( sessionEventListeners.length + 3 );
		Collections.addAll( list, sessionEventListeners );
		return list;
	}

	public SessionEventListener[] buildBaseline() {
		final SessionEventListener[] arr;
		if ( autoListener != null ) {
			if ( StatisticalLoggingSessionEventListener.isLoggingEnabled() ) {
				arr = new SessionEventListener[2];
				arr[0] = buildStatsListener();
				arr[1] = buildAutoListener( autoListener );
			}
			else {
				arr = new SessionEventListener[1];
				arr[0] = buildAutoListener( autoListener );
			}
		}
		else {
			if ( StatisticalLoggingSessionEventListener.isLoggingEnabled() ) {
				arr = new SessionEventListener[1];
				arr[0] = buildStatsListener();
			}
			else {
				arr = EMPTY;
			}
		}
		return arr;
	}

	private static SessionEventListener buildAutoListener(final Class<? extends SessionEventListener> autoListener) {
		try {
			return autoListener.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					"Unable to instantiate specified auto SessionEventListener: " + autoListener.getName(),
					e
			);
		}
	}

	private static SessionEventListener buildStatsListener() {
		return new StatisticalLoggingSessionEventListener();
	}

}
