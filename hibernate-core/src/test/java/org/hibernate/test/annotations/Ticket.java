/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Flight ticket
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Ticket implements Serializable {
	Long id;
	String number;

	public Ticket() {
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	@Column(name = "ticket_number")
	public String getNumber() {
		return number;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public void setNumber(String string) {
		number = string;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Ticket ) ) return false;

		final Ticket ticket = (Ticket) o;

		if ( !number.equals( ticket.number ) ) return false;

		return true;
	}

	public int hashCode() {
		return number.hashCode();
	}

}

