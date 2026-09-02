/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;

/// Standalone-provider REF_CURSOR factory which selects standard access or the
/// fixture's custom untyped access from effective metadata.
///
/// @author Steve Ebersole
public final class ExampleRefCursorSupportFactory implements RefCursorSupportFactory {
	public static final ExampleRefCursorSupportFactory INSTANCE = new ExampleRefCursorSupportFactory();

	private ExampleRefCursorSupportFactory() {
	}

	@Override
	public RefCursorSupport createRefCursorSupport(RefCursorSupportCreationContext context) {
		return context.supportsStandardRefCursors()
				? RefCursorSupports.standard().createRefCursorSupport( context )
				: new FixtureRefCursorSupport( context );
	}

	private static final class FixtureRefCursorSupport implements RefCursorSupport {
		private final RefCursorSupportCreationContext creationContext;

		private FixtureRefCursorSupport(RefCursorSupportCreationContext creationContext) {
			this.creationContext = creationContext;
		}

		@Override
		public void registerRefCursorParameter(CallableStatement statement, int position) {
			try {
				statement.registerOutParameter( position, Types.OTHER );
			}
			catch (SQLException e) {
				throw creationContext.convert( e, "Fixture REF_CURSOR registration failed at position [" + position + "]" );
			}
		}

		@Override
		public void registerRefCursorParameter(CallableStatement statement, String name) {
			try {
				statement.registerOutParameter( name, Types.OTHER );
			}
			catch (SQLException e) {
				throw creationContext.convert( e, "Fixture REF_CURSOR registration failed for name [" + name + "]" );
			}
		}

		@Override
		public ResultSet getResultSet(CallableStatement statement, int position) {
			try {
				return (ResultSet) statement.getObject( position );
			}
			catch (SQLException e) {
				throw creationContext.convert( e, "Fixture REF_CURSOR extraction failed at position [" + position + "]" );
			}
		}

		@Override
		public ResultSet getResultSet(CallableStatement statement, String name) {
			try {
				return (ResultSet) statement.getObject( name );
			}
			catch (SQLException e) {
				throw creationContext.convert( e, "Fixture REF_CURSOR extraction failed for name [" + name + "]" );
			}
		}
	}
}
