/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Instantiates embeddable values from the attribute values supplied by
/// Hibernate.
///
/// Select an implementation for an embeddable using
/// [org.hibernate.annotations.EmbeddableInstantiator#value()], or register it
/// for an embeddable Java type using
/// [org.hibernate.annotations.EmbeddableInstantiatorRegistration#instantiator()].
///
/// @apiNote Incubating until the proposed `instantiate(IntFunction valueAccess)`
/// form can be implemented.
///
/// @see org.hibernate.annotations.EmbeddableInstantiator#value()
/// @see org.hibernate.annotations.EmbeddableInstantiatorRegistration#instantiator()
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface EmbeddableInstantiator extends Instantiator {
	/**
	 * Create an instance of the embeddable
	 */
	Object instantiate(ValueAccess valueAccess);
}
