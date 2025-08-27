/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Concurrent Map implementation of mappings entity name -> EntityPersister.
 * Concurrency is optimised for read operations; write operations will
 * acquire a lock and are relatively costly: only use for long living,
 * read-mostly use cases.
 * This implementation attempts to avoid type pollution problems.
 */
public final class EntityPersisterConcurrentMap {

	private final ConcurrentHashMap<String,EntityPersisterHolder> map = new ConcurrentHashMap<>();
	private volatile EntityPersister[] values = new EntityPersister[0];
	private volatile String[] keys = new String[0];

	public EntityPersister get(final String name) {
		final EntityPersisterHolder entityPersisterHolder = map.get( name );
		if ( entityPersisterHolder != null ) {
			return entityPersisterHolder.entityPersister;
		}
		return null;
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
		//Assumption: the write lock is being held (synchronize on this)
		final int size = map.size();
		final EntityPersister[] newValues = new EntityPersister[size];
		final String[] newKeys = new String[size];
		int i = 0;
		for ( Map.Entry<String, EntityPersisterHolder> e : map.entrySet() ) {
			newValues[i] = e.getValue().entityPersister;
			newKeys[i] = e.getKey();
			i++;
		}
		this.values = newValues;
		this.keys = newKeys;
	}

	/**
	 * @deprecated Higly inefficient - do not use; this exists
	 * to support other deprecated methods and will be removed.
	 */
	@Deprecated(forRemoval = true)
	public Map<String, EntityPersister> convertToMap() {
		return map.entrySet().stream().collect( Collectors.toUnmodifiableMap(
				Map.Entry::getKey,
				e -> e.getValue().entityPersister
		) );
	}

	/**
	 * Implementation note: since EntityPersister is an highly used
	 * interface, we intentionally avoid using a generic Map referring
	 * to it to avoid type pollution.
	 * Using a concrete holder class bypasses the problem, at a minimal
	 * tradeoff of memory.
	 */
	private final static class EntityPersisterHolder {

		private final EntityPersister entityPersister;

		EntityPersisterHolder(final EntityPersister entityPersister) {
			Objects.requireNonNull( entityPersister );
			this.entityPersister = entityPersister;
		}

	}

}
