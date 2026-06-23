/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Occurs after an entity instance is fully loaded.
 *
 * @author Kabir Khan, Gavin King
 */
public class PostLoadEvent extends AbstractSessionEvent {
	private Object entity;
	private Object id;
	private EntityPersister persister;

	public PostLoadEvent(@Nonnull EventSource session) {
		super(session);
	}

	public PostLoadEvent(@Nullable Object id, @Nullable EntityPersister persister, @Nullable Object entity, @Nonnull EventSource session) {
		super(session);
		this.id = id;
		this.persister = persister;
		this.entity = entity;
	}

	public void reset() {
		entity = null;
		id = null;
		persister = null;
	}

	public @Nullable Object getEntity() {
		return entity;
	}

	public @Nullable EntityPersister getPersister() {
		return persister;
	}

	public @Nullable Object getId() {
		return id;
	}

	public @Nonnull PostLoadEvent setEntity(@Nullable Object entity) {
		this.entity = entity;
		return this;
	}

	public @Nonnull PostLoadEvent setId(@Nullable Object id) {
		this.id = id;
		return this;
	}

	public @Nonnull PostLoadEvent setPersister(@Nullable EntityPersister persister) {
		this.persister = persister;
		return this;
	}

}
