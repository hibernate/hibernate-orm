/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.spi;

import org.hibernate.models.spi.TypeDetails;

/// Read-only categorized description of a mapped value.
///
/// A value describes the semantic shape nested within an attribute, collection
/// element, collection index, or collection identifier. It is deliberately
/// independent of [AttributeMetadata]: not every mapped value is itself an
/// attribute.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ValueMetadata {
	/// The Java type of the mapped value.
	TypeDetails getType();

	/// The semantic mapping shape of the value.
	ValueNature getNature();
}
