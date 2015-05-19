/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;

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
		if ( !logger.isEnabledFor( translatedLevel ) ) {
			return;
		}
		try {
			logger.log(
					loggerClassName,
					translatedLevel,
					parameters == null || parameters.length == 0
							? String.valueOf( message )
							: MessageFormat.format(String.valueOf( message ), parameters ),
					thrown
			);
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
		intercept( level, parameters == null ? format : String.format( format, parameters ), thrown );
		if ( !logger.isEnabledFor( translatedLevel ) ) {
			return;
		}
		try {
			logger.log(
					loggerClassName,
					translatedLevel,
					parameters == null ? format : String.format( format, parameters ),
					thrown
			);
		}
		catch (Throwable ignored) {
		}
	}

	private static org.apache.log4j.Level translate(final Level level) {
		if ( level == null ) {
			return org.apache.log4j.Level.ALL;
		}

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
