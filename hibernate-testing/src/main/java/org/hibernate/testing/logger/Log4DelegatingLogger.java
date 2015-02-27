/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.logger;

import org.jboss.logging.Logger;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@code Logger} implementation which delegates to Log4J but makes it possible
 * to test for events being logged (not logged).
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2015 Red Hat Inc.
 */
public final class Log4DelegatingLogger extends Logger {

	private final org.apache.log4j.Logger logger;

	// Synchronize access on the field
	private final List<LogListener> enabledListeners = new LinkedList<LogListener>();

	Log4DelegatingLogger(final String name) {
		super( name );
		logger = org.apache.log4j.Logger.getLogger( name );
	}

	void registerListener(LogListener newListener) {
		synchronized ( enabledListeners ) {
			if ( newListener != null ) {
				enabledListeners.add( newListener );
			}
		}
	}

	void clearAllListeners() {
		synchronized ( enabledListeners ) {
			enabledListeners.clear();
		}
	}

	public boolean isEnabled(final Level level) {
		final org.apache.log4j.Level l = translate( level );
		return logger.isEnabledFor( l ) && l.isGreaterOrEqual( logger.getEffectiveLevel() );
	}

	protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
		final org.apache.log4j.Level translatedLevel = translate( level );
		intercept( level, parameters == null || parameters.length == 0 ? String.valueOf( message ) : MessageFormat.format( String.valueOf( message ), parameters ), thrown );
		if ( logger.isEnabledFor( translatedLevel ) )
			try {
				logger.log( loggerClassName, translatedLevel,
						parameters == null || parameters.length == 0 ? String.valueOf( message ) : MessageFormat.format( String.valueOf( message ), parameters ), thrown );
			}
			catch (Throwable ignored) {
			}
	}

	private void intercept(Level level, String renderedMessage, Throwable thrown) {
		synchronized ( enabledListeners ) {
			for ( LogListener listener : enabledListeners ) {
				listener.loggedEvent( level, renderedMessage, thrown );
			}
		}
	}

	protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
		final org.apache.log4j.Level translatedLevel = translate( level );
		intercept( level, parameters == null ? String.format( format ) : String.format( format, parameters ), thrown );
		if ( logger.isEnabledFor( translatedLevel ) )
			try {
				logger.log( loggerClassName, translatedLevel, parameters == null ? String.format( format ) : String.format( format, parameters ), thrown );
			}
			catch (Throwable ignored) {
			}
	}

	private static org.apache.log4j.Level translate(final Level level) {
		if ( level != null )
			switch ( level ) {
				case FATAL:
					return org.apache.log4j.Level.FATAL;
				case ERROR:
					return org.apache.log4j.Level.ERROR;
				case WARN:
					return org.apache.log4j.Level.WARN;
				case INFO:
					return org.apache.log4j.Level.INFO;
				case DEBUG:
					return org.apache.log4j.Level.DEBUG;
				case TRACE:
					return org.apache.log4j.Level.TRACE;
			}
		return org.apache.log4j.Level.ALL;
	}

}
