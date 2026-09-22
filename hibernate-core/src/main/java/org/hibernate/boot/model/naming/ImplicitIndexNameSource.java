/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/// Naming dependencies for an index. The inherited table and column identifiers
/// describe the table and columns participating in this naming decision.
///
/// @author Steve Ebersole
public non-sealed interface ImplicitIndexNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.INDEX;
	}
}
