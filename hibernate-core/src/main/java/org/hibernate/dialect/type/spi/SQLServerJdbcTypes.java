/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.SQLServerCastingXmlArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.SQLServerCastingXmlJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock SQL Server JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// and contribute the returned descriptor or constructor without referring to
/// its concrete implementation class.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class SQLServerJdbcTypes {
	private SQLServerJdbcTypes() {
	}

	/// Obtain SQL Server's casting XML descriptor.
	public static JdbcType castingXml() {
		return SQLServerCastingXmlJdbcType.INSTANCE;
	}

	/// Obtain SQL Server's casting XML-array type constructor.
	public static JdbcTypeConstructor castingXmlArrayConstructor() {
		return SQLServerCastingXmlArrayJdbcTypeConstructor.INSTANCE;
	}
}
