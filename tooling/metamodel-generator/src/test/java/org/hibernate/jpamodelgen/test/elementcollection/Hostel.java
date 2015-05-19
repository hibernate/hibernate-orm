/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.elementcollection;

import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.MapKeyClass;


/**
 * @author Hardy Ferentschik
 */
public class Hostel {
	private Map roomsByName;

	private Map cleaners;

	@ElementCollection(targetClass = Room.class)
	@MapKeyClass(String.class)
	public Map getRoomsByName() {
		return roomsByName;
	}

	public void setRoomsByName(Map roomsByName) {
		this.roomsByName = roomsByName;
	}

	public Map getCleaners() {
		return cleaners;
	}

	public void setCleaners(Map cleaners) {
		this.cleaners = cleaners;
	}
}
