/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.jfr.internal;

import org.hibernate.event.spi.HibernateEvent;
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
@StackTrace(false)
@AllowNonPortable
public class JdbcPreparedStatementExecutionEvent extends Event implements HibernateEvent {
	public static final String NAME = "org.hibernate.orm.JdbcPreparedStatementExecution";

	@Label( "PreparedStatement SQL" )
	public String sql;

	@Label( "PreparedStatement Execution Time" )
	public long executionTime;

	@Override
	public String toString() {
		return NAME ;
	}

	public transient long startedAt;

}
