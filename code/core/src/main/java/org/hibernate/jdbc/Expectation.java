package org.hibernate.jdbc;

import org.hibernate.HibernateException;

import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * Defines an expected DML operation outcome.
 *
 * @author Steve Ebersole
 */
public interface Expectation {
	public void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition) throws SQLException, HibernateException;
	public int prepare(PreparedStatement statement) throws SQLException, HibernateException;
	public boolean canBeBatched();
}
