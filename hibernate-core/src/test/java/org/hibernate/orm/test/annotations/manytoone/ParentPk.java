/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class ParentPk implements Serializable {
	@Column(length = 50)
	String firstName;
	String lastName;

	/**
	 * is a male or a female
	 */
	//show hetereogenous PK types
	boolean isMale;

	public int hashCode() {
		//this implem sucks
		return firstName.hashCode() + lastName.hashCode() + ( isMale ? 0 : 1 );
	}

	public boolean equals(Object obj) {
		//firstName and lastName are expected to be set in this implem
		if ( obj != null && obj instanceof ParentPk ) {
			ParentPk other = (ParentPk) obj;
			return firstName.equals( other.firstName )
					&& lastName.equals( other.lastName )
					&& isMale == other.isMale;
		}
		else {
			return false;
		}
	}
}
