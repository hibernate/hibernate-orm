/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.spi;

import org.hibernate.HibernateException;

/**
 * Occurs when there is a problem configuring a {@link ConnectionProvider}.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public class ConnectionProviderConfigurationException extends HibernateException {
	public ConnectionProviderConfigurationException(String message) {
		super( message );
	}
	public ConnectionProviderConfigurationException(String message, Throwable cause) {
		super( message, cause );
	}
}
