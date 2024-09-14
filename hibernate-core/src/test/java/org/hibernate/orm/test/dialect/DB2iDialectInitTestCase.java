/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
