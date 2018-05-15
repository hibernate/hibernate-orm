/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Occurs after inserting an item in the datastore
 * 
 * @author Gavin King
 */
public class PostInsertEvent extends AbstractEvent {
	private Object entity;
	private EntityTypeDescriptor entityDescriptor;
	private Object[] state;
	private Object id;
	
	public PostInsertEvent(
			Object entity,
			Object id,
			Object[] state,
			EntityTypeDescriptor descriptor,
			EventSource source) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.entityDescriptor = descriptor;
	}
	
	public Object getEntity() {
		return entity;
	}

	public Object getId() {
		return id;
	}

	public EntityTypeDescriptor getDescriptor() {
		return entityDescriptor;
	}

	public Object[] getState() {
		return state;
	}
}
