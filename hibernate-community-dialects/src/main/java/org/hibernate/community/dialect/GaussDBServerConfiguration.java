/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * Utility class that extracts some initial configuration from the database for
 * {@link GaussDBDialect}, most notably the compatibility mode.
 *
 * @author plafaith
 */
@Internal
public class GaussDBServerConfiguration {

	/**
	 * The GaussDB compatibility mode of the target database ({@code pg_database.datcompatibility}):
	 * {@code A} = Oracle-compatible, {@code B}/{@code M} = MySQL-compatible, {@code PG} = PostgreSQL-compatible.
	 */
	public enum CompatibilityMode {
		A,
		B,
		M,
		PG;

		/**
		 * Whether this mode uses the MySQL-compatible kernel (datcompatibility {@code B} or {@code M}).
		 */
		public boolean isMySQLCompatible() {
			return this == B || this == M;
		}

		static CompatibilityMode parse(String value) {
			switch ( value.trim().toUpperCase( Locale.ROOT ) ) {
				case "A":
					return A;
				case "B":
					return B;
				case "M":
					return M;
				case "PG":
					return PG;
				default:
					// keep the lenient historical behavior: unknown values behave like A mode
					return A;
			}
		}
	}

	/**
	 * The default configuration, used when the compatibility mode cannot be detected:
	 * Oracle-compatible mode {@code A}.
	 */
	public static final GaussDBServerConfiguration DEFAULT = new GaussDBServerConfiguration( CompatibilityMode.A );

	private final CompatibilityMode compatibilityMode;

	public GaussDBServerConfiguration(CompatibilityMode compatibilityMode) {
		this.compatibilityMode = compatibilityMode;
	}

	public CompatibilityMode getCompatibilityMode() {
		return compatibilityMode;
	}

	/**
	 * Determines the {@link CompatibilityMode}, preferring an explicit
	 * {@link GaussDBDialect#GAUSSDB_COMPATIBILITY_MODE} config value: it works even when JDBC metadata
	 * is disallowed on boot ({@code hibernate.temp.use_jdbc_metadata_defaults=false}, e.g. the
	 * SchemaUpdate tests), where metadata is null and the {@code datcompatibility} probe below cannot
	 * run &mdash; without it the dialect would silently default to {@code A} and break
	 * {@code information_schema.sequences} extraction (M mode has no such view). Falls back to probing
	 * {@code pg_database.datcompatibility} when no explicit mode is configured, and to
	 * {@link #DEFAULT} when detection fails.
	 */
	public static GaussDBServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
		final Map<String, Object> configValues = info.getConfigurationValues();
		if ( configValues != null ) {
			final Object configured = configValues.get( GaussDBDialect.GAUSSDB_COMPATIBILITY_MODE );
			if ( configured != null ) {
				final String mode = configured.toString().trim();
				if ( !mode.isEmpty() ) {
					return new GaussDBServerConfiguration( CompatibilityMode.parse( mode ) );
				}
			}
		}
		final DatabaseMetaData metaData = info.getDatabaseMetadata();
		if ( metaData != null ) {
			try ( Statement statement = metaData.getConnection().createStatement();
					ResultSet rs = statement.executeQuery(
							"select datcompatibility from pg_database where datname = current_database()" ) ) {
				if ( rs.next() ) {
					final String mode = rs.getString( 1 );
					if ( mode != null ) {
						return new GaussDBServerConfiguration( CompatibilityMode.parse( mode ) );
					}
				}
			}
			catch (SQLException e) {
				// not GaussDB or the probe failed — keep the default
			}
		}
		return DEFAULT;
	}
}
