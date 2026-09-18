/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.Incubating;
import org.hibernate.accessor.AccessorFactory;
import org.hibernate.property.access.spi.PropertyAccessorService;

/**
 * {@link PropertyAccessorService} implementation backed by a reflection-based
 * {@link AccessorFactory}.
 */
@Incubating(since = "8.0")
public class DelegatingPropertyAccessorService implements PropertyAccessorService {

	private final AccessorFactory factory;

	public DelegatingPropertyAccessorService(AccessorFactory factory) {
		this.factory = factory;
	}

	@Override
	public AccessorFactory hibernateAccessorFactory() {
		return factory;
	}
}
