/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test of the {@link StandardRefCursorSupport} class.
 *
 * @author Daniel Heinrich
 */
@TestForIssue(jiraKey = "HHH-10612")
public class StandardRefCursorSupportTest {

    interface TestDatabaseMetaData extends DatabaseMetaData {
        boolean supportsRefCursors() throws SQLException;
    }

    @Test
    public void testSupportsRefCursorsAboveJava8() throws Exception {
        TestDatabaseMetaData metaMock = Mockito.mock(TestDatabaseMetaData.class);
        Mockito.when(metaMock.supportsRefCursors()).thenReturn(true);

        boolean result = StandardRefCursorSupport.supportsRefCursors(metaMock);
        assertThat(result, is(true));
    }
}