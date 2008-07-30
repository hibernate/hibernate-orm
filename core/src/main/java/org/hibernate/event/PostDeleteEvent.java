/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after deleting an item from the datastore
 * 
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Serializable id;
	private Object[] deletedState;
	
	public PostDeleteEvent(
			Object entity, 
			Serializable id,
			Object[] deletedState,
			EntityPersister persister,
			EventSource source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.persister = persister;
		this.deletedState = deletedState;
	}
	
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object getEntity() {
		return entity;
	}
	public Object[] getDeletedState() {
		return deletedState;
	}
}
