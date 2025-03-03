/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.animal;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@PrimaryKeyJoinColumn( name = "mammal_id_fk" )
public class Mammal extends Animal {
	private boolean pregnant;
	private Date birthdate;

	public boolean isPregnant() {
		return pregnant;
	}

	public void setPregnant(boolean pregnant) {
		this.pregnant = pregnant;
	}

	@Temporal( TemporalType.DATE )
	public Date getBirthdate() {
		return birthdate;
	}


	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Mammal ) ) {
			return false;
		}

		Mammal mammal = ( Mammal ) o;

		if ( pregnant != mammal.pregnant ) {
			return false;
		}
		if ( birthdate != null ? !birthdate.equals( mammal.birthdate ) : mammal.birthdate != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = ( pregnant ? 1 : 0 );
		result = 31 * result + ( birthdate != null ? birthdate.hashCode() : 0 );
		return result;
	}
}
