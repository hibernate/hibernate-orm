/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * @author Ståle W. Pedersen
 */
public interface CompositeTracker extends PrimeAmongSecondarySupertypes {

	void $$_hibernate_setOwner(String name, CompositeOwner tracker);

	void $$_hibernate_clearOwner(String name);

	@Override
	default CompositeTracker asCompositeTracker() {
		return this;
	}

}
