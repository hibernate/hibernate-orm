/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.jdbc.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.REPORTED;
import static org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.SUPPORTED;

/// Immutable overrides applied to raw JDBC metadata reports.
///
/// A custom Dialect should retain one profile and return it from
/// [Dialect#getJdbcMetadataOverrides()]. Select [SupportOverride#REPORTED] to
/// use Hibernate's interpretation of the raw driver report,
/// [SupportOverride#SUPPORTED] to force the effective answer to `true`, or
/// [SupportOverride#UNSUPPORTED] to force it to `false`.
///
/// These overrides describe effective driver behavior, not database SQL
/// syntax. In particular, standard REF_CURSOR support says whether the JDBC
/// `Types.REF_CURSOR` and typed `getObject(..., ResultSet.class)` path is
/// usable; callable SQL support is supplied separately.
///
/// @see Dialect#getJdbcMetadataOverrides()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class JdbcMetadataOverrides {
	/// The base-Dialect profile: use reported named-parameter and standard
	/// REF_CURSOR support, and force batch-update support.
	public static final JdbcMetadataOverrides STANDARD =
			new JdbcMetadataOverrides( REPORTED, SUPPORTED, REPORTED );

	private final SupportOverride namedParameterSupport;
	private final SupportOverride batchUpdateSupport;
	private final SupportOverride standardRefCursorSupport;

	private JdbcMetadataOverrides(
			SupportOverride namedParameterSupport,
			SupportOverride batchUpdateSupport,
			SupportOverride standardRefCursorSupport) {
		this.namedParameterSupport = requireSupport( namedParameterSupport, "namedParameterSupport" );
		this.batchUpdateSupport = requireSupport( batchUpdateSupport, "batchUpdateSupport" );
		this.standardRefCursorSupport = requireSupport( standardRefCursorSupport, "standardRefCursorSupport" );
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(JdbcMetadataOverrides base) {
		return new Builder( requireProfile( base ) );
	}

	/// The effective named-callable-parameter override.
	public SupportOverride getNamedParameterSupport() {
		return namedParameterSupport;
	}

	/// The effective JDBC batch-update override.
	public SupportOverride getBatchUpdateSupport() {
		return batchUpdateSupport;
	}

	/// The effective standard JDBC REF_CURSOR API override.
	public SupportOverride getStandardRefCursorSupport() {
		return standardRefCursorSupport;
	}

	/// Resolution to apply to one raw JDBC support report.
	public enum SupportOverride {
		/// Use the supplied, defensively interpreted driver report.
		REPORTED {
			@Override
			public boolean resolve(boolean reported) {
				return reported;
			}
		},

		/// Force the effective answer to `true`.
		SUPPORTED {
			@Override
			public boolean resolve(boolean reported) {
				return true;
			}
		},

		/// Force the effective answer to `false`.
		UNSUPPORTED {
			@Override
			public boolean resolve(boolean reported) {
				return false;
			}
		};

		/// Resolve the effective answer from the interpreted driver report.
		///
		/// @param reported the interpreted driver report
		public abstract boolean resolve(boolean reported);
	}

	/// Build an immutable JDBC-metadata-override profile.
	///
	/// Reuse this builder if convenient; each [#build()] call captures an
	/// immutable snapshot.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@SPI(USE)
	public static final class Builder {
		private SupportOverride namedParameterSupport;
		private SupportOverride batchUpdateSupport;
		private SupportOverride standardRefCursorSupport;

		private Builder(JdbcMetadataOverrides base) {
			namedParameterSupport = base.namedParameterSupport;
			batchUpdateSupport = base.batchUpdateSupport;
			standardRefCursorSupport = base.standardRefCursorSupport;
		}

		/// Select the effective named-callable-parameter override.
		public Builder namedParameterSupport(SupportOverride support) {
			namedParameterSupport = requireSupport( support, "support" );
			return this;
		}

		/// Select the effective JDBC batch-update override.
		public Builder batchUpdateSupport(SupportOverride support) {
			batchUpdateSupport = requireSupport( support, "support" );
			return this;
		}

		/// Select the effective standard JDBC REF_CURSOR API override.
		public Builder standardRefCursorSupport(SupportOverride support) {
			standardRefCursorSupport = requireSupport( support, "support" );
			return this;
		}

		/// Build and retain an immutable profile, then supply it from
		/// [Dialect#getJdbcMetadataOverrides()].
		public JdbcMetadataOverrides build() {
			return new JdbcMetadataOverrides(
					namedParameterSupport,
					batchUpdateSupport,
					standardRefCursorSupport
			);
		}
	}

	private static JdbcMetadataOverrides requireProfile(JdbcMetadataOverrides profile) {
		if ( profile == null ) {
			throw new IllegalArgumentException( "Base JDBC metadata overrides cannot be null" );
		}
		return profile;
	}

	private static SupportOverride requireSupport(SupportOverride support, String name) {
		if ( support == null ) {
			throw new IllegalArgumentException( name + " cannot be null" );
		}
		return support;
	}
}
