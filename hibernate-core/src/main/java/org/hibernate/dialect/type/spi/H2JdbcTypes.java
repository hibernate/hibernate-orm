/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.H2DurationIntervalSecondJdbcType;
import org.hibernate.dialect.type.internal.H2JsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.H2JsonJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock H2 JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and contribute each result with the matching JDBC descriptor or constructor
/// operation. Preserve whether the original contribution replaced a descriptor
/// or added it only when absent.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class H2JdbcTypes {
	private H2JdbcTypes() {
	}

	/// Obtain H2's duration-based interval-second descriptor.
	public static JdbcType durationIntervalSecond() {
		return H2DurationIntervalSecondJdbcType.INSTANCE;
	}

	/// Obtain H2's JSON descriptor.
	public static JdbcType json() {
		return H2JsonJdbcType.INSTANCE;
	}

	/// Obtain H2's JSON-array type constructor.
	public static JdbcTypeConstructor jsonArrayConstructor() {
		return H2JsonArrayJdbcTypeConstructor.INSTANCE;
	}
}
