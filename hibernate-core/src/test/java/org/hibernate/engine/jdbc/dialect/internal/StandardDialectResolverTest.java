/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.engine.jdbc.dialect.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test of the {@link StandardDialectResolver} class.
 *
 * @author Bryan Turner
 */
public class StandardDialectResolverTest extends BaseUnitTestCase {

    @Test
    public void testResolveDialectInternalForPostgres81() throws SQLException {
        runPostgresDialectTest(8, 1, PostgreSQL81Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres82() throws SQLException {
        runPostgresDialectTest(8, 2, PostgreSQL82Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres83() throws SQLException {
        runPostgresDialectTest(8, 3, PostgreSQL82Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres84() throws SQLException {
        runPostgresDialectTest(8, 4, PostgreSQL82Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres9() throws SQLException {
        runPostgresDialectTest(9, 0, PostgreSQL82Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres91() throws SQLException {
        runPostgresDialectTest(9, 1, PostgreSQL82Dialect.class);
    }

    @Test
    public void testResolveDialectInternalForPostgres92() throws SQLException {
        runPostgresDialectTest(9, 2, PostgreSQL82Dialect.class);
    }

    private static void runPostgresDialectTest(int majorVersion, int minorVersion,
                                               Class<? extends Dialect> expectedDialect) throws SQLException {
        runDialectTest("PostgreSQL", majorVersion, minorVersion, expectedDialect);
    }

    private static void runDialectTest(String productName, int majorVersion, int minorVersion,
                                       Class<? extends Dialect> expectedDialect) throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        when(metaData.getDatabaseMajorVersion()).thenReturn(majorVersion);
        when(metaData.getDatabaseMinorVersion()).thenReturn(minorVersion);

        Dialect dialect = new StandardDialectResolver().resolveDialectInternal(metaData);

        StringBuilder builder = new StringBuilder(productName)
                .append(" ")
                .append(majorVersion);
        if (minorVersion > 0) {
            builder.append(".").append(minorVersion);
        }
        String dbms = builder.toString();

        assertNotNull("Dialect for " + dbms + " should not be null", dialect);
        assertTrue("Dialect for " + dbms + " should be " + expectedDialect.getSimpleName(),
                expectedDialect.isInstance(dialect));
    }
}
