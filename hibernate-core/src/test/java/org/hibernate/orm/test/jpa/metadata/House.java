/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;

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
