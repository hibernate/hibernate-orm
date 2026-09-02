/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;

/// HANA TABLE OUT fallback whose result sets are implicit.
///
/// @author Steve Ebersole
public final class ImplicitResultSetRefCursorSupport implements RefCursorSupport {
	public static final ImplicitResultSetRefCursorSupport INSTANCE = new ImplicitResultSetRefCursorSupport();

	private ImplicitResultSetRefCursorSupport() {
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		throw new UnsupportedOperationException(
				"HANA TABLE OUT results are implicit and cannot be extracted by position [" + position + "]"
		);
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		throw new UnsupportedOperationException(
				"HANA TABLE OUT results are implicit and cannot be extracted by name [" + name + "]"
		);
	}
}
