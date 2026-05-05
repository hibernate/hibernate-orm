/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import java.io.Serializable;

/// Effective constraint checking mode for deferrable constraints during graph-based flush planning.
/// This is the flush-based hook for controlling initial [deferrability][Deferrability].
///
/// @author Steve Ebersole
public enum DeferrableConstraintMode implements Serializable {
	/// Use each constraint's declared initial checking mode.
	DEFAULT,

	/// Treat deferrable constraints as checked after each statement for this flush.
	IMMEDIATE,

	/// Treat deferrable constraints as checked at transaction completion for this flush.
	DEFERRED;

	public boolean isDeferred(Deferrability deferrability) {
		return switch ( this ) {
			case DEFAULT -> deferrability == Deferrability.INITIALLY_DEFERRED;
			case IMMEDIATE -> false;
			case DEFERRED -> deferrability.isDeferrable();
		};
	}
}
