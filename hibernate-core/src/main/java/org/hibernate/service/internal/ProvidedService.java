/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

/**
 * A service provided as-is.
 *
 * @author Steve Ebersole
 */
public class ProvidedService<R> {
	private final Class<R> serviceRole;
	private final R service;

	public ProvidedService(Class<R> serviceRole, R service) {
		this.serviceRole = serviceRole;
		this.service = service;
	}

	public Class<R> getServiceRole() {
		return serviceRole;
	}

	public R getService() {
		return service;
	}
}
