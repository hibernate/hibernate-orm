/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import java.util.Set;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.StandardDirectJavaTimeJdbcSupport;

import static org.hibernate.SPI.Role.USE;

/// Supplies immutable stock direct Java Time JDBC support profiles.
///
/// @see DirectJavaTimeJdbcSupport
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

	/// Return the JDBC 4.2 scalar-access profile. It supports `LocalDate`,
	/// `LocalTime`, `LocalDateTime`, `OffsetTime`, and `OffsetDateTime`, but not
	/// `ZonedDateTime` or `Instant`. It makes no assumptions about native JDBC
	/// container support.
	public static DirectJavaTimeJdbcSupport jdbc42() {
		return StandardDirectJavaTimeJdbcSupport.jdbc42();
	}

	/// Return a scalar-access profile supporting every Java Time class recognized
	/// by Hibernate. It makes no assumptions about native JDBC container support.
	public static DirectJavaTimeJdbcSupport all() {
		return StandardDirectJavaTimeJdbcSupport.all();
	}

	/// Build a scalar-access profile for the specified Java Time classes. It makes
	/// no assumptions about native JDBC container support.
	///
	/// `OffsetTime` and `OffsetDateTime` are enabled only when both classes are
	/// specified. If either is absent, both are unsupported.
	public static DirectJavaTimeJdbcSupport of(Class<?>... supportedJavaTimeTypes) {
		return StandardDirectJavaTimeJdbcSupport.of( supportedJavaTimeTypes );
	}

	/// Build a profile with independent support for ordinary scalar access,
	/// native JDBC `STRUCT` attributes, and native JDBC `ARRAY` elements.
	///
	/// `OffsetTime` and `OffsetDateTime` are enabled within each access path only
	/// when both classes are specified for that path. If either is absent, both
	/// are unsupported for that path.
	public static DirectJavaTimeJdbcSupport of(
			Set<Class<?>> supportedJavaTimeTypes,
			Set<Class<?>> supportedInStructJavaTimeTypes,
			Set<Class<?>> supportedInArrayJavaTimeTypes) {
		return StandardDirectJavaTimeJdbcSupport.of(
				supportedJavaTimeTypes,
				supportedInStructJavaTimeTypes,
				supportedInArrayJavaTimeTypes
		);
	}
}
