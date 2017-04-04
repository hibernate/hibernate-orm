/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.ReplicationMode;

/**
 *  Defines an event class for the replication of an entity.
 *
 * @author Steve Ebersole
 */
public class ReplicateEvent extends AbstractEvent {
	private Object object;
	private ReplicationMode replicationMode;
	private String entityName;

	public ReplicateEvent(Object object, ReplicationMode replicationMode, EventSource source) {
		this(null, object, replicationMode, source);
	}
	
	public ReplicateEvent(String entityName, Object object, ReplicationMode replicationMode, EventSource source) {
		super(source);
		this.entityName = entityName;

		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create replication strategy with null entity"
			);
		}
		if ( replicationMode == null ) {
			throw new IllegalArgumentException(
					"attempt to create replication strategy with null replication mode"
			);
		}

		this.object = object;
		this.replicationMode = replicationMode;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public ReplicationMode getReplicationMode() {
		return replicationMode;
	}

	public void setReplicationMode(ReplicationMode replicationMode) {
		this.replicationMode = replicationMode;
	}

	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
