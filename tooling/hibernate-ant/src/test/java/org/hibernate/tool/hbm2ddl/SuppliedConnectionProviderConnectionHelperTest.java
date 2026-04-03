/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class SuppliedConnectionProviderConnectionHelperTest {

	private Connection rawConnection;
	private final String url = "jdbc:h2:mem:supplied_provider_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

	@BeforeEach
	public void setUp() throws Exception {
		rawConnection = DriverManager.getConnection(url, "sa", "");
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (rawConnection != null && !rawConnection.isClosed()) {
			rawConnection.close();
		}
	}

	private ConnectionProvider stubProvider() {
		return new ConnectionProvider() {
			@Override
			public Connection getConnection() {
				return rawConnection;
			}
			@Override
			public void closeConnection(Connection connection) throws SQLException {
				// don't actually close — we manage lifecycle in tearDown
			}
			@Override
			public boolean supportsAggressiveRelease() {
				return false;
			}
			@Override
			public boolean isUnwrappableAs(Class<?> unwrapType) {
				return false;
			}
			@Override
			public <T> T unwrap(Class<T> unwrapType) {
				return null;
			}
		};
	}

	@Test
	public void testPrepareNoAutoCommitNeeded() throws SQLException {
		rawConnection.setAutoCommit(true);
		SuppliedConnectionProviderConnectionHelper helper =
				new SuppliedConnectionProviderConnectionHelper(stubProvider(), new SqlExceptionHelper(true));
		helper.prepare(false);
		Connection conn = helper.getConnection();
		assertNotNull(conn);
		assertSame(rawConnection, conn);
		helper.release();
	}

	@Test
	public void testPrepareWithAutoCommit() throws SQLException {
		rawConnection.setAutoCommit(false);
		SuppliedConnectionProviderConnectionHelper helper =
				new SuppliedConnectionProviderConnectionHelper(stubProvider(), new SqlExceptionHelper(true));
		helper.prepare(true);
		Connection conn = helper.getConnection();
		assertNotNull(conn);
		assertTrue(conn.getAutoCommit());
		helper.release();
	}

	@Test
	public void testReleaseWithoutPrepare() throws SQLException {
		SuppliedConnectionProviderConnectionHelper helper =
				new SuppliedConnectionProviderConnectionHelper(stubProvider(), new SqlExceptionHelper(true));
		// connection is null, should not throw
		helper.release();
	}
}
