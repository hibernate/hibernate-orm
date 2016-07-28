package org.hibernate.jpamodelgen.test.elementcollection;

import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class OfficeBuilding {

	private Map<Integer, byte[]> doorCodes;

	@ElementCollection
	public Map<Integer, byte[]> getDoorCodes() {
		return doorCodes;
	}

	public void setDoorCodes(Map<Integer, byte[]> doorCodes) {
		this.doorCodes = doorCodes;
	}
}
