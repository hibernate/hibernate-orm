/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.DatabaseOperation;

import java.sql.Connection;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDatabaseOperation implements DatabaseOperation {
	protected final JdbcAction[] preActions;
	protected final JdbcAction[] postActions;

	public AbstractDatabaseOperation() {
		this( null, null );
	}

	public AbstractDatabaseOperation(JdbcAction[] preActions, JdbcAction[] postActions) {
		this.preActions = preActions;
		this.postActions = postActions;
	}

	protected void performPreActions(
			StatementAccess statementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		performActions( preActions, statementAccess, jdbcConnection, executionContext );
	}

	protected void performPostActions(
			StatementAccess statementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		performActions( postActions, statementAccess, jdbcConnection, executionContext );
	}

	private void performActions(
			JdbcAction[] actions,
			StatementAccess statementAccess,
			Connection jdbcConnection,
			ExecutionContext executionContext) {
		if ( actions == null ) {
			return;
		}

		for ( int i = 0; i < actions.length; i++ ) {
			actions[i].perform( statementAccess, jdbcConnection, executionContext );
		}
	}

	protected static JdbcAction[] toArray(List<JdbcAction> actions) {
		if ( CollectionHelper.isEmpty( actions ) ) {
			return null;
		}
		return actions.toArray( new JdbcAction[0] );
	}
}
