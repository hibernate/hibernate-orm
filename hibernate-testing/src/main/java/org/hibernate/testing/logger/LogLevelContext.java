/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.hibernate.Incubating;
import org.jboss.logging.Logger;

@Incubating
public class LogLevelContext implements AutoCloseable {

	private final String name;
	private final org.apache.logging.log4j.Level originalLevel;

	private LogLevelContext(String name, Logger.Level targetLevel) {
		this.name = name;
		this.originalLevel = getLogLevel( name );
		setRuntimeLevel( name, targetLevel );
	}

	public static LogLevelContext withLevel(String loggerName, Logger.Level level) {
		return new LogLevelContext( loggerName, level );
	}

	@Override
	public void close() {
		setRuntimeLevel( name, originalLevel );
	}

	private static void setRuntimeLevel(String name, Logger.Level level) {
		Object logger = Logger.getLogger( name );
		if ( logger instanceof DelegatingLogger dl ) {
			dl.setLevel( level );
		}
	}

	private static void setRuntimeLevel(String name, org.apache.logging.log4j.Level level) {
		Object logger = Logger.getLogger( name );
		if ( logger instanceof DelegatingLogger dl ) {
			dl.setLevel( level );
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
