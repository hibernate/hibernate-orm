/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;

/// GaussDB REF_CURSOR fallback based on the first positional parameter.
///
/// @author Steve Ebersole
public final class GaussDBRefCursorSupportFactory implements RefCursorSupportFactory {
	public static final GaussDBRefCursorSupportFactory INSTANCE = new GaussDBRefCursorSupportFactory();

	private GaussDBRefCursorSupportFactory() {
	}

	@Override
	public RefCursorSupport createRefCursorSupport(RefCursorSupportCreationContext context) {
		return new Support( context );
	}

	private static final class Support implements RefCursorSupport {
		private final RefCursorSupportCreationContext creationContext;

		private Support(RefCursorSupportCreationContext creationContext) {
			this.creationContext = creationContext;
		}

		@Override
		public void registerRefCursorParameter(CallableStatement statement, int position) {
			try {
				statement.registerOutParameter( position, Types.OTHER );
			}
			catch (SQLException e) {
				throw creationContext.convert(
						e,
						"Error asking dialect to register ref cursor parameter [" + position + "]"
				);
			}
		}

		@Override
		public void registerRefCursorParameter(CallableStatement statement, String name) {
			throw new UnsupportedOperationException(
					"GaussDB only supports registering REF_CURSOR parameters by position"
			);
		}

		@Override
		public ResultSet getResultSet(CallableStatement statement, int position) {
			if ( position != 1 ) {
				throw new UnsupportedOperationException(
						"GaussDB only supports REF_CURSOR parameters as the first parameter"
				);
			}
			try {
				return (ResultSet) statement.getObject( 1 );
			}
			catch (SQLException e) {
				throw creationContext.convert(
						e,
						"Error asking dialect to extract ResultSet from CallableStatement parameter [" + position + "]"
				);
			}
		}

		@Override
		public ResultSet getResultSet(CallableStatement statement, String name) {
			throw new UnsupportedOperationException(
					"GaussDB only supports accessing REF_CURSOR parameters by position"
			);
		}
	}
}
