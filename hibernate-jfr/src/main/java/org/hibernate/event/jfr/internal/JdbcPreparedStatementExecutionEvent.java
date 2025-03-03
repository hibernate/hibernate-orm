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

@Name(JdbcPreparedStatementExecutionEvent.NAME  )
@Label( "JDBC PreparedStatement Executed" )
@Category( "Hibernate ORM" )
@Description( "JDBC PreparedStatement Executed" )
@StackTrace
@AllowNonPortable
public class JdbcPreparedStatementExecutionEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.JdbcPreparedStatementExecution";

	@Label( "PreparedStatement SQL" )
	public String sql;

	@Override
	public String toString() {
		return NAME ;
	}

}
