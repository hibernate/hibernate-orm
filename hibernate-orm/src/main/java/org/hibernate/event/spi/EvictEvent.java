/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 *  Defines an event class for the evicting of an entity.
 *
 * @author Steve Ebersole
 */
public class EvictEvent extends AbstractEvent {

	private Object object;

	public EvictEvent(Object object, EventSource source) {
		super(source);
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
