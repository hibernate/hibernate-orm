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
