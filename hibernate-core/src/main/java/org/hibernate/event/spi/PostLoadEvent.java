/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after an entity instance is fully loaded.
 *
 * @author Kabir Khan, Gavin King
 */
public class PostLoadEvent extends AbstractEvent {
	private Object entity;
	private Object id;
	private EntityPersister persister;

	public PostLoadEvent(EventSource session) {
		super(session);
	}

	public void reset() {
		entity = null;
		id = null;
		persister = null;
	}

	public Object getEntity() {
		return entity;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	public Object getId() {
		return id;
	}

	public PostLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}

	public PostLoadEvent setId(Object id) {
		this.id = id;
		return this;
	}

	public PostLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}

}
