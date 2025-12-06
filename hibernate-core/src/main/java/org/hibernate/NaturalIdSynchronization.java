/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.FindOption;

/// Indicates whether to perform synchronization (a sort of flush)
/// before a [find by natural-id][KeyType#NATURAL].
///
/// @author Steve Ebersole
public enum NaturalIdSynchronization implements FindOption {
	/// Perform the synchronization.
	ENABLED,

	///  Do not perform the synchronization.
	DISABLED;
}
