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

@Name(JdbcPreparedStatementCreationEvent.NAME)
@Label("JDBC PreparedStatement Created")
@Category("Hibernate ORM")
@Description("JDBC PreparedStatement Created")
@StackTrace
@AllowNonPortable
public class JdbcPreparedStatementCreationEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.JdbcPreparedStatementCreation";

	@Label("PreparedStatement SQL")
	public String sql;

	@Override
	public String toString() {
		return NAME;
	}

}
