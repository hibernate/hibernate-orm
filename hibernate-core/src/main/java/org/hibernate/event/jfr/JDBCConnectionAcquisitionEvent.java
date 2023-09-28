package org.hibernate.event.jfr;

import org.hibernate.internal.build.AllowNonPortable;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(JDBCConnectionAcquisitionEvent.NAME  )
@Label( "JDBC Connection Obtained" )
@Category( "Hibernate ORM" )
@Description( "JDBC Connection Obtained" )
@StackTrace(false)
@AllowNonPortable
public class JDBCConnectionAcquisitionEvent extends Event {
	public static final String NAME = "org.hibernate.orm.JDBCConnectionAcquired";

	@Override
	public String toString() {
		return NAME ;
	}

}
