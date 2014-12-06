/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;


/**
 * Testing of patched support for PostgreSQL Lock error detection. HHH-7251
 *
 * @author Bryan Varner
 */
@TestForIssue( jiraKey = "HHH-7251" )
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
}
