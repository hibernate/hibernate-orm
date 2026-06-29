/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Called before injecting property values into a newly loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractSessionEvent {
	private Object entity;
	private Object[] state;
	private Object id;
	private EntityPersister persister;

	public PreLoadEvent(@Nonnull EventSource session) {
		super(session);
	}

	public void reset() {
		entity = null;
		state = null;
		id = null;
		persister = null;
	}

	public @Nullable Object getEntity() {
		return entity;
	}

	public @Nullable Object getId() {
		return id;
	}

	public @Nullable EntityPersister getPersister() {
		return persister;
	}

	@Nullable
	public Object[] getState() {
		return state;
	}

	public @Nonnull PreLoadEvent setEntity(@Nullable Object entity) {
		this.entity = entity;
		return this;
	}

	public @Nonnull PreLoadEvent setId(@Nullable Object id) {
		this.id = id;
		return this;
	}

	public @Nonnull PreLoadEvent setPersister(@Nullable EntityPersister persister) {
		this.persister = persister;
		return this;
	}

	public @Nonnull PreLoadEvent setState(@Nullable Object[] state) {
		this.state = state;
		return this;
	}
}
