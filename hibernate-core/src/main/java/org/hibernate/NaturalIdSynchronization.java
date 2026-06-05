/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/// Indicates whether natural id synchronization (a sort of flush)
/// should occur before a [find by natural id][KeyType#NATURAL].
///
/// @author Steve Ebersole
/// @since 7.3
public enum NaturalIdSynchronization implements FindOption {
	/// Synchronization should occur.
	ENABLED,

	/// Synchronization should not occur.
	DISABLED;
}
