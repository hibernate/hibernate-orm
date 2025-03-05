/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractEvent {
	private Object entity;
	private Object[] state;
	private Object id;
	private EntityPersister persister;

	public PreLoadEvent(EventSource session) {
		super(session);
	}

	public void reset() {
		entity = null;
		state = null;
		id = null;
		persister = null;
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

	public Object[] getState() {
		return state;
	}

	public PreLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}

	public PreLoadEvent setId(Object id) {
		this.id = id;
		return this;
	}

	public PreLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}

	public PreLoadEvent setState(Object[] state) {
		this.state = state;
		return this;
	}
}
