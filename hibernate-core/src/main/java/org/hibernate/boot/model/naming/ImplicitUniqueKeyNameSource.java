/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/// Naming dependencies for a unique key. The inherited table and column identifiers
/// describe the table and columns participating in this naming decision.
///
/// @author Steve Ebersole
public non-sealed interface ImplicitUniqueKeyNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.UNIQUE_KEY;
	}
}
