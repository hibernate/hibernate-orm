/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;

/**
 * Defines an expected DML operation outcome.
 *
 * @author Steve Ebersole
 */
public interface Expectation {
	/**
	 * Perform verification of the outcome of the RDBMS operation based on
	 * the type of expectation defined.
	 *
	 * @param rowCount The RDBMS reported "number of rows affected".
	 * @param statement The statement representing the operation
	 * @param batchPosition The position in the batch (if batching)
	 * @param statementSQL The SQL backing the prepared statement, for logging purposes
	 * @throws SQLException Exception from the JDBC driver
	 * @throws HibernateException Problem processing the outcome.
	 */
	public void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String statementSQL) throws SQLException, HibernateException;

	/**
	 * Perform any special statement preparation.
	 *
	 * @param statement The statement to be prepared
	 * @return The number of bind positions consumed (if any)
	 * @throws SQLException Exception from the JDBC driver
	 * @throws HibernateException Problem performing preparation.
	 */
	public int prepare(PreparedStatement statement) throws SQLException, HibernateException;

	/**
	 * Is it acceptable to combiner this expectation with statement batching?
	 *
	 * @return True if batching can be combined with this expectation; false otherwise.
	 */
	public boolean canBeBatched();
}
