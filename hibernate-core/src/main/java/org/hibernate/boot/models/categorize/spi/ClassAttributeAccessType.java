/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import jakarta.persistence.AccessType;

/**
 * Possible class-level {@linkplain jakarta.persistence.AccessType} values.
 *
 * @author Steve Ebersole
 */
public enum ClassAttributeAccessType {
	/**
	 * The class explicitly defined field access via {@linkplain jakarta.persistence.Access}
	 */
	EXPLICIT_FIELD(true, AccessType.FIELD),

	/**
	 * The class explicitly defined property access via {@linkplain jakarta.persistence.Access}
	 */
	EXPLICIT_PROPERTY(true, AccessType.PROPERTY),

	/**
	 * The class implicitly defined field access.
	 */
	IMPLICIT_FIELD(false, AccessType.FIELD),

	/**
	 * The class implicitly defined property access.
	 */
	IMPLICIT_PROPERTY(false, AccessType.PROPERTY);

	private final boolean explicit;
	private final AccessType jpaAccessType;

	ClassAttributeAccessType(boolean explicit, AccessType jpaAccessType) {
		this.explicit = explicit;
		this.jpaAccessType = jpaAccessType;
	}

	/**
	 * Whether the access-type was explicitly specified
	 */
	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * The corresponding {@linkplain jakarta.persistence.AccessType}
	 */
	public AccessType getJpaAccessType() {
		return jpaAccessType;
	}
}
