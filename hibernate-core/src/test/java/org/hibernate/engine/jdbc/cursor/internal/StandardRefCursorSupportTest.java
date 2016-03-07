package org.hibernate.engine.jdbc.cursor.internal;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test of the {@link StandardRefCursorSupport} class.
 *
 * @author Daniel Heinrich
 */
@TestForIssue(jiraKey = "OGM-986")
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