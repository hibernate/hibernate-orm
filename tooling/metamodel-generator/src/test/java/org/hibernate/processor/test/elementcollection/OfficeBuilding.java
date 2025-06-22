/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OfficeBuilding {

	private long id;
	private Map<Integer, byte[]> doorCodes;

	@Id
	public long getId() {
		return id;
	}

	@ElementCollection
	public Map<Integer, byte[]> getDoorCodes() {
		return doorCodes;
	}

	public void setDoorCodes(Map<Integer, byte[]> doorCodes) {
		this.doorCodes = doorCodes;
	}
}
