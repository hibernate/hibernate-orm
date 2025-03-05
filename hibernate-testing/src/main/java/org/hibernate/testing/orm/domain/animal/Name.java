/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.animal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Name {
	private String first;
	private Character initial;
	private String last;

	public Name() {}

	public Name(String first, Character initial, String last) {
		this.first = first;
		this.initial = initial;
		this.last = last;
	}

	public Name(String first, char initial, String last) {
		this( first, Character.valueOf( initial ), last );
	}

	@Column( name = "name_first" )
	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	@Column( name = "name_initial" )
	public Character getInitial() {
		return initial;
	}

	public void setInitial(Character initial) {
		this.initial = initial;
	}

	@Column( name = "name_last" )
	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}
}
