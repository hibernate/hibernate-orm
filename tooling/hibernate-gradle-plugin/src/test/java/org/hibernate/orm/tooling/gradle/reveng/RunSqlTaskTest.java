/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RunSqlTaskTest {

	@Test
	public void testCreateDelegatingDriver() throws Exception {
		Driver realDriver = new StubDriver();
		Method method = RunSqlTask.class.getDeclaredMethod("createDelegatingDriver", Driver.class);
		method.setAccessible(true);

		// We need an instance but can't easily create a RunSqlTask without a Gradle project.
		// Use ProjectBuilder like RevengTaskTest does.
		org.gradle.api.Project project = org.gradle.testfixtures.ProjectBuilder.builder().build();
		RunSqlTask task = project.getTasks().create("runSql", RunSqlTask.class);

		Driver delegating = (Driver) method.invoke(task, realDriver);
		assertNotNull(delegating);
		// The proxy should delegate acceptsURL to the real driver
		assertFalse(delegating.acceptsURL("jdbc:test:foo"));
	}

	@Test
	public void testCreateDelegatingDriverDelegates() throws Exception {
		StubDriver realDriver = new StubDriver();
		org.gradle.api.Project project = org.gradle.testfixtures.ProjectBuilder.builder().build();
		RunSqlTask task = project.getTasks().create("runSql2", RunSqlTask.class);

		Method method = RunSqlTask.class.getDeclaredMethod("createDelegatingDriver", Driver.class);
		method.setAccessible(true);
		Driver delegating = (Driver) method.invoke(task, (Driver) realDriver);

		// Verify delegation works
		delegating.getMajorVersion();
		assertTrue(realDriver.majorVersionCalled);
	}

	/**
	 * Minimal Driver stub for testing delegation.
	 */
	static class StubDriver implements Driver {
		boolean majorVersionCalled = false;

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			return null;
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return false;
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return new DriverPropertyInfo[0];
		}

		@Override
		public int getMajorVersion() {
			majorVersionCalled = true;
			return 1;
		}

		@Override
		public int getMinorVersion() {
			return 0;
		}

		@Override
		public boolean jdbcCompliant() {
			return false;
		}

		@Override
		public Logger getParentLogger() throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}
	}
}
