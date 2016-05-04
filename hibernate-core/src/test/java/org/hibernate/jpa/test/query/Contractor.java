/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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
