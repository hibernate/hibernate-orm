/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;

/// Describes the logical relationship between the loaded and current collection endpoints.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public enum CollectionTransition {
	/// No loaded-to-current collection transition was detected.
	///
	/// This value is used when the input contains only queued operations for an
	/// uninitialized collection.
	NONE,

	/// Create the collection rows associated with the current endpoint.
	///
	/// There is no corresponding remove from a loaded endpoint.
	CREATE,

	/// Remove the collection rows associated with the loaded endpoint.
	///
	/// There is no corresponding create at a current endpoint.
	REMOVE,

	/// Update the collection at its existing loaded/current endpoint.
	///
	/// The role and key are unchanged; the collection's contents or ordering may
	/// nevertheless require row deletion, insertion, or update.
	UPDATE,

	/// Remove rows from the loaded endpoint and create rows at the current endpoint.
	///
	/// This represents a collection role or owner-key change. The remove side must
	/// precede the create side, and each side retains its own semantic lifecycle.
	REMOVE_AND_CREATE
}
