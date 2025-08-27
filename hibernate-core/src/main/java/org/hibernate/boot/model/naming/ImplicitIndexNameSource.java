/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/**
 * @author Steve Ebersole
 */
public non-sealed interface ImplicitIndexNameSource
		extends ImplicitConstraintNameSource {
	@Override
	default Kind kind() {
		return Kind.INDEX;
	}
}
