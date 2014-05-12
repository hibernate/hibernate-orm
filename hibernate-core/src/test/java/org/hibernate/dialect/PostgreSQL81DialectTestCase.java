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

import org.junit.Test;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;


/**
 * Testing of patched support for PostgreSQL Lock error detection. HHH-7251
 *
 * @author Bryan Varner
 */

public class PostgreSQL81DialectTestCase extends BaseUnitTestCase {
	
	@Test
    @TestForIssue( jiraKey = "HHH-7251" )
	public void testDeadlockException() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);
		
		JDBCException exception = delegate.convert(new SQLException("Deadlock Detected", "40P01"), "", "");
		assertTrue(exception instanceof LockAcquisitionException);
	}
	
	@Test
    @TestForIssue( jiraKey = "HHH-7251" )
	public void testTimeoutException() {
		PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);
		
		JDBCException exception = delegate.convert(new SQLException("Lock Not Available", "55P03"), "", "");
		assertTrue(exception instanceof PessimisticLockException);
	}

    @Test
    public void testExtractConstraintName() {
        PostgreSQL81Dialect dialect = new PostgreSQL81Dialect();


        SQLException psqlException = new java.sql.SQLException("ERROR: duplicate key value violates unique constraint \"uk_4bm1x2ultdmq63y3h5r3eg0ej\"  Detail: Key (username, server_config)=(user, 1) already exists.", "23505");
        BatchUpdateException batchUpdateException = new BatchUpdateException("Concurrent Error", "23505", null);
        batchUpdateException.setNextException(psqlException);

        String constraintName = dialect.getViolatedConstraintNameExtracter().extractConstraintName(batchUpdateException);
        assertThat(constraintName, is("uk_4bm1x2ultdmq63y3h5r3eg0ej"));
    }
}
