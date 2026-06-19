/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import org.hibernate.models.spi.ClassDetails;

/// Categorized representation of attributes that identify instances in an entity
/// hierarchy.
///
/// A key mapping may describe the primary identifier or another key-like grouping
/// such as a natural id.  Implementations describe whether the key is represented
/// by one persistent attribute, an embedded id, or multiple id attributes paired
/// with an id-class.
///
/// @since 9.0
/// @author Steve Ebersole
public interface KeyMapping {
	/// The Java type represented by this key.
	ClassDetails getKeyType();

	/// Visit each persistent attribute that participates in the key.
	void forEachAttribute(AttributeConsumer consumer);

	/// Whether the given attribute participates in this key.
	boolean contains(AttributeMetadata attributeMetadata);
}
