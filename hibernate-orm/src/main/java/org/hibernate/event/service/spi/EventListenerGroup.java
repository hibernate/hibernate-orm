/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

import java.io.Serializable;

import org.hibernate.event.spi.EventType;

/**
 * Contract for a groups of events listeners for a particular event type.
 *
 * @author Steve Ebersole
 */
public interface EventListenerGroup<T> extends Serializable {

	/**
	 * Retrieve the event type associated with this groups of listeners.
	 *
	 * @return The event type.
	 */
	public EventType<T> getEventType();

	/**
	 * Are there no listeners registered?
	 *
	 * @return {@literal true} if no listeners are registered; {@literal false} otherwise.
	 */
	public boolean isEmpty();

	public int count();

	public Iterable<T> listeners();

	/**
	 * Mechanism to more finely control the notion of duplicates.
	 * <p/>
	 * For example, say you are registering listeners for an extension library.  This extension library
	 * could define a "marker interface" which indicates listeners related to it and register a strategy
	 * that checks against that marker interface.
	 *
	 * @param strategy The duplication strategy
	 */
	public void addDuplicationStrategy(DuplicationStrategy strategy);

	public void appendListener(T listener);
	public void appendListeners(T... listeners);

	public void prependListener(T listener);
	public void prependListeners(T... listeners);

	public void clear();

}
