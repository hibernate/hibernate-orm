/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.elementcollection;

import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.MapKeyColumn;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class House {
	private Map<String, Room> roomsByName;

	@ElementCollection(targetClass = Room.class)
	@MapKeyColumn(name = "room_name")
	public Map<String, Room> getRoomsByName() {
		return roomsByName;
	}

	public void setRoomsByName(Map<String, Room> roomsByName) {
		this.roomsByName = roomsByName;
	}
}


