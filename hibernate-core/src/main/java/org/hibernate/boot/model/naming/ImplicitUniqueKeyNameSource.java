/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/**
 * @author Steve Ebersole
 */
public non-sealed interface ImplicitUniqueKeyNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.UNIQUE_KEY;
	}
}
