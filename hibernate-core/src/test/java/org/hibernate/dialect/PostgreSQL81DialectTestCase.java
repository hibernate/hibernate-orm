/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testing of patched support for PostgreSQL Lock error detection. HHH-7251
 *
 * @author Bryan Varner
 */
public class PostgreSQL81DialectTestCase extends BaseUnitTestCase {
	
	@Test
	public void testDeadlockException() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);
		
		JDBCException exception = delegate.convert(new SQLException("Deadlock Detected", "40P01"), "", "");
		assertTrue(exception instanceof LockAcquisitionException);
	}
	
	@Test
	public void testTimeoutException() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);
		
		JDBCException exception = delegate.convert(new SQLException("Lock Not Available", "55P03"), "", "");
		assertTrue(exception instanceof PessimisticLockException);
	}
	
	/**
	 * Tests that getForUpdateString(String aliases, LockOptions lockOptions) will return a String
	 * that will effect the SELECT ... FOR UPDATE OF tableAlias1, ..., tableAliasN
	 */
	@TestForIssue( jiraKey = "HHH-5654" )
	public void testGetForUpdateStringWithAliasesAndLockOptions() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		LockOptions lockOptions = new LockOptions();
		lockOptions.setAliasSpecificLockMode("tableAlias1", LockMode.PESSIMISTIC_WRITE);
		
		String forUpdateClause = dialect.getForUpdateString("tableAlias1", lockOptions);
		assertTrue("for update of tableAlias1".equals(forUpdateClause));
		
		lockOptions.setAliasSpecificLockMode("tableAlias2", LockMode.PESSIMISTIC_WRITE);
		forUpdateClause = dialect.getForUpdateString("tableAlias1,tableAlias2", lockOptions);
		assertTrue("for update of tableAlias1,tableAlias2".equals(forUpdateClause));
	}

	@Test
	public void testExtractConstraintName() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		SQLException psqlException = new java.sql.SQLException("ERROR: duplicate key value violates unique constraint \"uk_4bm1x2ultdmq63y3h5r3eg0ej\" Detail: Key (username, server_config)=(user, 1) already exists.", "23505");
		BatchUpdateException batchUpdateException = new BatchUpdateException("Concurrent Error", "23505", null);
		batchUpdateException.setNextException(psqlException);
		String constraintName = dialect.getViolatedConstraintNameExtracter().extractConstraintName(batchUpdateException);
		assertThat(constraintName, is("uk_4bm1x2ultdmq63y3h5r3eg0ej"));
	}
}
