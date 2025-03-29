/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

/**
 * @author Chris Cranford
 */
@Entity @Access(AccessType.FIELD)
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
