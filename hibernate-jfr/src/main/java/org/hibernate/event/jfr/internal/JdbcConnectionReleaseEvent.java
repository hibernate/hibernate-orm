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

@Name(JdbcConnectionReleaseEvent.NAME)
@Label("JDBC Connection Release")
@Category("Hibernate ORM")
@Description("JDBC Connection Released")
@StackTrace(false)
@AllowNonPortable
public class JdbcConnectionReleaseEvent extends Event implements HibernateMonitoringEvent {
	public static final String NAME = "org.hibernate.orm.JdbcConnectionRelease";

	@Label("Session Identifier")
	public String sessionIdentifier;

	@Label("Tenant Identifier")
	public String tenantIdentifier;

	@Override
	public String toString() {
		return NAME;
	}

}
