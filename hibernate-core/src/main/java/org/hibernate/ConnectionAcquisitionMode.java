/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.internal.util.StringHelper;

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
		if ( "immediate".equalsIgnoreCase( value ) || "immediately".equalsIgnoreCase( value ) ) {
			return IMMEDIATELY;
		}

		return AS_NEEDED;
	}

	public static ConnectionAcquisitionMode interpret(Object setting) {
		if ( setting == null ) {
			return null;
		}

		if ( setting instanceof ConnectionAcquisitionMode ) {
			return (ConnectionAcquisitionMode) setting;
		}

		final String value = setting.toString();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}

		return interpret( value );
	}
}
