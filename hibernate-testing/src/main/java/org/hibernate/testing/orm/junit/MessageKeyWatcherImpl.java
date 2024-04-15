/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;

import org.jboss.logging.Logger;

/**
 * MessageIdWatcher implementation
 */
public class MessageKeyWatcherImpl implements MessageKeyWatcher, LogListener {
	private final String messageKey;
	private final List<String> loggerNames = new ArrayList<>();
	private final List<String> triggeredMessages = new ArrayList<>();

	public MessageKeyWatcherImpl(String messageKey) {
		this.messageKey = messageKey;
	}

	public void addLoggerName(String name) {
		loggerNames.add( name );
	}

	public void addLogger(org.hibernate.testing.orm.junit.Logger loggerAnn) {
		final Logger logger;
		if ( loggerAnn.loggerNameClass() != void.class ) {
			logger = Logger.getLogger( loggerAnn.loggerNameClass() );
		}
		else if ( ! "".equals( loggerAnn.loggerName().trim() ) ) {
			logger = Logger.getLogger( loggerAnn.loggerName().trim() );
		}
		else {
			throw new IllegalStateException(
					"@LoggingInspections for prefix '" + messageKey +
							"' did not specify proper Logger name.  Use `@LoggingInspections#loggerName" +
							" or `@LoggingInspections#loggerNameClass`"
			);
		}

		LogInspectionHelper.registerListener( this, logger );
	}

	public static String loggerKey(org.hibernate.testing.orm.junit.Logger loggerAnn) {
		final Logger logger;
		if ( loggerAnn.loggerNameClass() != void.class ) {
			logger = Logger.getLogger( loggerAnn.loggerNameClass() );
		}
		else if ( ! "".equals( loggerAnn.loggerName().trim() ) ) {
			logger = Logger.getLogger( loggerAnn.loggerName().trim() );
		}
		else {
			throw new IllegalArgumentException(
					"`@Logger` must specify either `#loggerNameClass` or `#loggerName`"
			);
		}

		return logger.getName();
	}

	public List<String> getLoggerNames() {
		return loggerNames;
	}

	@Override
	public String getMessageKey() {
		return messageKey;
	}

	@Override
	public boolean wasTriggered() {
		return ! triggeredMessages.isEmpty();
	}

	@Override
	public List<String> getTriggeredMessages() {
		return triggeredMessages;
	}

	@Override
	public String getFirstTriggeredMessage() {
		return triggeredMessages.isEmpty() ? null : triggeredMessages.get( 0 );
	}

	@Override
	public void reset() {
		triggeredMessages.clear();
	}

	@Override
	public void loggedEvent(Logger.Level level, String renderedMessage, Throwable thrown) {
		if ( renderedMessage != null ) {
			if ( renderedMessage.startsWith( messageKey ) ) {
				triggeredMessages.add( renderedMessage );
			}
		}
	}

	@Override
	public String toString() {
		return "MessageIdWatcherImpl{" +
				"messageKey='" + messageKey + '\'' +
				", loggerNames=" + loggerNames +
				'}';
	}
}
