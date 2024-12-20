/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Specialized {@link Managed} contract for MappedSuperclass classes.
 *
 * @author Luis Barreiro
 */
public interface ManagedMappedSuperclass extends Managed {

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default ManagedMappedSuperclass asManagedMappedSuperclass() {
		return this;
	}

}
