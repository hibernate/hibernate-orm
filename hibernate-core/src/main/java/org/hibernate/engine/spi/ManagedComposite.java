/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Specialized {@link Managed} contract for component/embeddable classes.
 *
 * @author Steve Ebersole
 */
public interface ManagedComposite extends Managed {

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default ManagedComposite asManagedComposite() {
		return this;
	}

}
