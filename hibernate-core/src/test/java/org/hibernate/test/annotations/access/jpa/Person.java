/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.test.annotations.access.jpa;
import javax.persistence.Access;
import javax.persistence.Entity;


/**
 * @author Hardy Ferentschik
 */
@Entity
// explicitly override the access type to be property (default is field, see Being)
@Access(javax.persistence.AccessType.PROPERTY)
public class Person extends Being {

	String firstname;

	private String lastname;

	public String getFirstname() {
		return null;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
}


