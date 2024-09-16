/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Indicates the manner in which JDBC {@linkplain java.sql.Connection connections}
 * are acquired. Complementary to {@link ConnectionReleaseMode}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
 */
public enum ConnectionAcquisitionMode {
	/**
	 * The {@code Connection} will be acquired as soon as a session is opened.
	 * <p>
	 * This circumvents the {@link ConnectionReleaseMode}, as the {@code Connection}
	 * will then be held until the session is closed.
	 */
	IMMEDIATELY,
	/**
	 * A {@code Connection} is acquired only when (and if) it's actually needed.
	 * <p>
	 * This is the default (and legacy) behavior.
	 */
	AS_NEEDED;

	public static ConnectionAcquisitionMode interpret(String value) {
		return "immediate".equalsIgnoreCase( value ) || "immediately".equalsIgnoreCase( value )
				? IMMEDIATELY
				: AS_NEEDED;
	}

	public static ConnectionAcquisitionMode interpret(Object setting) {
		if ( setting == null ) {
			return null;
		}

		if ( setting instanceof ConnectionAcquisitionMode mode ) {
			return mode;
		}

		final String value = setting.toString();
		if ( isEmpty( value ) ) {
			return null;
		}

		return interpret( value );
	}
}
