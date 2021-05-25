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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.message.MessageFormatMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * A {@code Logger} implementation which delegates to Log4J2 but makes it possible
 * to test for events being logged (not logged).
 *
 * @author <a href="mailto:sanne@hibernate.org">Sanne Grinovero</a> (C) 2015 Red Hat Inc.
 */
public final class Log4J2DelegatingLogger extends Logger {

	private final AbstractLogger logger;
	private final MessageFormatMessageFactory messageFactory;

	// Synchronize access on the field
	private final List<LogListener> enabledListeners = new LinkedList<>();
	private final AtomicBoolean interceptEnabled = new AtomicBoolean( false );

	Log4J2DelegatingLogger(final String name) {
		super( name );
		org.apache.logging.log4j.Logger logger = LogManager.getLogger( name );
		if ( !( logger instanceof AbstractLogger ) ) {
			throw new LoggingException( "The logger for [" + name + "] does not extend AbstractLogger. Actual logger: " + logger
					.getClass()
					.getName() );
		}
		this.logger = (AbstractLogger) logger;
		this.messageFactory = new MessageFormatMessageFactory();
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
		return this.logger.isEnabled( translate( level ) );
	}

	@Override
	protected void doLog(
			final Level level,
			final String loggerClassName,
			final Object message,
			final Object[] parameters,
			final Throwable thrown) {
		final org.apache.logging.log4j.Level translatedLevel = translate( level );
		if ( interceptEnabled.get() ) {
			intercept(
					level,
					parameters == null || parameters.length == 0 ?
							String.valueOf( message ) :
							MessageFormat.format( String.valueOf( message ), parameters ),
					thrown
			);
		}
		if ( !this.logger.isEnabled( translatedLevel ) ) {
			return;
		}
		try {
			this.logger.logMessage( loggerClassName, translatedLevel, null,
					( parameters == null || parameters.length == 0 ) ?
							this.messageFactory.newMessage( message ) :
							this.messageFactory.newMessage( String.valueOf( message ), parameters ),
					thrown
			);
		}
		catch (Throwable ignored) {
		}
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
		final org.apache.logging.log4j.Level translatedLevel = translate( level );
		if ( interceptEnabled.get() ) {
			intercept( level, parameters == null ? format : String.format( format, parameters ), thrown );
		}
		if ( !logger.isEnabled( translatedLevel ) ) {
			return;
		}
		try {
			this.logger.logMessage(
					loggerClassName,
					translatedLevel,
					null,
					new StringFormattedMessage( format, parameters ),
					thrown
			);
		}
		catch (Throwable ignored) {
		}
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
		}

		return org.apache.logging.log4j.Level.ALL;
	}

}
