/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.type.internal.SybaseJtdsJsonAsStringArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.SybaseJtdsJsonAsStringJdbcType;
import org.hibernate.dialect.type.internal.SybaseJtdsLongNVarcharJdbcType;
import org.hibernate.dialect.type.internal.SybaseJtdsNCharJdbcType;
import org.hibernate.dialect.type.internal.SybaseJtdsNClobJdbcType;
import org.hibernate.dialect.type.internal.SybaseJtdsNVarcharJdbcType;
import org.hibernate.dialect.type.internal.SybaseJtdsXmlAsStringArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.internal.SybaseJtdsXmlAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's stock Sybase jTDS JDBC type descriptors.
///
/// Call these methods from
/// [org.hibernate.dialect.Dialect#contributeTypes(org.hibernate.boot.model.TypeContributions, org.hibernate.service.ServiceRegistry)]
/// only for the jTDS driver branch and preserve the existing registration order
/// of nationalized, JSON, XML, and array descriptors.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class SybaseJdbcTypes {
	private SybaseJdbcTypes() {
	}

	/// Obtain the jTDS NCLOB descriptor.
	public static JdbcType jtdsNClob() {
		return SybaseJtdsNClobJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS NCHAR descriptor.
	public static JdbcType jtdsNChar() {
		return SybaseJtdsNCharJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS NVARCHAR descriptor.
	public static JdbcType jtdsNVarchar() {
		return SybaseJtdsNVarcharJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS long-NVARCHAR descriptor.
	public static JdbcType jtdsLongNVarchar() {
		return SybaseJtdsLongNVarcharJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS JSON-as-nationalized-string descriptor.
	public static JdbcType jtdsJson() {
		return SybaseJtdsJsonAsStringJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS JSON-as-nationalized-string array constructor.
	public static JdbcTypeConstructor jtdsJsonArrayConstructor() {
		return SybaseJtdsJsonAsStringArrayJdbcTypeConstructor.INSTANCE;
	}

	/// Obtain the jTDS XML-as-nationalized-string descriptor.
	public static JdbcType jtdsXml() {
		return SybaseJtdsXmlAsStringJdbcType.JTDS_INSTANCE;
	}

	/// Obtain the jTDS XML-as-nationalized-string array constructor.
	public static JdbcTypeConstructor jtdsXmlArrayConstructor() {
		return SybaseJtdsXmlAsStringArrayJdbcTypeConstructor.INSTANCE;
	}
}
