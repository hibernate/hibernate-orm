package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2iDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Onno Goczol
 */
@JiraKey(value = "HHH-15046")
@RequiresDialect(DB2iDialect.class)
public class DB2iDialectInitTestCase {

    @Test
    public void testInitUniqueDelegate() {
        final var db2iDialect = new DB2iDialect();
        assertNotNull(db2iDialect);
    }

}
