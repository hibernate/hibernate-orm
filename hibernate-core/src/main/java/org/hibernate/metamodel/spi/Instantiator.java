/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;

/**
 * Strategy for instantiating a managed type
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Instantiator {
	/**
	 * Performs and "instance of" check to see if the given object is an
	 * instance of managed structure
	 * @see Class#isInstance
	 */
	boolean isInstance(Object object);

	/**
	 * @see Class#equals
	 */
	boolean isSameClass(Object object);
}
