/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.spi;

import org.hibernate.SPI;
import org.hibernate.resource.beans.spi.ManagedBean;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/**
 * A bean handle with lifecycle hooks for its owning {@link BeanContainer}.
 * Consumers access the bean through {@link ManagedBean}; the container uses
 * {@link #initialize()} and {@link #release()} to manage its lifecycle.
 * Custom containers implement and supply these handles from their acquisition methods.
 * <p>
 * Consumers must request release through the managed bean registry or
 * {@link BeanContainer#releaseBean(ManagedBean)}, rather than invoking
 * {@link #release()} directly, so cache eviction and lifecycle bookkeeping
 * take place as well.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ContainedBean<B> extends ManagedBean<B> {
	/**
	 * Initialize the bean if necessary. Already initialized handles may do nothing.
	 */
	@SPI({ USE, IMPLEMENT })
	void initialize();

	/**
	 * Release resources associated with this handle without forcing initialization.
	 * Handles with no resources to release may do nothing.
	 */
	@SPI({ USE, IMPLEMENT })
	void release();
}
