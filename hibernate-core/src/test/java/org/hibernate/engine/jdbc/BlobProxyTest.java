/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-17770")
class BlobProxyTest {

    @Test
    void testLengthIsNotTruncated() throws SQLException {
        long THREE_GB = 3 * 1024 * 1024 * 1024L;
        Blob blob = BlobProxy.generateProxy(null, THREE_GB);
        assertEquals(THREE_GB, blob.length());
    }
}
