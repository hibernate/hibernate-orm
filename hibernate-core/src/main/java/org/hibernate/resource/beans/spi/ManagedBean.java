/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/**
 * Generalized contract for a (CDI or Spring) "managed bean" as seen by Hibernate
 * <p>
 * Integrations consume these handles and implement and supply them through
 * the {@link org.hibernate.resource.beans.container.spi.ContainedBean} contract
 * returned by a custom bean container.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ManagedBean<T> {
	/**
	 * The bean Java type
	 */
	Class<T> getBeanClass();

	/**
	 * The bean reference
	 */
	T getBeanInstance();
}
