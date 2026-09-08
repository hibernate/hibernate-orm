/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.StandardDirectJavaTimeJdbcSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock direct Java Time JDBC support profiles.
///
/// @see StandardDirectJavaTimeJdbcSupport
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class DirectJavaTimeJdbcSupports {
	private DirectJavaTimeJdbcSupports() {
	}

	/// Return a profile supporting no Java Time classes.
	public static DirectJavaTimeJdbcSupport none() {
		return StandardDirectJavaTimeJdbcSupport.none();
	}

	/// Return the JDBC 4.2 profile. It supports `LocalDate`, `LocalTime`,
	/// `LocalDateTime`, `OffsetTime`, and `OffsetDateTime`, but not
	/// `ZonedDateTime` or `Instant`.
	public static DirectJavaTimeJdbcSupport jdbc42() {
		return StandardDirectJavaTimeJdbcSupport.jdbc42();
	}

	/// Return a profile supporting every Java Time class recognized by Hibernate.
	public static DirectJavaTimeJdbcSupport all() {
		return StandardDirectJavaTimeJdbcSupport.all();
	}

	/// Build a profile for the specified Java Time classes.
	///
	/// `OffsetTime` and `OffsetDateTime` are enabled only when both classes are
	/// specified. If either is absent, both are unsupported.
	public static DirectJavaTimeJdbcSupport of(Class<?>... supportedJavaTimeTypes) {
		return StandardDirectJavaTimeJdbcSupport.of( supportedJavaTimeTypes );
	}
}
