/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;

/// Kind of table mutation
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public enum MutationKind {
	/// An `INSERT`
	INSERT,
	/// An `UPDATE`
	UPDATE,
	/// An `UPDATE` used to (re)assign a collection row's order/index column only.ity
	UPDATE_ORDER,
	/// A `DELETE`
	DELETE,
	/// Specialized no-op (callback carrier)
	NO_OP;
}
