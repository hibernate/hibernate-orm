/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Occurs after deleting an item from the datastore
 * 
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityTypeDescriptor entityDescriptor;
	private Object id;
	private Object[] deletedState;
	
	public PostDeleteEvent(
			Object entity,
			Object id,
			Object[] deletedState,
			EntityTypeDescriptor descriptor,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.entityDescriptor = descriptor;
		this.deletedState = deletedState;
	}
	
	public Object getId() {
		return id;
	}

	public EntityTypeDescriptor getDescriptor() {
		return entityDescriptor;
	}

	public Object getEntity() {
		return entity;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}
}
