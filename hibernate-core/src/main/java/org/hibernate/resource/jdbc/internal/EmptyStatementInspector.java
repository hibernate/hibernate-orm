/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * @author Jan Schatteman
 */
public class EmptyStatementInspector implements StatementInspector {
	/**
	 * The singleton reference.
	 */
	public static final StatementInspector INSTANCE = new EmptyStatementInspector();

	@Override
	public String inspect(String sql) {
		return sql;
	}

	protected EmptyStatementInspector() {}
}
