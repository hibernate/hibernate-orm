/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

/// Controls whether a test traversal executes terminal cascade and orphan
/// effects after making the corresponding semantic decisions.
///
/// This is provisional differential-test infrastructure and is never selected
/// by the ordinary production cascade entry path.
///
/// @author Steve Ebersole
enum CascadeEffectMode {
	EXECUTE,
	DECISION_ONLY
}
