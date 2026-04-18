/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityEntryRef;

import java.util.function.Supplier;

public class EntityEntryRefImpl implements EntityEntryRef {

	private final Object entity;
	private final Supplier<EntityEntry> entityEntry;

	public EntityEntryRefImpl(Object entity, Supplier<EntityEntry> entityEntry) {
		this.entity = entity;
		this.entityEntry = entityEntry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getEntity() {
		return entity;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EntityEntry getEntityEntry() {
		return entityEntry.get();
	}

}
