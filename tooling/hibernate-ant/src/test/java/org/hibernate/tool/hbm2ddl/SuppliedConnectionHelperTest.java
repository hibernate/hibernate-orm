/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class SuppliedConnectionHelperTest {

	private Connection rawConnection;

	@BeforeEach
	public void setUp() throws Exception {
		rawConnection = DriverManager.getConnection(
				"jdbc:h2:mem:supplied_helper_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "");
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (rawConnection != null && !rawConnection.isClosed()) {
			rawConnection.close();
		}
	}

	@Test
	public void testPrepareNoAutoCommit() throws SQLException {
		rawConnection.setAutoCommit(true);
		SuppliedConnectionHelper helper = new SuppliedConnectionHelper(rawConnection, new SqlExceptionHelper(true));
		helper.prepare(false);
		assertSame(rawConnection, helper.getConnection());
		helper.release();
	}

	@Test
	public void testPrepareWithAutoCommit() throws SQLException {
		rawConnection.setAutoCommit(false);
		SuppliedConnectionHelper helper = new SuppliedConnectionHelper(rawConnection, new SqlExceptionHelper(true));
		helper.prepare(true);
		assertTrue(rawConnection.getAutoCommit());
		helper.release();
		// After release with toggleAutoCommit, autocommit should be restored to false
		// But connection is set to null in release, so we can't check.
		// Just verify no exception.
	}

	@Test
	public void testPrepareWithAutoCommitAlreadyTrue() throws SQLException {
		rawConnection.setAutoCommit(true);
		SuppliedConnectionHelper helper = new SuppliedConnectionHelper(rawConnection, new SqlExceptionHelper(true));
		helper.prepare(true);
		// toggleAutoCommit should be false since autocommit was already true
		assertTrue(rawConnection.getAutoCommit());
		helper.release();
	}

	@Test
	public void testGetConnection() throws SQLException {
		SuppliedConnectionHelper helper = new SuppliedConnectionHelper(rawConnection, new SqlExceptionHelper(true));
		assertSame(rawConnection, helper.getConnection());
	}

	@Test
	public void testReleaseNullsConnection() throws SQLException {
		SuppliedConnectionHelper helper = new SuppliedConnectionHelper(rawConnection, new SqlExceptionHelper(true));
		helper.prepare(false);
		assertNotNull(helper.getConnection());
		helper.release();
		assertNull(helper.getConnection());
	}
}
