/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.jboss.logging.Logger;

public class LoggingEventImpl implements LoggingEvent {
	private final Logger.Level level;
	private final String message;

	public LoggingEventImpl(Logger.Level level, String message) {
		this.level = level;
		this.message = message;
	}

	@Override
	public String toString() {
		return "LoggingEventImpl{" +
				"level=" + level +
				", message='" + message + '\'' +
				'}';
	}

	@Override
	public Logger.Level getLevel() {
		return level;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
