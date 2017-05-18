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
 * Occurs afterQuery inserting an item in the datastore
 * 
 * @author Gavin King
 */
public class PostInsertEvent extends AbstractEvent {
	private Object entity;
	private EntityTypeImplementor persister;
	private Object[] state;
	private Serializable id;
	
	public PostInsertEvent(
			Object entity, 
			Serializable id,
			Object[] state,
			EntityTypeImplementor persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.persister = persister;
	}
	
	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public EntityTypeImplementor getPersister() {
		return persister;
	}
	public Object[] getState() {
		return state;
	}
}
