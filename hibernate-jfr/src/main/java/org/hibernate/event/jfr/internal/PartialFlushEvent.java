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

@Name( PartialFlushEvent.NAME )
@Label( "PartialFlushEvent Execution" )
@Category( "Hibernate ORM" )
@Description( "PartialFlushEvent Execution" )
@StackTrace(false)
@AllowNonPortable
public class PartialFlushEvent extends Event implements HibernateEvent {
	public static final String NAME = "org.hibernate.orm.PartialFlushEvent";

	@Label( "Session Identifier" )
	public String sessionIdentifier;

	@Label( "Number Of Processed Entities" )
	public int numberOfEntitiesProcessed;

	@Label( "Number Of Processed Collectionc" )
	public int numberOfCollectionsProcessed;

	@Label( "PartialFlushEvent time" )
	public long executionTime;

	@Label( "Auto Flush" )
	public boolean isAutoFlush;

	@Override
	public String toString() {
		return NAME ;
	}

	public transient long startedAt;

}
