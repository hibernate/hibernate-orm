/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/// The lifecycle state of a [Component]'s structural shape.
///
/// @see Component#completeShape()
///
/// @since 9.0
/// @author Steve Ebersole
public enum ComponentShapeState {
	/// Properties and other shape-defining metadata may still be contributed.
	BUILDING,

	/// The component's property order and selectable traversal are final.
	COMPLETE
}
