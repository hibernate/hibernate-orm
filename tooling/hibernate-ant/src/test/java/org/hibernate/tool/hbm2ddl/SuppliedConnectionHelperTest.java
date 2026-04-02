/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SuppliedConnectionHelperTest {

	@Test
	public void testPrepareGetConnectionRelease() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:supplied_conn_test", "sa", "")) {
			SqlExceptionHelper sqlHelper = new SqlExceptionHelper(true);
			SuppliedConnectionHelper helper = new SuppliedConnectionHelper(conn, sqlHelper);

			helper.prepare(true);
			assertNotNull(helper.getConnection());

			helper.release();
		}
	}

	@Test
	public void testPrepareWithoutAutoCommit() throws Exception {
		try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:supplied_conn_test2", "sa", "")) {
			SqlExceptionHelper sqlHelper = new SqlExceptionHelper(true);
			SuppliedConnectionHelper helper = new SuppliedConnectionHelper(conn, sqlHelper);

			helper.prepare(false);
			assertNotNull(helper.getConnection());

			helper.release();
		}
	}
}
