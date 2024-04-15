/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.animal;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
