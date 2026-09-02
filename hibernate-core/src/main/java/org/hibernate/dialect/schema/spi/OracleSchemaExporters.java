/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.schema.internal.OracleUserDefinedTypeExporter;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;

import static org.hibernate.SPI.Role.USE;

/// Provides supported access to Oracle-specific schema exporters.
///
/// Retain the returned exporter for the lifetime of the supplying Dialect and
/// return it from [Dialect#getUserDefinedTypeExporter()]. Do not import or
/// instantiate Hibernate's internal Oracle exporter directly.
///
/// @see Dialect#getUserDefinedTypeExporter()
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class OracleSchemaExporters {
	private OracleSchemaExporters() {
	}

	/// Create an Oracle user-defined-type exporter owned by `dialect`.
	public static Exporter<UserDefinedType> userDefinedTypes(
			Dialect dialect,
			UserDefinedTypeDdlSupport ddlSupport) {
		if ( dialect == null || ddlSupport == null ) {
			throw new IllegalArgumentException( "Dialect and UDT DDL support must not be null" );
		}
		return new OracleUserDefinedTypeExporter( dialect, ddlSupport );
	}
}
