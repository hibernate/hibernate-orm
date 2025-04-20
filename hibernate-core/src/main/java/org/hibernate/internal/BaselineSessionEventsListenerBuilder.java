/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;

import static java.util.Collections.addAll;

/**
 * @author Steve Ebersole
 *
 * @apiNote Due to a mistake, this internal class was exposed via the layer-breaking operation
 * {@link org.hibernate.boot.spi.SessionFactoryOptions#getBaselineSessionEventsListenerBuilder()}.
 * Clients should avoid direct use of this class.
 *
 * @deprecated This class is no longer needed and will be removed.
 */
@Deprecated(since = "7.0", forRemoval = true)
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
		// Capacity: needs to hold at least all elements from the baseline,
		//           but also expect to add a little more later.
		final List<SessionEventListener> list =
				new ArrayList<>( sessionEventListeners.length + 3 );
		addAll( list, sessionEventListeners );
		return list;
	}

	public SessionEventListener[] buildBaseline() {
		if ( StatisticalLoggingSessionEventListener.isLoggingEnabled() ) {
			return autoListener == null
					? new SessionEventListener[] { statsListener() }
					: new SessionEventListener[] { statsListener(), autoListener() };
		}
		else {
			return autoListener == null
					? EMPTY
					: new SessionEventListener[] { autoListener() };
		}
	}

	private SessionEventListener autoListener() {
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

	private static SessionEventListener statsListener() {
		return new StatisticalLoggingSessionEventListener();
	}

}
