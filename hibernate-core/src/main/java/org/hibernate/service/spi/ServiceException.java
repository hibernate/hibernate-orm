/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem with a service.
 *
 * @author Steve Ebersole
 */
public class ServiceException extends HibernateException {
	public ServiceException(String message, Throwable root) {
		super( message, root );
	}

	public ServiceException(String message) {
		super( message );
	}
}
