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

@Name( PartialFlushEvent.NAME )
@Label( "PartialFlushEvent Execution" )
@Category( "Hibernate ORM" )
@Description( "PartialFlushEvent Execution" )
@StackTrace(false)
@AllowNonPortable
public class PartialFlushEvent extends Event implements HibernateMonitoringEvent {
	public static final String NAME = "org.hibernate.orm.PartialFlushEvent";

	@Label( "Session Identifier" )
	public String sessionIdentifier;

	@Label( "Number Of Processed Entities" )
	public int numberOfEntitiesProcessed;

	@Label( "Number Of Processed Collections" )
	public int numberOfCollectionsProcessed;

	@Label( "Auto Flush" )
	public boolean isAutoFlush;

	@Override
	public String toString() {
		return NAME ;
	}

}
