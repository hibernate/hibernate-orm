/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import org.hibernate.StatementObserver;

import java.io.Serializable;

/// Default StatementObserver implementation.  Simply ignores call.
///
/// @author Steve Ebersole
public class IgnoredStatementObserver implements StatementObserver, Serializable {
	public static final IgnoredStatementObserver IGNORE = new IgnoredStatementObserver();

	@Override
	public void performingSql(String sql, int batchPosition) {
		// just ignore
	}
}
