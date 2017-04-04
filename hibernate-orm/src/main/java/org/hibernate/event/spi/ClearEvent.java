/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * An event for {@link org.hibernate.Session#clear()} listening
 *
 * @author Steve Ebersole
 */
public class ClearEvent extends AbstractEvent {
	/**
	 * Constructs an event from the given event session.
	 *
	 * @param source The session event source.
	 */
	public ClearEvent(EventSource source) {
		super( source );
	}
}
