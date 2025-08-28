/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.embeddables;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class NameInfo {
	private String firstName;
	private String lastName;

	NameInfo() {

	}

	public NameInfo(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( firstName != null ? firstName.hashCode() : 0 );
		result = result * 31 + ( lastName != null ? lastName.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof NameInfo ) ) {
			return false;
		}
		NameInfo that = (NameInfo) obj;
		if ( firstName != null ? !firstName.equals( that.firstName) : that.firstName != null ) {
			return false;
		}
		if ( lastName != null ? !lastName.equals( that.lastName) : that.lastName != null ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "NameInfo{" +
				"firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				'}';
	}
}
