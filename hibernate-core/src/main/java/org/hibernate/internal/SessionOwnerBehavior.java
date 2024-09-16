/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
