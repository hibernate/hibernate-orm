/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.schema;

import org.hibernate.testing.cleaner.DatabaseCleaner;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * This listener should be called before the discovery request is sent to the test engines.
 * <p>
 * Note, it is on purpose not registered as a service within the {@code hibernate-testing} (i.e. in {@code META-INF/services} of this jar).
 * This is to prevent this listener be invoked by non Hibernate ORM users of the {@code hibernate-testing} lib.
 * <p>
 * See also <a href="https://junit.org/junit5/docs/current/user-guide/#launcher-api-launcher-session-listeners-tool-example-usage">...</a>
 */
public class CheckClearSchemaListener implements LauncherSessionListener {

	@Override
	public void launcherSessionOpened(LauncherSession session) {
		DatabaseCleaner.clearSchemas();
	}
}
