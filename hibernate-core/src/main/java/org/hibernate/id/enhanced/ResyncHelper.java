/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;

import java.sql.SQLException;

/**
 * @author Gavin King
 */
class ResyncHelper {

	private static long execute(DdlTransactionIsolator isolator, String sequenceCurrentValue, String message) {
		isolator.getJdbcContext().getSqlStatementLogger().logStatement( sequenceCurrentValue );
		try ( var select = isolator.getIsolatedConnection().prepareStatement( sequenceCurrentValue ) ) {
			try ( var resultSet = select.executeQuery() ) {
				resultSet.next();
				return resultSet.getLong( 1 );
			}
		}
		catch (SQLException e) {
			throw isolator.getJdbcContext().getSqlExceptionHelper()
					.convert( e, message, sequenceCurrentValue );
		}
	}

	static long getNextSequenceValue(DdlTransactionIsolator isolator, String sequenceName) {
		return execute( isolator,
				isolator.getJdbcContext().getDialect().getSequenceSupport()
						.getSequenceNextValString( sequenceName ),
				"Could not fetch the current sequence value from the database" );
	}

	static long getMaxPrimaryKey(DdlTransactionIsolator isolator, String primaryKeyColumnName, String tableName) {
		return execute( isolator,
				"select max(" + primaryKeyColumnName + ") from " + tableName,
				"Could not fetch the max primary key from the database" );
	}

	static long getCurrentTableValue(DdlTransactionIsolator isolator, String tableName, String columnName) {
		return execute( isolator,
				"select " + columnName + " from " + tableName,
				"Could not fetch the current table value from the database" );
	}

	static long getCurrentTableValue(
			DdlTransactionIsolator isolator,
			String tableName, String columnName,
			String segmentColumnName, String segmentValue) {
		return execute( isolator,
				"select " + columnName + " from " + tableName
						+ " where " + segmentColumnName + " = '" + segmentValue + "'",
				"Could not fetch the current table value from the database" );
	}
}
