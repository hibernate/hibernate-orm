/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;
import org.hibernate.service.spi.ServiceException;

/**
 * Indicates that an unknown service was requested from the registry.
 *
 * @author Steve Ebersole
 */
public class UnknownServiceException extends ServiceException {
	public final Class<?> serviceRole;

	public UnknownServiceException(Class<?> serviceRole) {
		super( "Unknown service requested [" + serviceRole.getName() + "]" );
		this.serviceRole = serviceRole;
	}

	public Class<?> getServiceRole() {
		return serviceRole;
	}
}
