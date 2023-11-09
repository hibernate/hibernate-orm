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

@Name( CachePutEvent.NAME )
@Label( "Cache Put Executed" )
@Category( "Hibernate ORM" )
@Description( "Cache Put Executed" )
@StackTrace(false)
@AllowNonPortable
public class CachePutEvent extends Event implements HibernateEvent {
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

	@Label( "Cache Put Execution Time" )
	public long executionTime;

	@Label( "Description" )
	public String description;

	@Label( "Cache Content Has Changed" )
	public boolean cacheChanged;

	@Override
	public String toString() {
		return NAME ;
	}

	public transient long startedAt;

}
