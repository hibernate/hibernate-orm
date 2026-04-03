/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class ManagedProviderConnectionHelperTest {

	private Properties h2Properties() {
		Properties props = new Properties();
		props.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
		props.setProperty("hibernate.connection.url",
				"jdbc:h2:mem:managed_helper_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
		props.setProperty("hibernate.connection.username", "sa");
		props.setProperty("hibernate.connection.password", "");
		props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		return props;
	}

	@Test
	public void testPrepareAndGetConnection() throws SQLException {
		ManagedProviderConnectionHelper helper = new ManagedProviderConnectionHelper(h2Properties());
		helper.prepare(false);
		Connection conn = helper.getConnection();
		assertNotNull(conn);
		helper.release();
	}

	@Test
	public void testPrepareWithAutoCommit() throws SQLException {
		ManagedProviderConnectionHelper helper = new ManagedProviderConnectionHelper(h2Properties());
		helper.prepare(true);
		Connection conn = helper.getConnection();
		assertNotNull(conn);
		assertTrue(conn.getAutoCommit());
		helper.release();
	}

	@Test
	public void testReleaseWithoutPrepare() throws SQLException {
		ManagedProviderConnectionHelper helper = new ManagedProviderConnectionHelper(h2Properties());
		// release without prepare should not throw (connection and serviceRegistry are null)
		helper.release();
	}
}
