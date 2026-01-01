/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

/**
 * Models the ability to load an entity by a key, either primary or natural.
 *
 * @apiNote It is obviously expected that the entity-type and all options are
 * defined on the implementors of this contract.
 *
 * @param <T> The entity type.
 *
 * @author Steve Ebersole
 */
public interface FindByKeyOperation<T> {
	/// Perform the find operation for the given key.
	///
	/// @param key The primary or natural key by which to load the entity.
	T performFind(Object key);
}
