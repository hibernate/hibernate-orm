/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

/// Test-only sink for normalized cascade traversal decisions.
///
/// Production traversal does not install a sink. Implementations must not
/// mutate cascade state or retain the traversal context.
///
/// @author Steve Ebersole
@FunctionalInterface
interface CascadeDecisionTrace {
	void record(CascadeTraceEvent event);

	/// Reports allocation of the mutable attribute-path workspace used by a
	/// traversal. This callback is used only by structural benchmark tracing.
	default void pathAllocated() {
	}
}
