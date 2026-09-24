/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

/// Centralized logging for details we log as [Logger.Level#INFO] on bootstrap which certain
/// environments routinely want to suppress.  Using the dedicated [category-name][#CATEGORY_NAME]
/// here allows that.
///
/// @since 8.0
/// @author Steve Ebersole
@SubSystemLogging(
		name = BootInfoLogging.CATEGORY_NAME,
		description = "Centralized logging for useful Hibernate ORM information at bootstrap"
)
public class BootInfoLogging {
	public static final String CATEGORY_NAME = SubSystemLogging.BASE + ".info";

	public static final CoreMessageLogger CORE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			CATEGORY_NAME,
			Locale.ROOT
	);

	public static final ConnectionInfoLogger CONNECTION_INFO_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			ConnectionInfoLogger.class,
			CATEGORY_NAME,
			Locale.ROOT
	);

	/// Logs the `HHH000001: Hibernate ORM core version {version}` message using the
	/// {@value #CATEGORY_NAME} category name.
	///
	/// @see CoreMessageLogger#version(String)
	public static void logVersion(String version) {
		CORE_LOGGER.version( version );
	}

	/// Logs the `HHH10001005: Database info: {details}` message using the
	/// {@value #CATEGORY_NAME} category name.
	///
	/// @see ConnectionInfoLogger#logConnectionInfoDetails(String)
	public static void logConnectionInfo(DatabaseConnectionInfo databaseConnectionInfo) {
		CONNECTION_INFO_LOGGER.logConnectionInfoDetails( databaseConnectionInfo.toInfoString() );
	}
}
