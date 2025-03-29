/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Lukasz Antoniak
 */
@Entity
@DiscriminatorValue("Contractor")
public class Contractor extends Employee {
	private String company;

	public Contractor() {
	}

	public Contractor(String name, Double salary, String company) {
		super( name, salary );
		this.company = company;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Contractor ) ) return false;
		if ( !super.equals( o ) ) return false;

		Contractor that = (Contractor) o;

		if ( company != null ? !company.equals( that.company ) : that.company != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (company != null ? company.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Contractor(" + super.toString() + ", company = " + company + ")";
	}
}
