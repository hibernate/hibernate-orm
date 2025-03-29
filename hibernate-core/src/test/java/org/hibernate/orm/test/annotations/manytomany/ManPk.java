/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class ManPk implements Serializable {
	private String firstName;
	private String lastName;
	private boolean isElder;

	public boolean isElder() {
		return isElder;
	}

	public void setElder(boolean elder) {
		isElder = elder;
	}

	public int hashCode() {
		//this implem sucks
		return getFirstName().hashCode() + getLastName().hashCode() + ( isElder() ? 0 : 1 );
	}

	public boolean equals(Object obj) {
		//firstName and lastName are expected to be set in this implem
		if ( obj != null && obj instanceof ManPk ) {
			ManPk other = (ManPk) obj;
			return getFirstName().equals( other.getFirstName() )
					&& getLastName().equals( other.getLastName() )
					&& isElder() == other.isElder();
		}
		else {
			return false;
		}
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column(length=128)
	public String getFirstName() {
		return firstName;
	}

	@Column(length=128)
	public String getLastName() {
		return lastName;
	}
}
