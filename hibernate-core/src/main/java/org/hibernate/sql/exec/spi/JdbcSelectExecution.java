/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor.StatementCreator;

class JdbcSelectExecution {
	private static final StatementCreator CALLABLE_STATEMENT_CREATOR =
			(executionContext, sql) ->
					executionContext.getSession().getJdbcCoordinator().getStatementPreparer()
							.prepareQueryStatement( sql, true, null );

	static StatementCreator defaultStatementCreator(JdbcSelect jdbcSelect) {
		return jdbcSelect.isCallable()
				? CALLABLE_STATEMENT_CREATOR
				: StandardStatementCreator.getStatementCreator( null );
	}

}
