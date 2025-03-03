/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;
import java.util.Date;

/**
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.testing.orm.domain.animal.Mammal} instead
 */
@Deprecated
public class Mammal extends Animal {
	private boolean pregnant;
	private Date birthdate;

	public boolean isPregnant() {
		return pregnant;
	}

	public void setPregnant(boolean pregnant) {
		this.pregnant = pregnant;
	}

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
