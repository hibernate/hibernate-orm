/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.SPI;
import org.hibernate.procedure.internal.DB2CallableStatementSupport;
import org.hibernate.procedure.internal.JTDSCallableStatementSupport;
import org.hibernate.procedure.internal.PostgreSQLCallableStatementSupport;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.internal.SybaseCallableStatementSupport;

import static org.hibernate.SPI.Role.USE;

/// Provides standard and database-specific callable-statement strategies.
///
/// Select a stable stock strategy or build one standard strategy, retain it for
/// the Dialect's lifetime, and return it from
/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()]. Implement
/// [CallableStatementSupport] directly only for a genuinely different call
/// protocol.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class CallableStatementSupports {
	private static final NamedCallableParameterRenderer POSITIONAL_RENDERER =
			(sqlAppender, parameterName) -> sqlAppender.appendSql( '?' );
	private static final CallableStatementSupport STANDARD =
			new StandardCallableStatementSupport( false, POSITIONAL_RENDERER );
	private static final CallableStatementSupport STANDARD_WITH_REF_CURSORS =
			new StandardCallableStatementSupport( true, POSITIONAL_RENDERER );

	private CallableStatementSupports() {
	}

	/// Obtain the stable standard strategy without REF_CURSOR support.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	public static CallableStatementSupport standard() {
		return STANDARD;
	}

	/// Obtain the stable standard strategy with REF_CURSOR support.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	public static CallableStatementSupport standardWithRefCursors() {
		return STANDARD_WITH_REF_CURSORS;
	}

	/// Begin configuring an immutable standard strategy.
	public static StandardBuilder builder() {
		return new StandardBuilder();
	}

	/// Obtain the stable DB2 callable-statement strategy.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	public static CallableStatementSupport db2() {
		return DB2CallableStatementSupport.INSTANCE;
	}

	/// Obtain the stable PostgreSQL callable-statement strategy.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	///
	/// @param supportsProcedures whether the database supports native procedures
	public static CallableStatementSupport postgresql(boolean supportsProcedures) {
		return supportsProcedures
				? PostgreSQLCallableStatementSupport.INSTANCE
				: PostgreSQLCallableStatementSupport.V10_INSTANCE;
	}

	/// Obtain the stable Sybase callable-statement strategy.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	public static CallableStatementSupport sybase() {
		return SybaseCallableStatementSupport.INSTANCE;
	}

	/// Obtain the stable jTDS callable-statement strategy.
	///
	/// Supply the returned strategy from
	/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
	public static CallableStatementSupport jtds() {
		return JTDSCallableStatementSupport.INSTANCE;
	}

	/// Configures the standard callable-statement algorithm.
	///
	/// Reuse this builder if convenient; each [#build()] call captures an
	/// immutable snapshot.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@SPI(USE)
	public static final class StandardBuilder {
		private boolean supportsRefCursors;
		private NamedCallableParameterRenderer namedParameterRenderer = POSITIONAL_RENDERER;

		private StandardBuilder() {
		}

		/// Configure whether REF_CURSOR parameters are supported.
		public StandardBuilder supportsRefCursors(boolean supportsRefCursors) {
			this.supportsRefCursors = supportsRefCursors;
			return this;
		}

		/// Configure rendering of a named callable argument.
		///
		/// The renderer must append the complete argument, including its JDBC
		/// parameter marker.
		public StandardBuilder namedParameterRenderer(NamedCallableParameterRenderer renderer) {
			if ( renderer == null ) {
				throw new IllegalArgumentException( "Named callable parameter renderer cannot be null" );
			}
			this.namedParameterRenderer = renderer;
			return this;
		}

		/// Build and retain an immutable strategy, then supply it from
		/// [org.hibernate.dialect.Dialect#getCallableStatementSupport()].
		public CallableStatementSupport build() {
			return new StandardCallableStatementSupport( supportsRefCursors, namedParameterRenderer );
		}
	}
}
