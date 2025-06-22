/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

/**
 * @author Steve Ebersole
 */
public enum SessionOwnerBehavior {
	// todo : (5.2) document differences in regard to SessionOwner implementations
	LEGACY_JPA,
	LEGACY_NATIVE
}
