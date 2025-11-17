/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.ScrollMode;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import org.checkerframework.checker.nullness.qual.Nullable;

public class StandardStatementCreator implements JdbcSelectExecutor.StatementCreator {

	static final StandardStatementCreator[] INSTANCES;

	static {
		final var scrollModes = ScrollMode.values();
		final var instances = new StandardStatementCreator[scrollModes.length + 1];
		for ( int i = 0; i < scrollModes.length; i++ ) {
			instances[i] = new StandardStatementCreator( scrollModes[i] );
		}
		instances[scrollModes.length] = new StandardStatementCreator( null );
		INSTANCES = instances;
	}

	public static JdbcSelectExecutor.StatementCreator getStatementCreator(@Nullable ScrollMode scrollMode) {
		return StandardStatementCreator.INSTANCES[scrollMode == null
				? StandardStatementCreator.INSTANCES.length - 1
				: scrollMode.ordinal()];
	}

	final @Nullable ScrollMode scrollMode;

	private StandardStatementCreator(@Nullable ScrollMode scrollMode) {
		this.scrollMode = scrollMode;
	}

	@Override
	public PreparedStatement createStatement(ExecutionContext executionContext, String sql)
			throws SQLException {
		return executionContext.getSession().getJdbcCoordinator().getStatementPreparer()
				.prepareQueryStatement( sql, false, scrollMode );
	}
}
