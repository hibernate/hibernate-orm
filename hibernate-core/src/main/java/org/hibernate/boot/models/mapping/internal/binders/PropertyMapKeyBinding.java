/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.binders;

/// Pending map-key binding derived from a named property of the map element.
///
/// This is a typed replacement for the old "try later" style of second-pass
/// callback.  Member binding records the collection, target element binder, and
/// target property name.  The collection-index phase resolves it after all entity
/// members have been bound.
///
/// @since 9.0
/// @author Steve Ebersole
public record PropertyMapKeyBinding(
		org.hibernate.mapping.Map collection,
		EntityTypeBinder elementTypeBinder,
		String propertyName) {
}
