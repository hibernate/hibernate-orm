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

@Name( CachePutEvent.NAME )
@Label( "Cache Put Executed" )
@Category( "Hibernate ORM" )
@Description( "Cache Put Executed" )
@StackTrace
@AllowNonPortable
public class CachePutEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.CachePut";

	@Label( "Session Identifier" )
	public String sessionIdentifier;

	@Label( "Region Name" )
	public String regionName;

	@Label( "Entity Name" )
	public String entityName;

	@Label( "Collection Name" )
	public String collectionName;

	@Label( "Used Natural Id" )
	public boolean isNaturalId;

	@Label( "Description" )
	public String description;

	@Label( "Cache Content Has Changed" )
	public boolean cacheChanged;

	@Override
	public String toString() {
		return NAME ;
	}

}
