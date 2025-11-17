/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import org.hibernate.engine.jdbc.proxy.BlobProxy;
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
