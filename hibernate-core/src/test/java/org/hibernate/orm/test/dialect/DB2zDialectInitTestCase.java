package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2zDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import java.util.List;

import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.junit.Assert.assertNotNull;

/**
 * @author Onno Goczol
 */
@TestForIssue(jiraKey = "HHH-15046")
@RequiresDialect(DB2zDialect.class)
public class DB2zDialectInitTestCase {

    static class DB2zDialectWithExplicitTimezoneSupport extends DB2zDialect {
        @Override
        protected List<Integer> getSupportedJdbcTypeCodes() {
            return List.of(TIMESTAMP_WITH_TIMEZONE);
        }
    }

    @Test
    public void testInitWithTimezoneSupport() {
        final var db2zDialect = new DB2zDialectWithExplicitTimezoneSupport();
        assertNotNull(db2zDialect);
    }
}
