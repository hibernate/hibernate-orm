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

@Name( CacheGetEvent.NAME )
@Label( "Cache Get Executed" )
@Category( "Hibernate ORM" )
@Description( "Cache Get Executed" )
@StackTrace
@AllowNonPortable
public class CacheGetEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.CacheGet";

	@Label( "Session Identifier" )
	public String sessionIdentifier;

	@Label( "Entity Name" )
	public String entityName;

	@Label( "Collection Name" )
	public String collectionName;

	@Label( "Used Natural Id" )
	public boolean isNaturalId;

	@Label( "Region Name" )
	public String regionName;

	@Label("Cache Hit")
	public boolean hit;

	@Override
	public String toString() {
		return NAME ;
	}

}
