/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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
