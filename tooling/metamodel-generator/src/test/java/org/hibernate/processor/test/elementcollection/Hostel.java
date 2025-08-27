/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyClass;


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
