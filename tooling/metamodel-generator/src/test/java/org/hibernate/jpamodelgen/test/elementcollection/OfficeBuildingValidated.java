/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.elementcollection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
 * @author Chris Cranford
 */
@Entity
public class OfficeBuildingValidated {

	// mock a bean validation annotation using TYPE_USE
	@Target({ ElementType.TYPE_USE })
	@Retention(RetentionPolicy.RUNTIME)
	public @interface NotNullAllowed {

	}

	@ElementCollection
	@NotNullAllowed
	private Map<@NotNullAllowed Integer, @NotNullAllowed byte[]> doorCodes;

	@ElementCollection
	@NotNullAllowed
	private Set<@NotNullAllowed String> computerSerialNumbers;

	@ElementCollection
	@NotNullAllowed
	private List<@NotNullAllowed String> employeeNames;

	@ElementCollection
	@NotNullAllowed
	private List<@NotNullAllowed Room> rooms;

	public Map<Integer, byte[]> getDoorCodes() {
		return doorCodes;
	}

	public void setDoorCodes(Map<Integer, byte[]> doorCodes) {
		this.doorCodes = doorCodes;
	}

	public Set<String> getComputerSerialNumbers() {
		return computerSerialNumbers;
	}

	public void setComputerSerialNumbers(Set<String> computerSerialNumbers) {
		this.computerSerialNumbers = computerSerialNumbers;
	}

	public List<String> getEmployeeNames() {
		return employeeNames;
	}

	public void setEmployeeNames(List<String> employeeNames) {
		this.employeeNames = employeeNames;
	}

	public List<Room> getRooms() {
		return rooms;
	}

	public void setRooms(List<Room> rooms) {
		this.rooms = rooms;
	}
}
