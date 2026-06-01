/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Concurrent Map implementation of mappings entity name to EntityPersister.
 * Concurrency is optimized for read operations; write operations will
 * acquire a lock and are relatively costly: only use for long living,
 * read-mostly use cases.
 * This implementation attempts to avoid type pollution problems.
 */
public final class EntityPersisterConcurrentMap {

	private final ConcurrentHashMap<String,EntityPersisterHolder> map = new ConcurrentHashMap<>();
	private volatile EntityPersister[] values = new EntityPersister[0];
	private volatile String[] keys = new String[0];

	public EntityPersister get(final String name) {
		final var entityPersisterHolder = map.get( name );
		return entityPersisterHolder == null ? null : entityPersisterHolder.entityPersister;
	}

	public EntityPersister[] values() {
		return values;
	}

	public synchronized void put(final String name, final EntityPersister entityPersister) {
		map.put( name, new EntityPersisterHolder( entityPersister ) );
		recomputeValues();
	}

	public synchronized void putIfAbsent(final String name, final EntityPersister entityPersister) {
		map.putIfAbsent( name, new EntityPersisterHolder( entityPersister ) );
		recomputeValues();
	}

	public boolean containsKey(final String name) {
		return map.containsKey( name );
	}

	public String[] keys() {
		return keys;
	}

	private void recomputeValues() {
		//Assumption: the write lock is held (synchronize on this)
		final int size = map.size();
		final var newValues = new EntityPersister[size];
		final var newKeys = new String[size];
		int i = 0;
		for ( var entry : map.entrySet() ) {
			newValues[i] = entry.getValue().entityPersister;
			newKeys[i] = entry.getKey();
			i++;
		}
		values = newValues;
		keys = newKeys;
	}

	/**
	 * Implementation note: since EntityPersister is an highly used
	 * interface, we intentionally avoid using a generic Map referring
	 * to it to avoid type pollution.
	 * Using a concrete holder class bypasses the problem, at a minimal
	 * tradeoff of memory.
	 */
	private record EntityPersisterHolder(EntityPersister entityPersister) {
		private EntityPersisterHolder {
			Objects.requireNonNull( entityPersister );
		}
	}
}
