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

@Name(JdbcConnectionReleaseEvent.NAME)
@Label("JDBC Connection Release")
@Category("Hibernate ORM")
@Description("JDBC Connection Released")
@StackTrace(false)
@AllowNonPortable
public class JdbcConnectionReleaseEvent extends Event implements HibernateEvent {
	public static final String NAME = "org.hibernate.orm.JdbcConnectionRelease";

	@Label("Session Identifier")
	public String sessionIdentifier;

	@Label("Tenant Identifier")
	public String tenantIdentifier;

	@Label("Connection Release Time")
	public long executionTime;

	@Override
	public String toString() {
		return NAME;
	}

	public transient long startedAt;

}
