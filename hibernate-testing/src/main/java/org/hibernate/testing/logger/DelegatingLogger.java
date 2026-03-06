/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.jboss.logging.Logger;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@code Logger} implementation which delegates to Log4J2 but makes it possible
 * to test for events being logged (not logged).
 *
 * @author Sanne Grinovero (C) 2015 Red Hat Inc.
 */
public final class DelegatingLogger extends Logger {

	private final Logger delegate;

	// Synchronize access on the field
	private final List<LogListener> enabledListeners = new LinkedList<>();
	private final AtomicBoolean interceptEnabled = new AtomicBoolean( false );

	DelegatingLogger(final Logger logger) {
		super( logger.getName() );
		this.delegate = logger;
	}

	void registerListener(LogListener newListener) {
		synchronized (enabledListeners) {
			if ( newListener != null ) {
				enabledListeners.add( newListener );
				interceptEnabled.set( true );
			}
		}
	}

	void clearAllListeners() {
		synchronized (enabledListeners) {
			enabledListeners.clear();
			interceptEnabled.set( false );
		}
	}

	@Override
	public boolean isEnabled(final Level level) {
		return this.delegate.isEnabled( level );
	}

	@Override
	protected void doLog(
			final Level level,
			final String loggerClassName,
			final Object message,
			final Object[] parameters,
			final Throwable thrown) {
		if ( interceptEnabled.get() ) {
			intercept(
					level,
					(parameters == null || parameters.length == 0) ?
							String.valueOf( message ) :
							MessageFormat.format( String.valueOf( message ), parameters ),
					thrown
			);
		}

		if ( !this.delegate.isEnabled( level ) ) {
			return;
		}

		this.delegate.log( loggerClassName, level, message, parameters, thrown );
	}

	private void intercept(Level level, String renderedMessage, Throwable thrown) {
		synchronized (enabledListeners) {
			for ( LogListener listener : enabledListeners ) {
				listener.loggedEvent( level, renderedMessage, thrown );
			}
		}
	}

	@Override
	protected void doLogf(
			final Level level,
			final String loggerClassName,
			final String format,
			final Object[] parameters,
			final Throwable thrown) {

		if ( interceptEnabled.get() ) {
			intercept( level, parameters == null ? format : String.format( format, parameters ), thrown );
		}

		if ( !delegate.isEnabled( level ) ) {
			return;
		}

		this.delegate.logf( loggerClassName, level, thrown, format, parameters );
	}

	private static org.apache.logging.log4j.Level translate(final Level level) {
		if ( level == null ) {
			return org.apache.logging.log4j.Level.ALL;
		}

		switch ( level ) {
			case FATAL:
				return org.apache.logging.log4j.Level.FATAL;
			case ERROR:
				return org.apache.logging.log4j.Level.ERROR;
			case WARN:
				return org.apache.logging.log4j.Level.WARN;
			case INFO:
				return org.apache.logging.log4j.Level.INFO;
			case DEBUG:
				return org.apache.logging.log4j.Level.DEBUG;
			case TRACE:
				return org.apache.logging.log4j.Level.TRACE;
			default:
				return org.apache.logging.log4j.Level.INFO;
		}
	}

	org.apache.logging.log4j.Level getLevel() {
		return LogManager.getLogger( getName() ).getLevel();
	}

	void setLevel(org.apache.logging.log4j.Level level) {
		((LoggerContext) LogManager.getContext( false ))
				.getConfiguration()
				.getLoggerConfig( getName() )
				.setLevel( level );
	}

	void setLevel(Level level) {
		setLevel( translate( level ) );
	}
}
