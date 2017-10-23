/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.joined.notownedrelation;

import javax.persistence.Entity;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class PersonalContact extends Contact {
	private String firstname;

	public PersonalContact() {
	}

	public PersonalContact(Long id, String email, String firstname) {
		super( id, email );
		this.firstname = firstname;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
}