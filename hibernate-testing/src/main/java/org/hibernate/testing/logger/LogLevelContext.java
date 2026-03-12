/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.hibernate.Incubating;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

@Incubating
public class LogLevelContext implements AutoCloseable {

	private final String name;
	private final org.apache.logging.log4j.Level originalLevel;

	private LogLevelContext(String name, Logger.Level targetLevel) {
		this.name = name;
		this.originalLevel = getLogLevel( name );
		doSetRuntimeLevel( name, targetLevel );
	}

	public static LogLevelContext withLevel(Class<?> loggerName, Logger.Level level) {
		return withLevel( loggerName.getName(), level );
	}

	public static LogLevelContext withLevel(String loggerName, Logger.Level level) {
		return new LogLevelContext( loggerName, level );
	}

	@Override
	public void close() {
		doSetRuntimeLevel( name, originalLevel );
	}

	private static void doSetRuntimeLevel(String name, Object level) {
		Assertions.assertNotNull( name );
		Assertions.assertNotNull( level );
		Object logger = Logger.getLogger( name );
		if ( logger instanceof DelegatingLogger dl ) {
			if ( level instanceof Logger.Level l ) {
				dl.setLevel( l );
			}
			else if ( level instanceof org.apache.logging.log4j.Level l ) {
				dl.setLevel( l );
			}
			else {
				throw new AssertionError( "Unexpected logger level type: " + level.getClass().getName() );
			}
		}
		else {
			throw new AssertionError( "Unexpected logger type: " + logger.getClass()
					.getName() + ". Logger must be: " + DelegatingLogger.class.getName() );
		}
	}

	private static org.apache.logging.log4j.Level getLogLevel(String name) {
		Object logger = Logger.getLogger( name );
		if ( logger instanceof DelegatingLogger dl ) {
			return dl.getLevel();
		}
		return org.apache.logging.log4j.Level.INFO;
	}
}
