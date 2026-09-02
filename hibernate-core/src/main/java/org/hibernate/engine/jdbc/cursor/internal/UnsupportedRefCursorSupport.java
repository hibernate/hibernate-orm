/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;

/// REF_CURSOR strategy for drivers without a usable access path.
///
/// @author Steve Ebersole
public final class UnsupportedRefCursorSupport implements RefCursorSupport {
	public static final UnsupportedRefCursorSupport INSTANCE = new UnsupportedRefCursorSupport();

	private UnsupportedRefCursorSupport() {
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, int position) {
		throw new UnsupportedOperationException(
				"REF_CURSOR registration is not supported for position [" + position + "]"
		);
	}

	@Override
	public void registerRefCursorParameter(CallableStatement statement, String name) {
		throw new UnsupportedOperationException(
				"REF_CURSOR registration is not supported for name [" + name + "]"
		);
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) {
		throw new UnsupportedOperationException(
				"REF_CURSOR extraction is not supported for position [" + position + "]"
		);
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) {
		throw new UnsupportedOperationException(
				"REF_CURSOR extraction is not supported for name [" + name + "]"
		);
	}
}
