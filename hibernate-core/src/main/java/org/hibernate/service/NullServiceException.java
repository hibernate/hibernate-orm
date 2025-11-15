/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import org.hibernate.service.spi.ServiceException;

/**
 * @author Andrea Boriero
 */
public class NullServiceException extends ServiceException {
	public final Class<?> serviceRole;

	public NullServiceException(Class<?> serviceRole) {
		super( "Unknown service requested [" + serviceRole.getName() + "]" );
		this.serviceRole = serviceRole;
	}

	public Class<?> getServiceRole() {
		return serviceRole;
	}
}
