/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.io.Serializable;

/// Serializable state captured by generator resynchronization and reset commands.
///
/// The optimizer itself is runtime state and is therefore not serialized. Its
/// adjustment is retained as declarative input for calculating a resynchronized
/// source value. While the original live optimizer remains attached, [#reset()]
/// also clears its local state. After deserialization there is no optimizer to
/// reset, and that operation intentionally becomes a no-op.
///
/// @since 9.0
/// @author Steve Ebersole
final class OptimizerResetState implements Serializable {
	private final int adjustment;
	private transient Optimizer optimizer;

	OptimizerResetState(Optimizer optimizer) {
		this.optimizer = optimizer;
		this.adjustment = optimizer.getAdjustment();
	}

	/// The adjustment applied when calculating the next source value.
	int adjustment() {
		return adjustment;
	}

	/// Resets the attached live optimizer, if this state has not been deserialized.
	void reset() {
		if ( optimizer != null ) {
			optimizer.reset();
		}
	}
}
