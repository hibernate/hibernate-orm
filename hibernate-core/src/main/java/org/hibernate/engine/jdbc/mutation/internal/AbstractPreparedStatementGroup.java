/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_MESSAGE_LOGGER;

public abstract class AbstractPreparedStatementGroup implements PreparedStatementGroup {
	private final SharedSessionContractImplementor session;

	public AbstractPreparedStatementGroup(SharedSessionContractImplementor session) {
		this.session = session;
	}

	protected void clearBatch(PreparedStatementDetails statementDetails) {
		final PreparedStatement statement = statementDetails.getStatement();
		assert statement != null;

		try {
			// This code can be called after the connection is released
			// and the statement is closed. If the statement is closed,
			// then SQLException will be thrown when PreparedStatement#clearBatch
			// is called.
			// Ensure the statement is not closed before
			// calling PreparedStatement#clearBatch.
			if ( !statement.isClosed() ) {
				statement.clearBatch();
			}
		}
		catch ( SQLException e ) {
			BATCH_MESSAGE_LOGGER.unableToReleaseBatchStatement();
		}
	}

	protected void release(PreparedStatementDetails statementDetails) {
		if ( statementDetails.toRelease() ) {
			if ( statementDetails.getStatement() == null ) {
				BATCH_LOGGER.debugf(
						"PreparedStatementDetails did not contain PreparedStatement on releaseStatements: %s",
						statementDetails.getSqlString()
				);
			}
			else {
				clearBatch( statementDetails );
			}
			statementDetails.releaseStatement( session );
		}
	}
}
