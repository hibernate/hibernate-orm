/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.spi;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.cursor.internal.FirstParameterRefCursorSupport;
import org.hibernate.engine.jdbc.cursor.internal.ImplicitResultSetRefCursorSupport;
import org.hibernate.engine.jdbc.cursor.internal.JdbcTypeRefCursorSupport;
import org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport;
import org.hibernate.engine.jdbc.cursor.internal.UnsupportedRefCursorSupport;

import static org.hibernate.SPI.Role.USE;
import static java.sql.Types.OTHER;
import static java.sql.Types.REF;

/// Supported factories for common REF_CURSOR JDBC access strategies.
///
/// Retain the selected factory and supply that same instance from
/// [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class RefCursorSupports {
	private static final RefCursorSupportFactory STANDARD = StandardRefCursorSupport::new;
	private static final RefCursorSupportFactory UNSUPPORTED = context -> UnsupportedRefCursorSupport.INSTANCE;
	private static final RefCursorSupportFactory METADATA_SELECTED = metadataSelected( UNSUPPORTED );
	private static final RefCursorSupportFactory POSTGRESQL_FALLBACK =
			context -> new FirstParameterRefCursorSupport( "PostgreSQL", OTHER, context );
	private static final RefCursorSupportFactory POSTGRES_PLUS_FALLBACK =
			context -> new FirstParameterRefCursorSupport( "Postgres Plus", REF, context );
	private static final RefCursorSupportFactory HANA_FALLBACK =
			context -> ImplicitResultSetRefCursorSupport.INSTANCE;
	private static final RefCursorSupportFactory POSTGRESQL = metadataSelected( POSTGRESQL_FALLBACK );
	private static final RefCursorSupportFactory POSTGRES_PLUS = metadataSelected( POSTGRES_PLUS_FALLBACK );
	private static final RefCursorSupportFactory HANA = metadataSelected( HANA_FALLBACK );

	private RefCursorSupports() {
	}

	/// Retain and supply the stable factory which selects standard JDBC access
	/// when effective metadata permits it and unsupported access otherwise from
	/// [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory metadataSelected() {
		return METADATA_SELECTED;
	}

	/// Retain and supply a factory which selects standard JDBC access when
	/// effective metadata permits it and otherwise creates the given fallback
	/// from [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory metadataSelected(RefCursorSupportFactory fallback) {
		if ( fallback == null ) {
			throw new IllegalArgumentException( "REF_CURSOR fallback factory cannot be null" );
		}
		return context -> context.supportsStandardRefCursors()
				? STANDARD.createRefCursorSupport( context )
				: requireSupport( fallback, fallback.createRefCursorSupport( context ) );
	}

	/// Retain and supply the stable factory which always uses standard JDBC
	/// REF_CURSOR registration and typed extraction from
	/// [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory standard() {
		return STANDARD;
	}

	/// Retain and supply a factory which registers the given JDBC type code and
	/// extracts the cursor through untyped `getObject()` calls from
	/// [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory jdbcType(int jdbcTypeCode) {
		return context -> new JdbcTypeRefCursorSupport( jdbcTypeCode, context );
	}

	/// Retain and supply the stable factory which rejects every REF_CURSOR
	/// registration and extraction operation from
	/// [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory unsupported() {
		return UNSUPPORTED;
	}

	/// Retain and supply the stable metadata-selected PostgreSQL access factory
	/// from [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory postgresql() {
		return POSTGRESQL;
	}

	/// Retain and supply the stable metadata-selected Postgres Plus access factory
	/// from [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory postgresPlus() {
		return POSTGRES_PLUS;
	}

	/// Retain and supply the stable metadata-selected HANA TABLE OUT access factory
	/// from [org.hibernate.dialect.Dialect#getRefCursorSupportFactory()].
	public static RefCursorSupportFactory hana() {
		return HANA;
	}

	private static RefCursorSupport requireSupport(
			RefCursorSupportFactory factory,
			RefCursorSupport support) {
		if ( support == null ) {
			throw new IllegalStateException(
					"RefCursorSupportFactory [" + factory + "] returned null; a non-null RefCursorSupport is required"
			);
		}
		return support;
	}
}
