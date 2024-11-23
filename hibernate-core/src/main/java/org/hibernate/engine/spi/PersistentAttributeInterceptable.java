/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttributeInterceptable extends PrimeAmongSecondarySupertypes {
	PersistentAttributeInterceptor $$_hibernate_getInterceptor();
	void $$_hibernate_setInterceptor(PersistentAttributeInterceptor interceptor);

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default PersistentAttributeInterceptable asPersistentAttributeInterceptable() {
		return this;
	}

}
