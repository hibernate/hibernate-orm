/*
 * SPDX-License-Identifier: Apache-2.0
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
	 * In this acquisition mode, {@link ConnectionReleaseMode#ON_CLOSE} must be used.
	 */
	IMMEDIATELY,
	/**
	 * A {@code Connection} is acquired only when (and if) it's actually needed.
	 * <p>
	 * This is the default (and legacy) behavior.
	 * <p>
	 * In this acquisition mode, any {@link ConnectionReleaseMode} must be used.
	 */
	AS_NEEDED;

	public static ConnectionAcquisitionMode interpret(String value) {
		return "immediate".equalsIgnoreCase( value )
			|| "immediately".equalsIgnoreCase( value )
				? IMMEDIATELY
				: AS_NEEDED;
	}

	public static ConnectionAcquisitionMode interpret(Object setting) {
		if ( setting == null ) {
			return null;
		}
		else if ( setting instanceof ConnectionAcquisitionMode mode ) {
			return mode;
		}
		else {
			final String value = setting.toString();
			return isEmpty( value ) ? null : interpret( value );
		}
	}
}
