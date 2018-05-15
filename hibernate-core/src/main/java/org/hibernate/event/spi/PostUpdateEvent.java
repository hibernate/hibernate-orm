/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Occurs after the datastore is updated
 * 
 * @author Gavin King
 */
public class PostUpdateEvent extends AbstractEvent {
	private Object entity;
	private EntityTypeDescriptor entityDescriptor;
	private Object[] state;
	private Object[] oldState;
	private Object id;
	//list of dirty properties as computed by Hibernate during a FlushEntityEvent
	private final int[] dirtyProperties;
	
	public PostUpdateEvent(
			Object entity,
			Object id,
			Object[] state,
			Object[] oldState,
			int[] dirtyProperties,
			EntityTypeDescriptor descriptor,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.oldState = oldState;
		this.dirtyProperties = dirtyProperties;
		this.entityDescriptor = descriptor;
	}
	
	public Object getEntity() {
		return entity;
	}

	public Object getId() {
		return id;
	}

	public Object[] getOldState() {
		return oldState;
	}

	public EntityTypeDescriptor getDescriptor() {
		return entityDescriptor;
	}

	public Object[] getState() {
		return state;
	}

	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
