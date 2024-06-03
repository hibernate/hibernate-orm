/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after the datastore is updated
 * 
 * @author Gavin King
 */
public class PostUpsertEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Object id;
	//list of dirty properties as computed by Hibernate during a FlushEntityEvent
	private final int[] dirtyProperties;

	public PostUpsertEvent(
			Object entity,
			Object id,
			Object[] state,
			int[] dirtyProperties,
			EntityPersister persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.dirtyProperties = dirtyProperties;
		this.persister = persister;
	}
	
	public Object getEntity() {
		return entity;
	}

	public Object getId() {
		return id;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return persister.getFactory();
	}

	public Object[] getState() {
		return state;
	}

	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
