/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.internal;

import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.internal.build.AllowNonPortable;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * @author Steve Ebersole
 */
@Name(SessionClosedEvent.NAME)
@Label("Session Closed")
@Category("Hibernate ORM")
@Description("Hibernate Session closed")
@StackTrace(false)
@AllowNonPortable
public class SessionClosedEvent extends Event implements HibernateMonitoringEvent {
	public static final String NAME = "org.hibernate.orm.SessionClosed";

	@Label("Session Identifier" )
	public String sessionIdentifier;

	@Override
	public String toString() {
		return NAME + "(" + sessionIdentifier + ")";
	}
}
