/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;

/**
 * Occurs afterQuery the datastore is updated
 * 
 * @author Gavin King
 */
public class PostUpdateEvent extends AbstractEvent {
	private Object entity;
	private EntityTypeImplementor persister;
	private Object[] state;
	private Object[] oldState;
	private Serializable id;
	//list of dirty properties as computed by Hibernate during a FlushEntityEvent
	private final int[] dirtyProperties;
	
	public PostUpdateEvent(
			Object entity, 
			Serializable id,
			Object[] state,
			Object[] oldState,
			int[] dirtyProperties,
			EntityTypeImplementor persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.oldState = oldState;
		this.dirtyProperties = dirtyProperties;
		this.persister = persister;
	}
	
	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public Object[] getOldState() {
		return oldState;
	}
	public EntityTypeImplementor getPersister() {
		return persister;
	}
	public Object[] getState() {
		return state;
	}

	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
