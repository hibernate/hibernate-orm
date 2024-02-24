/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.elementcollection;

import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.OneToMany;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Hotel {
	private int id;
	private Map roomsByName;
	private Map cleaners;

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ElementCollection(targetClass = Room.class)
	@MapKeyClass(String.class)
	public Map getRoomsByName() {
		return roomsByName;
	}

	public void setRoomsByName(Map roomsByName) {
		this.roomsByName = roomsByName;
	}

	@OneToMany(targetEntity = Cleaner.class)
	@MapKeyClass(Room.class)
	public Map getCleaners() {
		return cleaners;
	}

	public void setCleaners(Map cleaners) {
		this.cleaners = cleaners;
	}
}
