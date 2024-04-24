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
 * Occurs after inserting an item in the datastore
 * 
 * @author Gavin King
 */
public class PostInsertEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Object id;
	
	public PostInsertEvent(
			Object entity,
			Object id,
			Object[] state,
			EntityPersister persister,
			EventSource source) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
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
}
