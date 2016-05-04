/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity(name="House")
public class House {
	private Key key;
	private Address address;
	private Set<Room> rooms;
	private Map<String, Room> roomsByName;
	private List<Room> roomsBySize;

	@ElementCollection
	@OrderColumn(name = "size_order")
	public List<Room> getRoomsBySize() {
		return roomsBySize;
	}

	public void setRoomsBySize(List<Room> roomsBySize) {
		this.roomsBySize = roomsBySize;
	}

	@ElementCollection
	@MapKeyColumn(name="room_name")
	public Map<String, Room> getRoomsByName() {
		return roomsByName;
	}

	public void setRoomsByName(Map<String, Room> roomsByName) {
		this.roomsByName = roomsByName;
	}

	@ElementCollection
	public Set<Room> getRooms() {
		return rooms;
	}

	public void setRooms(Set<Room> rooms) {
		this.rooms = rooms;
	}

	@EmbeddedId
	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public static class Key implements Serializable {
		private String uuid;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}
}
