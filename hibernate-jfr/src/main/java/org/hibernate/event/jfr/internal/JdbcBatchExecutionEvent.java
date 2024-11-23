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

@Name(JdbcBatchExecutionEvent.NAME)
@Label("JDBC Batch Execution")
@Category("Hibernate ORM")
@Description("JDBC Batch Execution")
@StackTrace(false)
@AllowNonPortable
public class JdbcBatchExecutionEvent extends Event implements HibernateMonitoringEvent {
	public static final String NAME = "org.hibernate.orm.JdbcBatchExecution";

	@Label("PreparedStatement SQL")
	public String sql;

	@Override
	public String toString() {
		return NAME;
	}

}
