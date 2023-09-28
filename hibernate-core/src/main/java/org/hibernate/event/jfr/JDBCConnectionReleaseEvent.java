package org.hibernate.event.jfr;

import org.hibernate.internal.build.AllowNonPortable;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(JDBCConnectionReleaseEvent.NAME  )
@Label( "JDBC Connection Release" )
@Category( "Hibernate ORM" )
@Description( "JDBC Connection Released" )
@StackTrace(false)
@AllowNonPortable
public class JDBCConnectionReleaseEvent extends Event {
	public static final String NAME = "org.hibernate.orm.JDBCConnectionReleased";

	@Override
	public String toString() {
		return NAME ;
	}

}
