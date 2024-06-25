/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Steve Ebersole
 */
public interface MessageKeyWatcher {
	String getMessageKey();

	boolean wasTriggered();

	List<LoggingEvent> getTriggeredEvents();

	default List<String> getTriggeredMessages() {
		return getTriggeredEvents().stream().map( LoggingEvent::getMessage ).collect( Collectors.toList() );
	}

	LoggingEvent getFirstTriggeredEvent();

	default String getFirstTriggeredMessage() {
		LoggingEvent firstEvent = getFirstTriggeredEvent();
		return firstEvent == null ? null : firstEvent.getMessage();
	}

	void reset();

}
