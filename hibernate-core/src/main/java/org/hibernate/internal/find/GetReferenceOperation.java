/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import org.hibernate.Incubating;

/// Models the ability to get reference to an entity by a key, either primary or natural.
///
/// @apiNote It is obviously expected that the entity-type and all options are
/// defined on the implementors of this contract.
///
/// @param <T> The entity type.
///
/// @author Steve Ebersole
@Incubating
public interface GetReferenceOperation<T> {
	/// Perform the operation based on the given key.
	///
	/// @param key The primary or natural key by which to get the entity reference.
	T performGetReference(Object key);
}
