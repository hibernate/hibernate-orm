/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author Gavin King
 */
class ResyncHelper {

	// TODO: Use SqlExceptionHelper, SqlStatementLogger, available in the JdbcContext

	private static long resultValue(PreparedStatement select) throws SQLException {
		try ( var resultSet = select.executeQuery() ) {
			resultSet.next();
			return resultSet.getLong( 1 );
		}
	}

	static long getNextSequenceValue(Connection connection, String sequenceName, Dialect dialect) {
		final String sequenceCurrentValue =
				dialect.getSequenceSupport()
						.getSequenceNextValString( sequenceName );
		try ( var select = connection.prepareStatement( sequenceCurrentValue ) ) {
			return resultValue( select );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the current sequence value from the database", e );
		}
	}

	static long getMaxPrimaryKey(Connection connection, String primaryKeyColumnName, String tableName) {
		final String selectMax =
				"select max(" + primaryKeyColumnName + ") from " + tableName;
		try ( var select = connection.prepareStatement( selectMax ) ) {
			return resultValue( select );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the max primary key from the database", e );
		}
	}

	static long getCurrentTableValue(Connection connection, String tableName, String columnName) {
		final String selectCurrent =
				"select " + columnName + " from " + tableName;
		try ( var select = connection.prepareStatement( selectCurrent ) ) {
			return resultValue( select );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the current table value from the database", e );
		}
	}

	static long getCurrentTableValue(
			Connection connection, String tableName, String columnName,
			String segmentColumnName, String segmentValue) {
		final String selectCurrent =
				"select " + columnName + " from " + tableName
					+ " where " + segmentColumnName + " = '" + segmentValue + "'";
		try ( var select = connection.prepareStatement( selectCurrent ) ) {
			return resultValue( select );
		}
		catch (SQLException e) {
			throw new HibernateException( "Could not fetch the current table value from the database", e );
		}
	}
}
