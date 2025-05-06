/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.internal;

import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.build.AllowNonPortable;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(JdbcConnectionAcquisitionEvent.NAME)
@Label("JDBC Connection Obtained")
@Category("Hibernate ORM")
@Description("JDBC Connection Obtained")
@StackTrace
@AllowNonPortable
public class JdbcConnectionAcquisitionEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.JdbcConnectionAcquisition";

	@Label("Session Identifier")
	public String sessionIdentifier;

	@Label("Tenant Identifier")
	public String tenantIdentifier;

	@Override
	public String toString() {
		return NAME;
	}

}
