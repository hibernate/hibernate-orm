/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class ParentPk implements Serializable {
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
