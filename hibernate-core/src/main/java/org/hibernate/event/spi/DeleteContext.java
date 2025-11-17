/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.IdentityHashMap;

import org.hibernate.event.internal.DefaultDeleteEventListener;

/**
 * A {@link DeleteEvent} represents a {@linkplain org.hibernate.Session#remove delete operation}
 * applied to a single entity. A {@code DeleteContext} is propagated across all cascaded delete operations,
 * and keeps track of all the entities we've already visited.
 *
 * @see DefaultDeleteEventListener#onDelete(DeleteEvent, DeleteContext)
 *
 * @author Gavin King
 */
public interface DeleteContext {

	boolean add(Object entity);

	static DeleteContext create() {
		// use extension to avoid creating
		// a useless wrapper object
		class Impl extends IdentityHashMap<Object,Object>
				implements DeleteContext {
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
