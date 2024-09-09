package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2zDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Onno Goczol
 */
@JiraKey(value = "HHH-15046")
@RequiresDialect(DB2zDialect.class)
public class DB2zDialectInitTestCase {

    static class DB2zDialectWithExplicitTimezoneSupport extends DB2zDialect {
	}

    @Test
    public void testInitWithTimezoneSupport() {
        final var db2zDialect = new DB2zDialectWithExplicitTimezoneSupport();
        assertNotNull(db2zDialect);
    }
}
