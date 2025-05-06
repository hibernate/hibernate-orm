/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.timestamp;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Person {
	private String name;
	private Date dob;
	private String currentAddress;
	Person() {}
	public Person(String name, Date dob, String address) {
		this.name = name;
		this.dob = dob;
		this.currentAddress = address;
	}
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCurrentAddress() {
		return currentAddress;
	}
	public void setCurrentAddress(String currentAddress) {
		this.currentAddress = currentAddress;
	}
}
