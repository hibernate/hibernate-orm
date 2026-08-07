/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.spi;

import org.hibernate.Internal;

/// Describes how an entity's mapped properties are selected for one cascade route.
///
/// @author Steve Ebersole
@Internal
public enum PropertySelectionKind {
	/// No mapped property needs to be considered.
	NONE,

	/// Every mapped property is considered in its ordinary metadata order.
	ALL,

	/// An ordered subset of mapped property indexes is considered.
	SELECTED
}
