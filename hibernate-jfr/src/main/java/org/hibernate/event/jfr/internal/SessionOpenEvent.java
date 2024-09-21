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
@Name(SessionOpenEvent.NAME)
@Label("Session Opened")
@Category("Hibernate ORM")
@Description("Hibernate Session opened")
@StackTrace(false)
@AllowNonPortable
public class SessionOpenEvent extends Event implements HibernateMonitoringEvent {
	public static final String NAME = "org.hibernate.orm.SessionOpen";

	@Label("Session Identifier" )
	public String sessionIdentifier;

	@Override
	public String toString() {
		return NAME + "(" + sessionIdentifier + ")";
	}
}
