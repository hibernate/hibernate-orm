/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Manages all of the MessageKeyWatcher defined by LoggingInspectionsScope
 */
public class LoggingInspectionsScope {
	private final Map<String, Map<String,MessageKeyWatcherImpl>> watcherMap = new HashMap<>();


	public LoggingInspectionsScope(LoggingInspections loggingInspections, ExtensionContext context) {
		for ( int i = 0; i < loggingInspections.messages().length; i++ ) {
			final LoggingInspections.Message message = loggingInspections.messages()[ i ];

			final String messageKey = message.messageKey().trim();
			assert ! messageKey.isEmpty();

			if ( message.loggers().length == 0 ) {
				return;
			}

			final Map<String, MessageKeyWatcherImpl> messageKeyWatcherMap;
			final Map<String, MessageKeyWatcherImpl> existingMessageKeyWatcherMap = watcherMap.get( messageKey );
			if ( existingMessageKeyWatcherMap != null ) {
				messageKeyWatcherMap = existingMessageKeyWatcherMap;
			}
			else {
				messageKeyWatcherMap = new HashMap<>();
				watcherMap.put( messageKey, messageKeyWatcherMap );
			}

			for ( Logger logger : message.loggers() ) {
				final String loggerKey = MessageKeyWatcherImpl.loggerKey( logger );
				final MessageKeyWatcherImpl watcher;
				final MessageKeyWatcherImpl existingWatcher = messageKeyWatcherMap.get( loggerKey );
				if ( existingWatcher != null ) {
					watcher = existingWatcher;
				}
				else {
					watcher = new MessageKeyWatcherImpl( messageKey );
					messageKeyWatcherMap.put( loggerKey, watcher );
				}
				watcher.addLogger( logger );
			}
		}
	}

	public void resetWatchers() {
		watcherMap.forEach(
				(messageKey,loggerMap) -> loggerMap.forEach( (logger,watcher) -> watcher.reset() )
		);
	}

	public MessageKeyWatcher getWatcher(String messageKey, String loggerName) {
		final Map<String, MessageKeyWatcherImpl> messageKeyWatcherMap = watcherMap.get( messageKey );
		return messageKeyWatcherMap.get( loggerName );
	}

	public MessageKeyWatcher getWatcher(String messageKey, Class<?> loggerNameClass) {
		final Map<String, MessageKeyWatcherImpl> messageKeyWatcherMap = watcherMap.get( messageKey );
		return messageKeyWatcherMap.get( loggerNameClass.getName() );
	}
}
