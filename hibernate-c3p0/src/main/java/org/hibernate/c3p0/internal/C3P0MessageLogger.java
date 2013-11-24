/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.c3p0.internal;

import java.sql.SQLException;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-c3p0 module.  It reserves message ids ranging from
 * 10001 to 15000 inclusively.
 * <p/>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
public interface C3P0MessageLogger extends CoreMessageLogger {

	/**
	 * Log a message (WARN) about conflicting {@code hibernate.c3p0.XYZ} and {@code c3p0.XYZ} settings
	 *
	 * @param hibernateStyle The {@code hibernate.c3p0} prefixed setting
	 * @param c3p0Style The {@code c3p0.} prefixed setting
	 */
	@LogMessage(level = WARN)
	@Message(value = "Both hibernate-style property '%1$s' and c3p0-style property '%2$s' have been set in Hibernate "
			+ "properties.  Hibernate-style property '%1$s' will be used and c3p0-style property '%2$s' will be ignored!", id = 10001)
	void bothHibernateAndC3p0StylesSet(String hibernateStyle,String c3p0Style);

	/**
	 * Log a message (INFO) about which Driver class is being used.
	 *
	 * @param jdbcDriverClass The JDBC Driver class
	 * @param jdbcUrl The JDBC URL
	 */
	@LogMessage(level = INFO)
	@Message(value = "C3P0 using driver: %s at URL: %s", id = 10002)
	void c3p0UsingDriver(String jdbcDriverClass, String jdbcUrl);

	/**
	 * Build a message about not being able to find the JDBC driver class
	 *
	 * @param jdbcDriverClass The JDBC driver class we could not find
	 *
	 * @return The message
	 */
	@Message(value = "JDBC Driver class not found: %s", id = 10003)
	String jdbcDriverNotFound(String jdbcDriverClass);

	/**
	 * Log a message (WARN) about not being able to stop the underlying c3p0 pool.
	 *
	 * @param e The exception when we tried to stop pool
	 */
	@LogMessage(level = WARN)
	@Message(value = "Could not destroy C3P0 connection pool", id = 10004)
	void unableToDestroyC3p0ConnectionPool(@Cause SQLException e);

	/**
	 * Build a message about not being able to start the underlying c3p0 pool.
	 *
	 * @return The message
	 */
	@Message(value = "Could not instantiate C3P0 connection pool", id = 10005)
	String unableToInstantiateC3p0ConnectionPool();
}
