/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Footballer.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(FootballerPk.class)
@DiscriminatorColumn(name = "bibi")
public class Footballer {
	private String firstname;
	private String lastname;
	private String club;

	public Footballer() {
	}

	public Footballer(String firstname, String lastname, String club) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.club = club;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Footballer ) ) return false;

		final Footballer footballer = (Footballer) o;

		if ( !firstname.equals( footballer.firstname ) ) return false;
		if ( !lastname.equals( footballer.lastname ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = firstname.hashCode();
		result = 29 * result + lastname.hashCode();
		return result;
	}

	@Id
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@Id
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}
}
