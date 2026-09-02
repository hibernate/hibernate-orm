/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.MySQLCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.MySQLCastingJsonJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock MySQL JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and register the returned descriptor or constructor without referring to its
/// concrete implementation class.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class MySQLJdbcTypes {
	private MySQLJdbcTypes() {
	}

	/// Obtain MySQL's casting JSON descriptor.
	public static JdbcType castingJson() {
		return MySQLCastingJsonJdbcType.INSTANCE;
	}

	/// Obtain MySQL's casting JSON-array type constructor.
	public static JdbcTypeConstructor castingJsonArrayConstructor() {
		return MySQLCastingJsonArrayJdbcTypeConstructor.INSTANCE;
	}
}
