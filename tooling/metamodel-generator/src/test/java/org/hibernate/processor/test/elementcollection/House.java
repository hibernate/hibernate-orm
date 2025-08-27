/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.MapKeyColumn;

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
