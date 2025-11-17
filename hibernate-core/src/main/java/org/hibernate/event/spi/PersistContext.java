/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.IdentityHashMap;

/**
 * A {@link PersistEvent} represents a {@linkplain org.hibernate.Session#persist(Object) persist operation}
 * applied to a single entity. A {@code PersistContext} is propagated across all cascaded persist operations,
 * and keeps track of all the entities we've already visited.
 *
 * @author Gavin King
 */
public interface PersistContext {

	boolean add(Object entity);

	static PersistContext create() {
		// use extension to avoid creating
		// a useless wrapper object
		class Impl extends IdentityHashMap<Object,Object>
				implements PersistContext {
			Impl() {
				super(10);
			}

			@Override
			public boolean add(Object entity) {
				return put(entity,entity)==null;
			}
		}
		return new Impl();
	}
}
