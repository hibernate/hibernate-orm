package org.hibernate.engine.jdbc.cursor.internal;

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
public class StandardRefCursorSupportTest {

    interface TestDatabaseMetaData extends DatabaseMetaData {
        boolean supportsRefCursors() throws SQLException;
    }

    @Test
    public void testSupportsRefCursorsAboveJava8() throws Exception {
        DatabaseMetaData metaMock = Mockito.mock(TestDatabaseMetaData.class);

        Method refCursors = DatabaseMetaData.class.getMethod("supportsRefCursors");
        Mockito.when(refCursors.invoke(metaMock)).thenReturn(true);

        boolean result = StandardRefCursorSupport.supportsRefCursors(metaMock);
        assertThat(result, is(true));
    }
}